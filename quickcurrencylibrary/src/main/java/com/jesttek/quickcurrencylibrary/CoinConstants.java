package com.jesttek.quickcurrencylibrary;

public enum CoinConstants {
    BITCOIN ("Bitcoin", "BTC", R.drawable.bitcoin),
    DOGECOIN ("Dogecoin", "DOGE", R.drawable.dogecoin),
    LITECOIN ("Litecoin", "LTC", R.drawable.litecoin),
    DARKCOIN ("Darkcoin", "DRK", R.drawable.darkcoin),
    USD ("USD", "USD", R.drawable.usd);

    private final String mName;
    private final String mShortName;
    private final int mIconResource;

    CoinConstants(String longName, String shortName, int resource) {
        mShortName = shortName;
        mName = longName;
        mIconResource = resource;
    }

    public String getName() {
        return mName;
    }

    public String getShortName() {
        return mShortName;
    }

    public int getIconResource() {
        return mIconResource;
    }
}
