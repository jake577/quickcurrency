package com.jesttek.quickcurrency.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.jesttek.quickcurrency.R;
import com.jesttek.quickcurrencylibrary.CoinConstants;
import com.jesttek.quickcurrencylibrary.ExchangeConstants;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.AbstractExchange;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Bitfinex;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Bitstamp;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Btce;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Bter;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Coinbase;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Cryptsy;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.DataLoadedListener;
import com.jesttek.quickcurrencylibrary.ExchangeControllers.Kraken;
import com.jesttek.quickcurrencylibrary.database.CoinExchangeProvider;
import com.jesttek.quickcurrencylibrary.database.Exchange;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;

public class WidgetService extends Service implements DataLoadedListener {
    private static final String TAG = WidgetService.class.getName();
    private RequestQueue mQueue;
    private int mWaitingOn = 0;
    private boolean mComplete = false;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mQueue = Volley.newRequestQueue(getApplicationContext());

        Object obj = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        if(obj == null) {
            stopSelf();
        }

        int[] appWidgetIds = (int[])obj;

        for (int widgetId : appWidgetIds) {
            SharedPreferences widgetPreferences = getSharedPreferences(ExchangeWidgetConfigActivity.PREFERENCE_FILE + widgetId, Context.MODE_PRIVATE);
            if (widgetPreferences.getBoolean(ExchangeWidgetConfigActivity.SETUP_PREFERENCE, false)) {
                mWaitingOn++;
                loadData(widgetId, (int)widgetPreferences.getLong(ExchangeWidgetConfigActivity.EXCHANGE_PREFERENCE, -1));
            }
        }
        mComplete = true;
        if(mWaitingOn == 0) {
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void loadData(int widgetId, int exchangeId) {
        String[] columns = new String[] {Exchange.KEY_NAME, Exchange.KEY_URL_ID};
        String[] args = {Integer.toString(exchangeId)};
        Cursor c = getContentResolver().query(CoinExchangeProvider.EXCHANGE_CONTENT_URI, columns, BaseColumns._ID + " = ?", args, null);
        if(c.moveToFirst()) {
            String urlId = c.getString(1);
            final ExchangeConstants exchangeName = ExchangeConstants.valueOf(c.getString(0));

            if (urlId != null) {

                AbstractExchange exchange = null;
                switch (exchangeName) {
                    case Bitfinex:
                        exchange = new Bitfinex(this, String.valueOf(widgetId), exchangeId);
                        break;
                    case Bitstamp:
                        exchange = new Bitstamp(this, String.valueOf(widgetId), exchangeId);
                        break;
                    case Btce:
                        exchange = new Btce(this, String.valueOf(widgetId), exchangeId);
                        break;
                    case Bter:
                        exchange = new Bter(this, String.valueOf(widgetId), exchangeId);
                        break;
                    case Coinbase:
                        exchange = new Coinbase(this, String.valueOf(widgetId), exchangeId);
                        break;
                    case Cryptsy:
                        exchange = new Cryptsy(this, String.valueOf(widgetId), exchangeId);
                        break;
                    case Kraken:
                        exchange = new Kraken(this, String.valueOf(widgetId), exchangeId);
                        break;
                }
                exchange.getData(mQueue, urlId);

            } else {
                //message was received for a website app doesn't support. Should never occur
                Log.w(TAG, "invalid exchange request: " + exchangeName);
            }
        }
        else {
            //Requested exchange id didn't exist
            Log.w(TAG, "invalid exchange id request: " + exchangeId);
        }
        c.close();
    }

    @Override
    public void onDataLoaded(String nodeId, int exchangeId, String last, String high, String low) {
        int widgetId = Integer.parseInt(nodeId);
        BigDecimal lastPrice = new BigDecimal(last);
        lastPrice = lastPrice.round(new MathContext(8));
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(8);
        df.setMinimumFractionDigits(0);
        df.setGroupingUsed(false);
        updateWidget(widgetId, df.format(lastPrice));
    }

    @Override
    public void onLoadFailed(String nodeId, int exchangeId, String message) {
        int widgetId = Integer.parseInt(nodeId);
        updateWidget(widgetId, "NetworkError");
    }

    private void updateWidget (int widgetId, String text) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
        SharedPreferences widgetPreferences = getSharedPreferences(ExchangeWidgetConfigActivity.PREFERENCE_FILE + widgetId, Context.MODE_PRIVATE);

        ExchangeConstants exchange = ExchangeConstants.valueOf(widgetPreferences.getString(ExchangeWidgetConfigActivity.EXCHANGE_NAME_PREFERENCE, ExchangeConstants.Coinbase.toString()));
        CoinConstants from = CoinConstants.valueOf(widgetPreferences.getString(ExchangeWidgetConfigActivity.CURRENCY_FROM_PREFERENCE, CoinConstants.BITCOIN.toString()));
        CoinConstants to = CoinConstants.valueOf(widgetPreferences.getString(ExchangeWidgetConfigActivity.CURRENCY_TO_PREFERENCE, CoinConstants.USD.toString()));

        RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_exchange);
        remoteViews.setTextViewText(R.id.exchange_textview, exchange.toString());
        remoteViews.setImageViewResource(R.id.currency_from_imageview, from.getIconResource());
        remoteViews.setImageViewResource(R.id.currency_to_imageview, to.getIconResource());
        remoteViews.setTextViewText(R.id.last_textview, text);

        Intent clickIntent = new Intent(this, ExchangeWidgetProvider.class);
        clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {widgetId} );
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        appWidgetManager.updateAppWidget(widgetId, remoteViews);

        mWaitingOn--;
        if(mWaitingOn == 0 && mComplete) {
            stopSelf();
        }
    }
}
