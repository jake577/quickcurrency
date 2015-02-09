package com.jesttek.quickcurrencylibrary.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;

public class SQLiteHelper extends SQLiteOpenHelper {

    public static final int CURRENT_VERSION = 1;
    public static final String DATABASE_NAME = "Exchange";
    public static final String CURRENCY_EXCHANGE_VIEW = "CurrencyExchange";
    private SQLiteStatement mPreparedInsertCurrencyPair;
    private SQLiteStatement mPreparedInsertExchange;
    public SQLiteHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, CURRENT_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + CurrencyPair.TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY, " +
                CurrencyPair.KEY_CURRENCY1 + " TEXT not null, " +    //currency1,currency2 are the currencies being exchanged
                CurrencyPair.KEY_CURRENCY2 + " TEXT not null, " +
                CurrencyPair.KEY_DISPLAYED + " INTEGER DEFAULT 1, " +            //If this pair will be displayed on the watch
                CurrencyPair.KEY_GRIDROW + " INTEGER);");            //The row that the currency pair is displayed in on the wearable

        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + Exchange.TABLE_NAME + "(" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Exchange.KEY_NAME + " TEXT not null, " +
                Exchange.KEY_URL + " TEXT not null, " +
                Exchange.KEY_ACTIVE + " INTEGER default 1, " +
                Exchange.FOREIGN_KEY_CURRENCYPAIR + " INTEGER not null, " +
                "FOREIGN KEY(" + Exchange.FOREIGN_KEY_CURRENCYPAIR + ") REFERENCES " + CurrencyPair.TABLE_NAME + "(" + BaseColumns._ID + ")" +
                ");");

        sqLiteDatabase.execSQL("CREATE VIEW " + CURRENCY_EXCHANGE_VIEW + " AS SELECT " + Exchange.TABLE_NAME + "." + BaseColumns._ID + " AS " + BaseColumns._ID + "," +
                        CurrencyPair.KEY_CURRENCY1 + "," +
                        CurrencyPair.KEY_CURRENCY2 + "," +
                        CurrencyPair.KEY_GRIDROW + "," +
                        CurrencyPair.KEY_DISPLAYED + "," +
                        Exchange.KEY_NAME + "," +
                        Exchange.KEY_URL + "," +
                        Exchange.KEY_ACTIVE + "," +
                        Exchange.FOREIGN_KEY_CURRENCYPAIR +
                        " FROM " + CurrencyPair.TABLE_NAME + " INNER JOIN " + Exchange.TABLE_NAME + " ON ("+CurrencyPair.TABLE_NAME +"."+BaseColumns._ID + " = " + Exchange.FOREIGN_KEY_CURRENCYPAIR + ")");

        String sql = "INSERT OR REPLACE INTO " + CurrencyPair.TABLE_NAME + " ( " + BaseColumns._ID + "," + CurrencyPair.KEY_CURRENCY1 + "," + CurrencyPair.KEY_CURRENCY2 + "," + CurrencyPair.KEY_DISPLAYED + "," + CurrencyPair.KEY_GRIDROW + " ) VALUES ( ?, ?, ?, ?, ? )";
        mPreparedInsertCurrencyPair = sqLiteDatabase.compileStatement(sql);
        sql = "INSERT OR REPLACE INTO " + Exchange.TABLE_NAME + " ( " + Exchange.KEY_NAME + "," + Exchange.KEY_URL + "," + Exchange.KEY_ACTIVE + "," + Exchange.FOREIGN_KEY_CURRENCYPAIR + " ) VALUES ( ?, ?, ?, ? )";
        mPreparedInsertExchange = sqLiteDatabase.compileStatement(sql);

        //populate tables
        sqLiteDatabase.beginTransactionNonExclusive();

        insertCurrencyPair(1, "BITCOIN", "USD", true, 0);
        insertCurrencyPair(2, "LITECOIN", "USD", true, 1);
        insertCurrencyPair(3, "LITECOIN", "BITCOIN", true, 2);
        insertCurrencyPair(4, "DOGECOIN", "USD", true, 3);
        insertCurrencyPair(5, "DOGECOIN", "BITCOIN", true, 4);

        //bitcoin usd
        insertExchange("bitfinex", "https://api.bitfinex.com/v1/pubticker/BTCUSD", true, 1);
        insertExchange("btce", "https://btc-e.com/api/2/btc_usd/ticker", true, 1);
        insertExchange("bter", "http://data.bter.com/api/1/ticker/btc_usd", true, 1);
        insertExchange("bitstamp", "https://www.bitstamp.net/api/ticker/", true, 1);

        //litecoin usd
        insertExchange("bitfinex", "https://api.bitfinex.com/v1/pubticker/LTCUSD", true, 2);
        insertExchange("btce", "https://btc-e.com/api/2/ltc_usd/ticker", true, 2);
        insertExchange("bter", "http://data.bter.com/api/1/ticker/ltc_usd", true, 2);

        //litecoin bitcoin
        insertExchange("bitfinex", "https://api.bitfinex.com/v1/pubticker/LTCBTC", true, 3);
        insertExchange("btce", "https://btc-e.com/api/2/ltc_btc/ticker", true, 3);
        insertExchange("bter", "http://data.bter.com/api/1/ticker/ltc_btc", true, 3);

        //dogecoin usd
        insertExchange("bter", "http://data.bter.com/api/1/ticker/doge_usd", true, 4);

        //dogecoin bitcoin
        insertExchange("bter", "http://data.bter.com/api/1/ticker/doge_btc", true, 5);

        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        //currently only one version exists. onUpgrade shouldn't happen, if it does drop and
        //recreate database;
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CurrencyPair.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Exchange.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    /**
     * Change the gridrow of a record and shift others accordingly
     * @param gridRowNew grid row to change to
     * @param gridRowOld grid row being changed from
     */
    public void changeRow(int gridRowOld, int gridRowNew) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor c = db.rawQuery("SELECT _id FROM " + CurrencyPair.TABLE_NAME + " WHERE gridRow = " + gridRowOld, null);
        c.moveToFirst();

        if(gridRowNew > gridRowOld) {
            db.execSQL("UPDATE " + CurrencyPair.TABLE_NAME + " SET gridRow = gridRow-1 WHERE gridRow > " +gridRowOld+ " AND gridRow <= " + gridRowNew);
        }
        else {
            db.execSQL("UPDATE " + CurrencyPair.TABLE_NAME + " SET gridRow = gridRow+1 WHERE gridRow >= " +gridRowNew+ " AND gridRow < " + gridRowOld);
        }
        db.execSQL("UPDATE " + CurrencyPair.TABLE_NAME + " SET gridRow = " +gridRowNew+ " WHERE _id = " + c.getInt(0));

        c.close();
        db.close();
    }

    /**
     * Convenience method for inserting Exchange using a prepared statement
     * @param name
     * @param URL
     * @param active
     * @param currencyPairId
     */
    private void insertExchange(String name, String URL, boolean active, int currencyPairId) {

        mPreparedInsertExchange.bindString(1, name);
        mPreparedInsertExchange.bindString(2, URL);
        mPreparedInsertExchange.bindLong(3, active ? 1 : 0);
        mPreparedInsertExchange.bindLong(4, currencyPairId);
        mPreparedInsertExchange.execute();
        mPreparedInsertExchange.clearBindings();
    }

    /**
     * Convenience method for inserting CurrencyPair using a prepared statement
     * @param id
     * @param currency1
     * @param currency2
     * @param displayed
     * @param gridRow
     */
    private void insertCurrencyPair(int id, String currency1, String currency2, boolean displayed, int gridRow) {

        mPreparedInsertCurrencyPair.bindLong(1, id);
        mPreparedInsertCurrencyPair.bindString(2, currency1);
        mPreparedInsertCurrencyPair.bindString(3, currency2);
        mPreparedInsertCurrencyPair.bindLong(4, displayed?1:0);
        mPreparedInsertCurrencyPair.bindLong(5, gridRow);
        mPreparedInsertCurrencyPair.execute();
        mPreparedInsertCurrencyPair.clearBindings();
    }
}
