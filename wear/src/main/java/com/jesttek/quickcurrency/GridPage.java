package com.jesttek.quickcurrency;

import com.jesttek.quickcurrencylibrary.CoinConstants;

public class GridPage {

    int id;
    String title;
    CoinConstants currencyFrom;
    CoinConstants currencyTo;
    String message;
    boolean poller = false;

    /**
     * Page that uses the NetworkPollFragment to display current exchange prices
     * @param id id for currency pair on given exchange
     * @param exchange Exchange used for conversion
     * @param from Currency being exchanged from
     * @param to Currency being exchanged to
     */
    public GridPage(int id, String exchange, CoinConstants from, CoinConstants to) {
        this.id = id;
        this.title = exchange;
        currencyFrom = from;
        currencyTo = to;
        poller = true;
    }

    /**
     * Simple page that uses a cardFragment to display a message
     * @param title Title to display
     * @param message Message to display
     */
    public GridPage(String title, String message) {
        this.message = message;
        this.title= title;
        poller = false;
    }
}