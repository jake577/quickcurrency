package com.jesttek.quickcurrencylibrary.ExchangeControllers;

public interface DataLoadedListener {
    public void onDataLoaded(String nodeId, int exchangeId, String last, String high, String low);
    public void onLoadFailed(String nodeId, int exchangeId, String message);
}
