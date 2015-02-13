package com.jesttek.quickcurrency;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.jesttek.quickcurrencylibrary.ExchangeConstants;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.AbstractExchange;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Bitfinex;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Bitstamp;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Btce;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Bter;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Coinbase;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Cryptsy;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.DataLoadedListener;
import com.jesttek.quickcurrencylibrary.MessageConstants;
import com.jesttek.quickcurrencylibrary.database.CoinExchangeProvider;
import com.jesttek.quickcurrencylibrary.database.CurrencyPair;
import com.jesttek.quickcurrencylibrary.database.Exchange;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class WearableDataLoaderService extends WearableListenerService implements DataLoadedListener {

    private static final String TAG = "QuickCurrencyPoller";
    private GoogleApiClient mGoogleApiClient;
    private RequestQueue mQueue;
    @Override
    public void onCreate() {
        super.onCreate();
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

        String[] columns = new String[] {Exchange.KEY_NAME, Exchange.KEY_URL_ID};
        String[] args = {Integer.toString(exchangeId)};
        Cursor c = getContentResolver().query(CoinExchangeProvider.EXCHANGE_CONTENT_URI, columns, BaseColumns._ID + " = ?", args, null);
        if(c.moveToFirst()) {
            String urlId = c.getString(1);
            final ExchangeConstants exchangeName = ExchangeConstants.valueOf(c.getString(0));

            if (urlId != null) {

                AbstractExchange exchange = null;
                switch (exchangeName) {
                    case Bitfinex:
                        exchange = new Bitfinex(this, nodeId, exchangeId);
                        break;
                    case Bitstamp:
                        exchange = new Bitstamp(this, nodeId, exchangeId);
                        break;
                    case Btce:
                        exchange = new Btce(this, nodeId, exchangeId);
                        break;
                    case Bter:
                        exchange = new Bter(this, nodeId, exchangeId);
                        break;
                    case Coinbase:
                        exchange = new Coinbase(this, nodeId, exchangeId);
                        break;
                    case Cryptsy:
                        exchange = new Cryptsy(this, nodeId, exchangeId);
                        break;
                }
                exchange.getData(mQueue, urlId);
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

    @Override
    public void onDataLoaded(String nodeId, int exchangeId, String last, String high, String low) {
        try {
            JSONObject message = new JSONObject();
            message.put("last", last);
            message.put("high", high);
            message.put("low", low);
            byte[] replyData = message.toString().getBytes("utf-8");
            Log.d(TAG, "Sending reply to pageId " + exchangeId);
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    nodeId,
                    replyData == null ? com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_REPLY_ERROR_PATH + "/" + exchangeId : com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_POLL_REPLY_PATH + "/" + exchangeId,
                    replyData
            );
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
    }

    @Override
    public void onLoadFailed(String nodeId, int exchangeId, String message) {
        Log.w(TAG, message);
        PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                mGoogleApiClient,
                nodeId,
                com.jesttek.quickcurrencylibrary.MessageConstants.MESSAGE_REPLY_ERROR_PATH + "/" + exchangeId,
                null
        );
    }
}