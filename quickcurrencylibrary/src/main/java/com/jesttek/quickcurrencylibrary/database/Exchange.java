package com.jesttek.quickcurrencylibrary.database;

import android.provider.BaseColumns;

public class Exchange {
    public static final String TABLE_NAME = "exchange";
    public static final String KEY_NAME = "name";
    public static final String KEY_URL = "url";
    public static final String KEY_ACTIVE = "active";
    public static final String FOREIGN_KEY_CURRENCYPAIR = "CurrencyPair_id";

    public static final String[] TABLE_COLUMNS = {BaseColumns._ID, KEY_NAME, KEY_URL, KEY_ACTIVE, FOREIGN_KEY_CURRENCYPAIR};

    private String mName;
    private String mUrl;
    private boolean mActive;
    private int mCurrencyPairId;

    /**
     *
     * @param name Name of the website that the currency pair is being exchanged on
     * @param url The url used to fetch data for the currency pair exchange
     * @param active If this exchange is displayed on the watch grid
     * @param currencyPairId Id of the currency pair being exchanged
     */
    public Exchange(String name, String url, boolean active, int currencyPairId) {
        mName = name;
        mUrl = url;
        mActive = active;
        mCurrencyPairId = currencyPairId;
    }

    /**
     *
     * @return The name of the website that the currency pair is being exchanged on
     */
    public String getName() {
        return mName;
    }

    /**
     *
     * @return The url used to fetch data for the currency pair exchange
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     *
     * @return True if this exchange is displayed on the watch grid
     */
    public boolean isActive() {
        return mActive;
    }

    /**
     *
     * @return the Id of the currency pair being exchanged
     * @see com.jesttek.quickcurrencylibrary.database.CurrencyPair
     */
    public int getCurrencyPairId() {
        return mCurrencyPairId;
    }
}
