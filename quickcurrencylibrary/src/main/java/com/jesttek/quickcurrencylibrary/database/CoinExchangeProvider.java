package com.jesttek.quickcurrencylibrary.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

public class CoinExchangeProvider extends ContentProvider {
    private SQLiteHelper mDBHelper;
    private static final String AUTHORITY = "com.jesttek.quickcurrencylibrary.database.CoinExchangeProvider";
    private static final int CURRENCY = 1;
    private static final int CURRENCY_ID = 2;
    private static final int EXCHANGE = 3;
    private static final int EXCHANGE_ID = 4;
    public static final Uri CURRENCY_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/Currency");
    public static final Uri EXCHANGE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/Exchange");

    private static final UriMatcher mURIMatcher;
    static {
        mURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mURIMatcher.addURI(AUTHORITY, "Currency", CURRENCY);
        mURIMatcher.addURI(AUTHORITY, "Currency/#", CURRENCY_ID);
        mURIMatcher.addURI(AUTHORITY, "Exchange", EXCHANGE);
        mURIMatcher.addURI(AUTHORITY, "Exchange/#", EXCHANGE_ID);
    }

    public static final String CURRENCY_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/Currency";
    public static final String CURRENCY_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/Currency";
    public static final String EXCHANGE_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/Exchange";
    public static final String EXCHANGE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/Exchange";

    @Override
    public boolean onCreate() {
        mDBHelper = new SQLiteHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case CURRENCY_ID:
                queryBuilder.appendWhere(CurrencyPair.TABLE_NAME + "."+ BaseColumns._ID +" =" + uri.getLastPathSegment());
            case CURRENCY:
                queryBuilder.setTables(CurrencyPair.TABLE_NAME );
                break;
            case EXCHANGE_ID:
                queryBuilder.appendWhere(BaseColumns._ID +" =" + uri.getLastPathSegment());
            case EXCHANGE:
                queryBuilder.setTables(SQLiteHelper.CURRENCY_EXCHANGE_VIEW);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }

        Cursor cursor = queryBuilder.query(mDBHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case CURRENCY:
                return CURRENCY_CONTENT_TYPE;
            case CURRENCY_ID:
                return CURRENCY_CONTENT_ITEM_TYPE;
            case EXCHANGE:
                return EXCHANGE_CONTENT_TYPE;
            case EXCHANGE_ID:
                return EXCHANGE_CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        int uriType = mURIMatcher.match(uri);
        String table = "";
        Uri returnUri = null;
        switch (uriType) {
            case CURRENCY_ID:
            case CURRENCY:
                returnUri = CURRENCY_CONTENT_URI;
                table = CurrencyPair.TABLE_NAME;
                break;
            case EXCHANGE_ID:
            case EXCHANGE:
                returnUri = EXCHANGE_CONTENT_URI;
                table = Exchange.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
        return ContentUris.withAppendedId(returnUri, mDBHelper.getWritableDatabase().insert(table, null, values));
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = mURIMatcher.match(uri);
        String table = "";
        switch (uriType) {
            case CURRENCY_ID:
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
            case CURRENCY:
                table = CurrencyPair.TABLE_NAME;
                break;
            case EXCHANGE_ID:
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
            case EXCHANGE:
                table = Exchange.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
        return mDBHelper.getWritableDatabase().delete(table, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = mURIMatcher.match(uri);
        String table = "";
        switch (uriType) {
            case CURRENCY_ID:
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
            case CURRENCY:
                table = CurrencyPair.TABLE_NAME;
                break;
            case EXCHANGE_ID:
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
            case EXCHANGE:
                table = Exchange.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }
        return mDBHelper.getWritableDatabase().update(table, values, selection, selectionArgs);
    }
}
