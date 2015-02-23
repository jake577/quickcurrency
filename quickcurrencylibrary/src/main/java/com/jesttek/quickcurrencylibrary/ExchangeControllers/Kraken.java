package com.jesttek.quickcurrencylibrary.ExchangeControllers;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Kraken extends AbstractExchange {
    //api documentation - https://www.kraken.com/help/api
    private static String APIURL = "https://api.kraken.com/0/public/Ticker?pair=";

    public Kraken(DataLoadedListener listener, String nodeId, int exchangeId) {
        super(listener, nodeId, exchangeId);
    }

    @Override
    public void getData(RequestQueue requestQueue, String UrlID) {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, APIURL.concat(UrlID), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        parseResponse(response);
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

    private void parseResponse(JSONObject response){
        try {

            JSONObject result = response.getJSONObject("result");

            Iterator<String> itr = result.keys();
            if(itr.hasNext()) {
                String pairName = itr.next();
                JSONObject currencyPairData = result.getJSONObject(pairName);
                //JSONArray ask = currencyPairData.getJSONArray("a");
                //JSONArray bid = currencyPairData.getJSONArray("b");
                JSONArray lastTrade = currencyPairData.getJSONArray("c");
                //JSONArray volume = currencyPairData.getJSONArray("v");
                //JSONArray volumeWeightedAverage = currencyPairData.getJSONArray("p");
                //JSONArray numberOfTrades = currencyPairData.getJSONArray("t");
                JSONArray lowPrice = currencyPairData.getJSONArray("l");
                JSONArray highPrice = currencyPairData.getJSONArray("h");
                //JSONArray open = currencyPairData.getJSONArray("o");

                String low = lowPrice.getString(1);
                String high = highPrice.getString(1);
                String last = lastTrade.getString(0);
                mLoadedListener.onDataLoaded(mNodeId, mExchangeId, last, low, high);
            }
            else {
                mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed parsing exchange data. No pair returned");
            }
        } catch (JSONException ex) {
            mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed parsing exchange data: " + ex.getMessage());
        }
    }
}
