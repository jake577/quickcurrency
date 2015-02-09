package com.jesttek.quickcurrency;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.jesttek.quickcurrencylibrary.database.CoinExchangeProvider;
import com.jesttek.quickcurrencylibrary.database.CurrencyPair;
import com.jesttek.quickcurrencylibrary.database.Exchange;

import java.util.ArrayList;

/**
 * A fragment representing a single Coin detail screen.
 * This fragment is either contained in a {@link CoinListActivity}
 * in two-pane mode (on tablets) or a {@link CoinDetailActivity}
 * on handsets.
 */
public class CoinDetailFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String COINPAIR_KEY = "COINPAIR_ID";
    private long mCoinPairId = -1;
    private CheckBoxArrayAdapter mListAdapter;

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private DetailCallbacks mCallbacks = sDummyCallbacks;

    /**
     * A dummy implementation of the {@link DetailCallbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static DetailCallbacks sDummyCallbacks = new DetailCallbacks() {
        @Override
        public void onItemDeleted() {
        }
    };

    public interface DetailCallbacks {
        /**
         * Callback for when an item has been deleted and needs the list to be refreshed
         */
        public void onItemDeleted();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CoinDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(COINPAIR_KEY)) {
            mCoinPairId = getArguments().getLong(COINPAIR_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListAdapter = new CheckBoxArrayAdapter(getActivity(), new ArrayList<CheckboxListItem>());
        setListAdapter(mListAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof DetailCallbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }
        mCallbacks = (DetailCallbacks) activity;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Loads exchanges that selected currency pair is listed on
        String selection = Exchange.FOREIGN_KEY_CURRENCYPAIR + "=?";
        String[] selectionArgs = {Long.toString(mCoinPairId)};
        String[] columns = {BaseColumns._ID, Exchange.KEY_NAME, Exchange.KEY_ACTIVE};
        CursorLoader loader = new CursorLoader(getActivity(), CoinExchangeProvider.EXCHANGE_CONTENT_URI, columns, selection, selectionArgs, null);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Add items from the cursor
        data.moveToFirst();
        do {
            mListAdapter.add(new CheckboxListItem(data.getString(1), data.getInt(2)==1, data.getInt(0)));
        } while (data.moveToNext());

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        CheckedTextView checkbox = (CheckedTextView) v;
        checkbox.setChecked(!checkbox.isChecked());

        ContentValues values = new ContentValues();
        if(checkbox.isChecked()) {
            values.put(Exchange.KEY_ACTIVE, 1);
        }
        else {
            values.put(Exchange.KEY_ACTIVE, 0);
        }

        getActivity().getContentResolver().update(CoinExchangeProvider.EXCHANGE_CONTENT_URI, values, BaseColumns._ID + " = ?", new String[] {Long.toString(mListAdapter.getItem(position).ID)});
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.currency_detail_actions, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove:
                ContentValues values = new ContentValues();
                values.put(CurrencyPair.KEY_DISPLAYED, 0);
                getActivity().getContentResolver().update(CoinExchangeProvider.CURRENCY_CONTENT_URI, values, BaseColumns._ID + " = ?", new String[]{Long.toString(mCoinPairId)});
                mCallbacks.onItemDeleted();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}