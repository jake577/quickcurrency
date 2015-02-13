package com.jesttek.quickcurrencylibrary.ExchangeControllers;

import com.android.volley.RequestQueue;

public abstract class AbstractExchange {

    protected DataLoadedListener mLoadedListener;
    protected String mNodeId;
    protected int mExchangeId;
    /**
     * @param listener the listener to call when data load has completed
     * @param nodeId The device the data is being loaded for
     * @param exchangeId The exchange page the data is being loaded for
     */
    public AbstractExchange (DataLoadedListener listener, String nodeId, int exchangeId) {
        mLoadedListener = listener;
        mNodeId = nodeId;
        mExchangeId = exchangeId;
    }

    /**
     * Load data from the exchange
     * @param requestQueue Used to make the request to the exchange
     * @param UrlID The API id used for the currency pair
     */
    public abstract void getData(RequestQueue requestQueue, String UrlID);
}
