package com.jesttek.quickcurrency;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.WindowInsets;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
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
import com.jesttek.quickcurrencylibrary.MessageConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MainWearActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks{

    private static final String TAG = MainWearActivity.class.getName();
    private Point mLastPage = new Point(0,0);
    private MyGridViewPagerAdapter mAdapter;
    private GoogleApiClient mGoogleApiClient;
    private GridViewPager mPager;
    private TextView mMessageText;
    private Handler mHandler = new Handler();

    private Runnable mRequestPages = new Runnable() {
        @Override
        public void run() {
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    requestPages();
                    return null;
                }
            }.execute();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);
        final Resources res = getResources();
        mPager = (GridViewPager) findViewById(R.id.pager);
        mPager.setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Adjust page margins:
                //   A little extra horizontal spacing between pages looks a bit
                //   less crowded on a round display.
                final boolean round = insets.isRound();
                int rowMargin = res.getDimensionPixelOffset(R.dimen.page_row_margin);
                int colMargin = res.getDimensionPixelOffset(round ?
                        R.dimen.page_column_margin_round : R.dimen.page_column_margin);
                mPager.setPageMargins(rowMargin, colMargin);
                return insets;
            }
        });

        mMessageText = (TextView) findViewById(R.id.message_text);
        mMessageText.setText("Loading\nWaiting for host device");
        mMessageText.setVisibility(View.VISIBLE);

        mAdapter = new MyGridViewPagerAdapter(this, getFragmentManager(), null);
        mAdapter.addPage(new GridPage("Loading","Waiting for host device"));
        mAdapter.notifyDataSetChanged();

        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageCount(1);
        mPager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, int i2, float v, float v2, int i3, int i4) {

            }

            @Override
            public void onPageSelected(int row, int col) {
                NetworkPollFragment lastFragment = (NetworkPollFragment)mAdapter.getLoadedFragment(mLastPage.y, mLastPage.x);
                if(lastFragment != null) {
                    lastFragment.setActive(false);
                }

                Point current = mPager.getCurrentItem();
                NetworkPollFragment currentFragment = (NetworkPollFragment)mAdapter.getLoadedFragment(current.y, current.x);
                if(currentFragment != null) {
                    currentFragment.setActive(true);
                }
                mLastPage.x = current.x;
                mLastPage.y = current.y;
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        connectGoogleApi();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this).setResultCallback(resultCallback);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    /**
     * This callback occurs after the MessageApi listener is added to the Google API Client.
     */
    private ResultCallback<Status> resultCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            mHandler.post(mRequestPages);
        }
    };

    /**
     * Sends message to host device requesting pages to display
     */
    public void requestPages() {
        Node peerNode = null;
        NodeApi.GetConnectedNodesResult rawNodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        List<Node> nodes = rawNodes.getNodes();
        if(nodes.size() > 0) {
            peerNode = nodes.get(0);
        }

        if(peerNode != null) {
            PendingResult r = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    peerNode.getId(),
                    MessageConstants.MESSAGE_PAGES_PATH,
                    null
            );
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPager.setVisibility(View.GONE);
                    mMessageText.setText("Cannot connect\nCheck that your host device is connected");
                    mMessageText.setVisibility(View.VISIBLE);
                }
            });

            //retry connecting to host
            mHandler.postDelayed(mRequestPages, 2500);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().equals(MessageConstants.MESSAGE_PAGES_REPLY_PATH))
        {
            try {
                ArrayList<ArrayList<GridPage>> pages = new ArrayList<ArrayList<GridPage>>();
                JSONArray data = new JSONArray(new String(messageEvent.getData(), "UTF-8"));
                int length = data.length();
                for (int i = 0; i < length; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    String currency1 = jsonObject.getString("currency1");
                    String currency2 = jsonObject.getString("currency2");

                    CoinConstants c1 = CoinConstants.valueOf(currency1);
                    CoinConstants c2 = CoinConstants.valueOf(currency2);

                    ArrayList<GridPage> ExchangePages = new ArrayList<GridPage>();
                    JSONArray exchanges = jsonObject.getJSONArray("exchanges");
                    int count = exchanges.length();
                    for(int n = 0; n < count; n++) {
                        JSONObject exchange = exchanges.getJSONObject(n);
                        ExchangePages.add(new GridPage(exchange.getInt("id"), exchange.getString("name"), c1, c2));
                    }
                    pages.add(ExchangePages);
                }

                mAdapter.setPages(pages);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                        mPager.setVisibility(View.VISIBLE);
                        mMessageText.setVisibility(View.GONE);
                    }
                });
            }
            catch (JSONException ex) {
                Log.e(TAG, "invalid json", ex);
            }
            catch (UnsupportedEncodingException ex) {
                Log.e(TAG, "invalid string encoding", ex);
            }
        }
    }

    public void connectGoogleApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.w(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
}