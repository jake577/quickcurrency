package com.jesttek.quickcurrencylibrary.ExchangeControllers;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class Bitfinex extends AbstractExchange{
    //api documentation - https://www.bitfinex.com/pages/api
    private static String APIURL = "https://api.bitfinex.com/v1/pubticker/";

    public Bitfinex(DataLoadedListener listener, String nodeId, int exchangeId) {
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
            String low = response.getString("low");
            String high = response.getString("high");
            String last = response.getString("last_price");
            mLoadedListener.onDataLoaded(mNodeId, mExchangeId, last, low, high);
        } catch (JSONException ex) {
            mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed parsing exchange data: " + ex.getMessage());
        }
    }
}
