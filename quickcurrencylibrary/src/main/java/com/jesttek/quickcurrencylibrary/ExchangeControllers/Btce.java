package com.jesttek.quickcurrencylibrary.ExchangeControllers;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;


public class Btce extends AbstractExchange{
//api documentation - https://btc-e.com/api/3/docs
    private static String APIURL = "https://btc-e.com/api/3/ticker/";

    public Btce(DataLoadedListener listener, String nodeId, int exchangeId) {
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
            //name of the first (and only) element depends on keypair. Just use the key iterator to
            //get the element .
            Iterator keys = response.keys();
            JSONObject ticker = response.getJSONObject(keys.next().toString());
            String low = ticker.getString("low");
            String high = ticker.getString("high");
            String last = ticker.getString("last");
            mLoadedListener.onDataLoaded(mNodeId, mExchangeId, last, low, high);
        } catch (JSONException ex) {
            mLoadedListener.onLoadFailed(mNodeId, mExchangeId, "Failed parsing exchange data: " + ex.getMessage());
        }
    }
}
