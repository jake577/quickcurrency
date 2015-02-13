package com.jesttek.quickcurrencylibrary.ExchangeControllers;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class Coinbase extends AbstractExchange{
    //api documentation - https://docs.exchange.coinbase.com
    private static String APIURL_STATS = "https://api.exchange.coinbase.com/products/<URLID>/stats";
    private static String APIURL_LAST = "https://api.exchange.coinbase.com/products/<URLID>/ticker";

    private String mHigh;
    private String mLow;

    public Coinbase(DataLoadedListener listener, String nodeId, int exchangeId) {
        super(listener, nodeId, exchangeId);
    }

    @Override
    public void getData(final RequestQueue requestQueue, final String UrlID) {
        //Coinbase uses two separate api calls for 24 hour high/low and last price.
        //Load the 24 hour high/low first, then load the last price
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, APIURL_STATS.replace("<URLID>", UrlID), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        parseStatsResponse(response);
                        getLastPrice(requestQueue, UrlID);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed connecting to exchange server: " + error.getMessage());
                    }
                });
        requestQueue.add(jsonRequest);
    }

    private void getLastPrice(RequestQueue requestQueue, String UrlID) {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, APIURL_LAST.replace("<URLID>", UrlID), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        parseLastPriceResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed connecting to exchange server: " + error.getMessage());
                    }
                });
        requestQueue.add(jsonRequest);
    }

    private void parseStatsResponse(JSONObject response){
        try {
            mLow = response.getString("low");
            mHigh = response.getString("high");
        } catch (JSONException ex) {
            mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed parsing exchange data: " + ex.getMessage());
        }
    }

    private void parseLastPriceResponse(JSONObject response){
        try {
            String last = response.getString("price");
            mLoadedListener.onDataLoaded(mNodeId, mExchangeId, last, mLow, mHigh);
        } catch (JSONException ex) {
            mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed parsing exchange data: " + ex.getMessage());
        }
    }
}
