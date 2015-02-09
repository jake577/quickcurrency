package com.jesttek.quickcurrencylibrary.database;

import android.provider.BaseColumns;

public class CurrencyPair {
    public static final String TABLE_NAME = "currency";
    public static final String KEY_CURRENCY1 = "currency1";
    public static final String KEY_CURRENCY2 = "currency2";
    public static final String KEY_DISPLAYED = "displayed";
    public static final String KEY_GRIDROW = "gridrow";


    public static final String[] TABLE_COLUMNS = {BaseColumns._ID, KEY_CURRENCY1, KEY_CURRENCY2, KEY_DISPLAYED, KEY_GRIDROW};

    private String mCurrency1;
    private String mCurrency2;
    private boolean mDisplayed;
    private int mGridRow;

    /**
     *
     * @param currency1 Currency being exchanged from
     * @param currency2 Currency being exchanged to
     * @param displayed If this pair is displayed on the watch grid
     * @param gridRow the row that this is displayed on the watch grid
     */
    public CurrencyPair(String currency1, String currency2, boolean displayed, int gridRow) {
        mCurrency1 = currency1;
        mCurrency2 = currency2;
        mDisplayed = displayed;
        mGridRow = gridRow;
    }

    /**
     *
     * @return The Currency being exchanged from
     */
    public String getCurrency1() {
        return mCurrency1;
    }

    /**
     *
     * @return The Currency being exchanged to
     */
    public String getCurrency2() {
        return mCurrency2;
    }

    /**
     *
     * @return True if this row is displayed on the watch grid
     */
    public boolean isDisplayed() {
        return mDisplayed;
    }

    /**
     *
     * @return the row that this is displayed on the watch grid
     */
    public int getGridRow() {
        return mGridRow;
    }
}
