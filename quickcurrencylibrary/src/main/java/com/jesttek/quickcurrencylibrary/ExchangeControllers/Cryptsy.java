package com.jesttek.quickcurrencylibrary.ExchangeControllers;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class Cryptsy extends AbstractExchange {
    //api documentation - https://www.cryptsy.com/pages/api
    private static String APIURL = "https://api.cryptsy.com/api/v2/markets/";

    public Cryptsy(DataLoadedListener listener, String nodeId, int exchangeId) {
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
            JSONObject data = response.getJSONObject("data");
            JSONObject stats = data.getJSONObject("24hr");
            JSONObject lastPrice = data.getJSONObject("last_trade");
            String low = stats.getString("price_low");
            String high = stats.getString("price_high");
            String last = lastPrice.getString("price");
            mLoadedListener.onDataLoaded(mNodeId, mExchangeId, last, low, high);
        } catch (JSONException ex) {
            mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed parsing exchange data: " + ex.getMessage());
        }
    }
}
