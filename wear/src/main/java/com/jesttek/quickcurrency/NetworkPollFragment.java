package com.jesttek.quickcurrency;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.jesttek.quickcurrencylibrary.CoinConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class NetworkPollFragment extends android.app.Fragment implements MessageApi.MessageListener {

    private static final String MESSAGE_REPLY_ERROR_PATH = "/polling_service/refresh_error";
    private static final String MESSAGE_REPLY_PATH = "/polling_service/fresh_data";
    private static final String MESSAGE_POLL_PATH = "/poll_coin";
    private static final String TAG = "QuickCurrencyDataViewer";

    private static final int REFRESH_TIME = 30000;

    //A short delay is used from when fragment is displayed and when first data request is made.
    //This is to stop a lot of requests being made at the same time as user flicks through pages.
    private static final int LOAD_DELAY = 200;

    private TextView mContentText;
    private TextView mTitleText;
    private Node mPeerNode; // The host node
    private Handler mHandler = new Handler();
    private CoinConstants mCurrencyFrom;
    private CoinConstants mCurrencyTo;
    private String mExchange;
    private boolean mPollingActive = false;
    private int mId = -1;
    private GoogleApiClient mGoogleApiClient;

    private Runnable mRequest = new Runnable() {
        @Override
        public void run() {
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    sendMessage();
                    return null;
                }
            }.execute();
        }
    };

    /**
     * Used to activate page after short delay
     */
    private class ActivateFragTask implements Runnable {
        private MessageApi.MessageListener mListener;
        public ActivateFragTask(MessageApi.MessageListener listener) {
            mListener = listener;
        }

        @Override
        public void run() {
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    Wearable.MessageApi.addListener(mGoogleApiClient, mListener).setResultCallback(resultCallback);
                    return null;
                }
            }.execute();
        }
    };

    /**
     * Prepares a new fragment
     * @param id Used to identify messages intended for this fragment
     * @param currencyFrom The currency being converted from
     * @param currencyFrom The currency being converted to
     * @param exchange The exchange that will be polled
     * @param active True if the fragment should start polling as soon as it's created
     * @return The new fragment
     */
    public static NetworkPollFragment create(int id, CoinConstants currencyFrom, CoinConstants currencyTo, String exchange, boolean active) {
        NetworkPollFragment fragment = new NetworkPollFragment();
        Bundle args = new Bundle();
        args.putSerializable("currencyFrom", currencyFrom);
        args.putSerializable("currencyTo", currencyTo);
        args.putString("exchange", exchange);
        args.putInt("id", id);
        args.putBoolean("active", active);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setRetainInstance(true);
        mGoogleApiClient = ((MainWearActivity)this.getActivity()).getGoogleApiClient();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCurrencyFrom = (CoinConstants)getArguments().getSerializable("currencyFrom");
        mCurrencyTo = (CoinConstants)getArguments().getSerializable("currencyTo");
        mExchange = getArguments().getString("exchange");
        mId = getArguments().getInt("id");
        mPollingActive = getArguments().getBoolean("active");

        if(mPollingActive) {
            Wearable.MessageApi.addListener(mGoogleApiClient, this).setResultCallback(resultCallback);
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.grid_view_pager_item, container, false);
        mContentText = (TextView) view.findViewById(R.id.content_text_view);
        mContentText.setText("loading...");
        mTitleText = (TextView) view.findViewById(R.id.grid_page_title);
        mTitleText.setText(mExchange.toUpperCase() + "\n" + mCurrencyFrom.getShortName() + "/" + mCurrencyTo.getShortName());
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onStop(){
        mPollingActive = false;
        super.onStop();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "node " + mId + ": onDetach()");
        setActive(false);
        super.onDetach();
    }

    /**
     * This callback occurs after the MessageApi listener is added to the Google API Client.
     */
    private ResultCallback<Status> resultCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            if(mPollingActive) {
                Log.d(TAG, "node " + mId + " :Listener connected: posting sendMessage");
                mHandler.post(mRequest);
            }
        }
    };

    /**
     * This method will generate all the nodes that are attached to a Google Api Client.
     * Theoretically, only one should be: the phone. I'm assuming that the first node is
     * the host phone, and saving that node so the node list doesn't need to be checked again.
     */
    private void sendMessage(){
        if(mPollingActive) {
            mHandler.postDelayed(mRequest, REFRESH_TIME);
        }

        if(mPollingActive) {
            Log.d(TAG, "node " + mId + " sending request for " + mExchange + " : " + mCurrencyFrom.getShortName() + "-" + mCurrencyTo.getShortName());

            //TODO: redo this if we haven't received a reply from the host in a while
            if(mPeerNode == null) {
                NodeApi.GetConnectedNodesResult rawNodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = rawNodes.getNodes();
                if(nodes.size() > 0) {
                    mPeerNode = nodes.get(0);
                }
            }

            if(mPeerNode != null) {
                try {
                    JSONObject message = new JSONObject();
                    message.put("exchangeId", mId);
                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient,
                            mPeerNode.getId(),
                            MESSAGE_POLL_PATH,
                            message.toString().getBytes("utf-8")
                    );

                } catch (JSONException ex) {
                    //Couldn't put the strings in new jsonObject.
                    //This should never happen.
                    Log.w(TAG, ex);
                } catch (UnsupportedEncodingException ex) {
                    //UTF-8 isn't supported.
                    //This should never happen
                    Log.w(TAG, ex);
                }
            }
            else {
                Log.d(TAG, "no host connected, no message sent");
            }
        }
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        /*
        This method runs in a background thread.
        */
        if(mPollingActive) {
            Log.v(TAG, "Node "+mId+": Message received on wear: " + messageEvent.getPath());
            if(messageEvent.getPath().endsWith(Integer.toString(mId)))
            {
                try {
                    JSONObject jsonObj = new JSONObject(new String(messageEvent.getData(), "UTF-8"));
                    final double last = jsonObj.getDouble("last");
                    final double high = jsonObj.getDouble("high");
                    final double low = jsonObj.getDouble("low");
                    Log.d(TAG, "node " + mId + " receiving results for " + mExchange);
                    if(messageEvent.getPath().startsWith(MESSAGE_REPLY_PATH)) {
                        this.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(high < 1 || last < 1 || low < 1) {
                                    mContentText.setText("Last: " + String.format("%.8f", last) + "\nHigh: " + String.format("%.8f", high) + "\nLow: " + String.format("%.8f", low));
                                }
                                else {
                                    mContentText.setText("Last: " + last + "\nHigh: " + high + "\nLow: " + low);
                                }
                            }
                        });
                    }
                    else {
                        Log.w(TAG, "error on host device when refreshing data");
                    }
                }
                catch(UnsupportedEncodingException ex) {
                    Log.d(TAG, "invalid json in message received");
                }
                catch(JSONException ex) {
                    Log.d(TAG, "invalid json in message received");
                }
            }
        }
    }

    /**
     * When actived polling is started after a short delay (to stop large amounts of network requests
     * starting from user flicking through pages).
     * @param active true if this is the current fragment being displayed
     */
    public void setActive(boolean active) {
        if(active) {
            if (mContentText.length() == 0) {
                mContentText.setText("loading...");
            }
            mPollingActive = true;

            //reset any pending messages
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new ActivateFragTask(this), LOAD_DELAY);
        }
        else {
            Log.d(TAG, "node " + mId + ": stopping polling");
            mPollingActive = false;
            mHandler.removeCallbacksAndMessages(null);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        }
    }
}