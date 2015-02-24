package com.jesttek.quickcurrencylibrary.ExchangeControllers;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;


public class Poloniex extends AbstractExchange {
    //api documentation - https://poloniex.com/api
    private static String APIURL = "https://poloniex.com/public?command=returnTicker";

    private String mCoinId;

    public Poloniex(DataLoadedListener listener, String nodeId, int exchangeId) {
        super(listener, nodeId, exchangeId);
    }

    @Override
    public void getData(RequestQueue requestQueue, String UrlID) {
        mCoinId = UrlID;

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, APIURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        parseData(response);
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

    private void parseData(JSONObject response){
        try {
            JSONObject coinData = response.getJSONObject(mCoinId);
            String last = coinData.getString("last");
            String high = coinData.getString("high24hr");
            String low = coinData.getString("low24hr");
            mLoadedListener.onDataLoaded(mNodeId, mExchangeId, last, low, high);
        } catch (JSONException ex) {
            mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed parsing exchange data: " + ex.getMessage());
        }
    }
}