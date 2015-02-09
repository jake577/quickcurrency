package com.jesttek.quickcurrency;


import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.jesttek.quickcurrencylibrary.database.CoinExchangeProvider;
import com.jesttek.quickcurrencylibrary.database.CurrencyPair;

/**
 * A fragment representing a list of Items.
 */
public class CoinSelectFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private AbsListView mListView;
    private SimpleCursorAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CoinSelectFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().setTitle("Select Exchange Pair");
        mListView = (ListView) getView().findViewById(android.R.id.list);
        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.currency_pair_list_item,
                null,
                new String[] { CurrencyPair.KEY_CURRENCY1, CurrencyPair.KEY_CURRENCY2},
                new int[] { R.id.text1 , R.id.text2 }, 0);
        mListView.setAdapter(mAdapter);

        Button okButton = (Button) getView().findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ContentValues values = new ContentValues();
                values.put(CurrencyPair.KEY_DISPLAYED, 1);
                int count = mListView.getCheckedItemCount();
                long[] selected = mListView.getCheckedItemIds();
                for(int i = 0; i < count; i++) {
                    getActivity().getContentResolver().update(CoinExchangeProvider.CURRENCY_CONTENT_URI, values, BaseColumns._ID + " = ?", new String[]{String.valueOf(selected[i])});
                }
                getTargetFragment().onActivityResult(getTargetRequestCode(), 0, null);
                dismiss();
            }
        });

        Button cancelButton = (Button) getView().findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout view = (LinearLayout)inflater.inflate(R.layout.fragment_currencyselect, container, false);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        //Load currencies for the list
        String selection = CurrencyPair.KEY_DISPLAYED + "=?";
        String[] selectionArgs = {"0"};
        String[] columns = {BaseColumns._ID, CurrencyPair.KEY_CURRENCY1, CurrencyPair.KEY_CURRENCY2};
        CursorLoader loader = new CursorLoader(getActivity(), CoinExchangeProvider.CURRENCY_CONTENT_URI, columns, selection, selectionArgs, null);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
        if(data.getCount() == 0) {
            Button cancelButton = (Button)getView().findViewById(R.id.cancel_button);
            TextView emptyText = (TextView)getView().findViewById(R.id.empty_text);
            mListView.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        }
        else {
            mAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }
}
