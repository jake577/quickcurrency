package com.jesttek.quickcurrency;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.jesttek.quickcurrencylibrary.MessageConstants;
import com.jesttek.quickcurrencylibrary.database.CoinExchangeProvider;
import com.jesttek.quickcurrencylibrary.database.CurrencyPair;
import com.jesttek.quickcurrencylibrary.database.Exchange;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class WearableDataLoaderService extends WearableListenerService {

    private static final String TAG = "QuickCurrencyPoller";
    private GoogleApiClient mGoogleApiClient;
    private RequestQueue mQueue;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.d(TAG,"Message received: " + messageEvent.getPath());
        if(messageEvent.getPath().equals(MessageConstants.MESSAGE_POLL_PATH)) {
            int exchangeId = -1;
            try {
                JSONObject jsonObj = new JSONObject(new String(messageEvent.getData(), "UTF-8"));
                exchangeId = jsonObj.getInt("exchangeId");
                Log.d(TAG, "received request from " + exchangeId);
            }
            catch (Exception ex) {
                Log.w(TAG, "message received in invalid format");
                Log.w(TAG, ex);
                super.onMessageReceived(messageEvent);
                return;
            }
            getCoinData(exchangeId, messageEvent.getSourceNodeId());
        }
        else if(messageEvent.getPath().equals(MessageConstants.MESSAGE_PAGES_PATH)) {

            String[] columns = new String[] { BaseColumns._ID, CurrencyPair.KEY_CURRENCY1, CurrencyPair.KEY_CURRENCY2, CurrencyPair.KEY_GRIDROW, Exchange.KEY_NAME, Exchange.FOREIGN_KEY_CURRENCYPAIR};
            Cursor c = getContentResolver().query(CoinExchangeProvider.EXCHANGE_CONTENT_URI, columns, Exchange.KEY_ACTIVE + " = ? AND " + CurrencyPair.KEY_DISPLAYED + " = ?", new String[] {"1", "1"}, CurrencyPair.KEY_GRIDROW);

            byte[] replyData = null;

            try {
                if (c.moveToFirst()) {
                    JSONArray coinPairs = new JSONArray();
                    boolean finished = false;
                    while (!finished) {
                        int pairId = c.getInt(5);
                        JSONObject pair = new JSONObject();
                        pair.put("currency1", c.getString(1));
                        pair.put("currency2", c.getString(2));

                        JSONArray exchanges = new JSONArray();
                        do {
                            JSONObject exchange = new JSONObject();
                            exchange.put("id", c.getInt(0));
                            exchange.put("name", c.getString(4));
                            exchanges.put(exchange);
                            finished = !c.moveToNext();
                        } while(!finished && c.getInt(5) == pairId);

                        pair.put("exchanges", exchanges);
                        coinPairs.put(pair);
                    }
                    replyData = coinPairs.toString().getBytes("utf-8");
                }
            }
            catch (JSONException ex) {
                Log.e(TAG, "Error writing json", ex);
            }
            catch (UnsupportedEncodingException ex) {
                Log.e(TAG, "Error writing json", ex);
            }
            catch (Exception ex) {
                Log.e(TAG, "Exception", ex);
            }
            Log.d(TAG,"Sending reply. Using path " + com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_PAGES_REPLY_PATH);
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    messageEvent.getSourceNodeId(),
                    com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_PAGES_REPLY_PATH,
                    replyData
            );
            c.close();
        } else {
            super.onMessageReceived(messageEvent);
        }
    }

    private void getCoinData(final int exchangeId, final String nodeId) {
        if(mQueue == null) {
            mQueue = Volley.newRequestQueue(getApplicationContext());
        }

        String[] columns = new String[] {Exchange.KEY_NAME, Exchange.KEY_URL};
        String[] args = {Integer.toString(exchangeId)};
        Cursor c = getContentResolver().query(CoinExchangeProvider.EXCHANGE_CONTENT_URI, columns, BaseColumns._ID + " = ?", args, null);
        if(c.moveToFirst()) {
            String url = c.getString(1);
            final String exchangeName = c.getString(0);

            if (url != null) {
                JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                byte[] replyData = null;
                                if (exchangeName.equals("bitstamp") || exchangeName.equals("bter")) {
                                    replyData = parseResponse(response);
                                } else if (exchangeName.equals("btce")) {
                                    replyData = parseBtceResponse(response);
                                } else if (exchangeName.equals("bitfinex")) {
                                    replyData = parseBitfinexResponse(response);
                                }

                                Log.d(TAG, "Sending reply to pageId " + exchangeId + " for exchange " + exchangeName);
                                PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                                        mGoogleApiClient,
                                        nodeId,
                                        replyData == null ? com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_REPLY_ERROR_PATH + "/" + exchangeId : com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_POLL_REPLY_PATH + "/" + exchangeId,
                                        replyData
                                );
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.w(TAG, "Failed connecting to bitstamp server: " + error.getMessage());
                                PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                                        mGoogleApiClient,
                                        nodeId,
                                        com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_REPLY_ERROR_PATH,
                                        null
                                );
                            }
                        });
                mQueue.add(jsonRequest);
            } else {
                //message was received for a website app doesn't support. Should never occur
                Log.w(TAG, "invalid exchange request: " + exchangeName);
                PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient,
                        nodeId,
                        com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_REPLY_ERROR_PATH,
                        null
                );
            }
        }
        else {
            //Requested exchange id didn't exist
            Log.w(TAG, "invalid exchange id request: " + exchangeId);
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    nodeId,
                    com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_REPLY_ERROR_PATH,
                    null
            );
        }
        c.close();
    }

    /**
     * Gets relevant data from json object and packs it into a new message to be sent to wearable
     * @param response the data from the web server to be parsed
     * @return the message to be sent back to the wearable. Null if there was an error parsing data
     */
    public byte[] parseBitfinexResponse(JSONObject response) {
        try {
            //get just the data we want and put it in a new JSONObject
            JSONObject message = new JSONObject();
            String low = response.getString("low");
            String high = response.getString("high");
            String last = response.getString("last_price");
            message.put("last", last);
            message.put("high", high);
            message.put("low", low);
            return message.toString().getBytes("utf-8");
        }
        catch(JSONException ex) {
            //Couldn't put the strings in new jsonObject.
            //This should never happen.
            Log.w(TAG, ex);
        }
        catch(UnsupportedEncodingException ex) {
            //UTF-8 isn't supported. This should never happen
            Log.w(TAG, ex);
        }
        return null;
    }

    /**
     * Gets relevant data from json object and packs it into a new message to be sent to wearable
     * @param response the data from the web server to be parsed
     * @return the message to be sent back to the wearable. Null if there was an error parsing data
     */
    public byte[] parseBtceResponse(JSONObject response) {
        try {
            //get just the data we want and put it in a new JSONObject
            JSONObject message = new JSONObject();
            JSONObject ticker = response.getJSONObject("ticker");
            String low = ticker.getString("low");
            String high = ticker.getString("high");
            String last = ticker.getString("last");
            message.put("last", last);
            message.put("high", high);
            message.put("low", low);
            return message.toString().getBytes("utf-8");
        }
        catch(JSONException ex) {
            //Couldn't put the strings in new jsonObject.
            //This should never happen.
            Log.w(TAG, ex);
        }
        catch(UnsupportedEncodingException ex) {
            //UTF-8 isn't supported. This should never happen
            Log.w(TAG, ex);
        }
        return null;
    }

    /**
     * Gets relevant data from json object and packs it into a new message to be sent to wearable.
     * Most exchanges return similar jsonObject that contains with "low", "high" and "last" fields
     * that can be parsed by this function.
     * @param response the data from the web server to be parsed
     * @return the message to be sent back to the wearable. Null if there was an error parsing data
     */
    public byte[] parseResponse(JSONObject response) {
        try {
            //get just the data we want and put it in a new JSONObject
            JSONObject message = new JSONObject();
            String low = response.getString("low");
            String high = response.getString("high");
            String last = response.getString("last");
            message.put("last", last);
            message.put("high", high);
            message.put("low", low);
            return message.toString().getBytes("utf-8");
        }
        catch(JSONException ex) {
            //Couldn't put the strings in new jsonObject.
            //This should never happen.
            Log.w(TAG, ex);
        }
        catch(UnsupportedEncodingException ex) {
            //UTF-8 isn't supported. This should never happen
            Log.w(TAG, ex);
        }
        return null;
    }
}