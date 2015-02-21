package com.jesttek.quickcurrency.widget;

import android.app.Activity;
import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.jesttek.quickcurrency.R;
import com.jesttek.quickcurrencylibrary.CoinConstants;
import com.jesttek.quickcurrencylibrary.ExchangeConstants;
import com.jesttek.quickcurrencylibrary.database.CoinExchangeProvider;
import com.jesttek.quickcurrencylibrary.database.CurrencyPair;
import com.jesttek.quickcurrencylibrary.database.Exchange;

public class ExchangeWidgetConfigActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String PREFERENCE_FILE = "widgetPrefs";
    public static final String REFRESH_RATE_PREFERENCE = "refreshRate";
    public static final String CURRENCY_PAIR_PREFERENCE = "currencyPairID";
    public static final String CURRENCY_FROM_PREFERENCE = "currencyFrom";
    public static final String CURRENCY_TO_PREFERENCE = "currencyTo";
    public static final String EXCHANGE_NAME_PREFERENCE = "exchangeName";
    public static final String SETUP_PREFERENCE = "setupComplete";
    public static final String EXCHANGE_PREFERENCE = "exchangeID";

    private static final int EXCHANGE_CURSORLOADER_ID = 0;
    private static final int CURRENCY_CURSORLOADER_ID = 1;


    private SimpleCursorAdapter mExchangeAdapter;
    private SimpleCursorAdapter mCurrencyAdapter;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences mWidgetPreferences;
    private Spinner mCurrencyList;
    private Spinner mExchangeList;
    private EditText mEditText;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_widget);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt( AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        //set inital preferences for the widget
        mWidgetPreferences = getSharedPreferences(PREFERENCE_FILE + mAppWidgetId, Context.MODE_PRIVATE);
        SharedPreferences.Editor widgetPrefEditor = mWidgetPreferences.edit();
        widgetPrefEditor.putLong(EXCHANGE_PREFERENCE, 1);
        widgetPrefEditor.putString(CURRENCY_FROM_PREFERENCE, CoinConstants.BITCOIN.toString());
        widgetPrefEditor.putString(CURRENCY_TO_PREFERENCE, CoinConstants.USD.toString());
        widgetPrefEditor.putString(EXCHANGE_NAME_PREFERENCE, ExchangeConstants.Coinbase.toString());
        widgetPrefEditor.putBoolean(SETUP_PREFERENCE, false);
        widgetPrefEditor.commit();

        mEditText = (EditText) findViewById(R.id.refresh_rate_edittext);
        mEditText.setOnFocusChangeListener( new View.OnFocusChangeListener() {
           @Override
           public void onFocusChange(View v, boolean hasFocus) {
               if(Integer.parseInt(mEditText.getText().toString()) < 5) {
                   mEditText.setText("5");
               }
           }
       });

        Button button = (Button) findViewById(R.id.done_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context context = ExchangeWidgetConfigActivity.this;

                SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                int rate = Integer.parseInt(mEditText.getText().toString());
                if(rate < 5) {
                    mEditText.setText("5");
                    rate = 5;
                }

                editor.putInt(REFRESH_RATE_PREFERENCE, rate*1000*60);
                editor.commit();
                SharedPreferences.Editor widgetPrefEditor = mWidgetPreferences.edit();
                widgetPrefEditor.putBoolean(SETUP_PREFERENCE, true);
                widgetPrefEditor.commit();

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ExchangeWidgetProvider.updateConfig(context, appWidgetManager, mAppWidgetId);
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });

        mCurrencyList = (Spinner) findViewById(R.id.currency_select_Spinner);
        mCurrencyAdapter = new SimpleCursorAdapter(this,
                R.layout.currency_pair_list_item,
                null,
                new String[]{CurrencyPair.KEY_CURRENCY1, CurrencyPair.KEY_CURRENCY2},
                new int[]{R.id.text1, R.id.text2}, 0);
        mCurrencyList.setAdapter(mCurrencyAdapter);
        mCurrencyList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Bundle args = new Bundle();
                args.putLong("id",id);
                getLoaderManager().restartLoader(EXCHANGE_CURSORLOADER_ID, args, ExchangeWidgetConfigActivity.this);

                SharedPreferences.Editor widgetPrefEditor = mWidgetPreferences.edit();
                Cursor cursor = (Cursor) mCurrencyAdapter.getItem(position);
                widgetPrefEditor.putLong(CURRENCY_PAIR_PREFERENCE, id);
                widgetPrefEditor.putString(CURRENCY_FROM_PREFERENCE, cursor.getString(1));
                widgetPrefEditor.putString(CURRENCY_TO_PREFERENCE, cursor.getString(2));
                widgetPrefEditor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mExchangeList = (Spinner) findViewById(R.id.exchange_select_Spinner);
        mExchangeAdapter = new SimpleCursorAdapter(this,
                R.layout.simple_highlighted_list_item,
                null,
                new String[]{Exchange.KEY_NAME},
                new int[]{R.id.text1}, 0);
        mExchangeList.setAdapter(mExchangeAdapter);
        mExchangeList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SharedPreferences.Editor widgetPrefEditor = mWidgetPreferences.edit();
                widgetPrefEditor.putLong(EXCHANGE_PREFERENCE, id);
                Cursor cursor = (Cursor) mExchangeAdapter.getItem(position);
                widgetPrefEditor.putString(EXCHANGE_NAME_PREFERENCE, cursor.getString(1));
                widgetPrefEditor.commit();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        getLoaderManager().initLoader(CURRENCY_CURSORLOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        switch (i) {
            case EXCHANGE_CURSORLOADER_ID:
                String selection = Exchange.FOREIGN_KEY_CURRENCYPAIR + "=?";
                String[] selectionArgs = {"1"};
                if(bundle!=null)
                {
                    selectionArgs[0] = String.valueOf(bundle.getLong("id"));
                }
                String[] exchangeCols = {BaseColumns._ID, Exchange.KEY_NAME};
                CursorLoader exchangeLoader = new CursorLoader(this, CoinExchangeProvider.EXCHANGE_CONTENT_URI, exchangeCols, selection, selectionArgs, null);
                return exchangeLoader;
            case CURRENCY_CURSORLOADER_ID:
                String[] currencyCols = {BaseColumns._ID, CurrencyPair.KEY_CURRENCY1, CurrencyPair.KEY_CURRENCY2};
                CursorLoader currencyLoader = new CursorLoader(this, CoinExchangeProvider.CURRENCY_CONTENT_URI, currencyCols, null, null, null);
                return currencyLoader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case EXCHANGE_CURSORLOADER_ID:
                mExchangeAdapter.swapCursor(cursor);
                break;
            case CURRENCY_CURSORLOADER_ID:
                mCurrencyAdapter.swapCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        switch (cursorLoader.getId()) {
            case EXCHANGE_CURSORLOADER_ID:
                mExchangeAdapter.swapCursor(null);
                break;
            case CURRENCY_CURSORLOADER_ID:
                mCurrencyAdapter.swapCursor(null);
                break;
        }
    }
}
