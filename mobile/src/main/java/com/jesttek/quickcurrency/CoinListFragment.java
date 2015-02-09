package com.jesttek.quickcurrency;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.ListView;

import com.jesttek.quickcurrencylibrary.database.CoinExchangeProvider;
import com.jesttek.quickcurrencylibrary.database.CurrencyPair;
import com.jesttek.quickcurrencylibrary.database.SQLiteHelper;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.dragdrop.DraggableManager;
import com.nhaarman.listviewanimations.itemmanipulation.dragdrop.OnItemMovedListener;

import java.util.ArrayList;

/**
 * A list fragment representing a list of Coins. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link CoinDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link ListCallbacks}
 * interface.
 */
public class CoinListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    //The serialization (saved instance state) Bundle key representing the
    //activated item position. Only used on tablets.
    private static final int ADD_COIN_RESULT = 0;
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    //The fragment's current callback object, which is notified of list item clicks.
    private ListCallbacks mCallbacks = sDummyCallbacks;

    // The current activated item position. Only used on tablets.
    private int mActivatedPosition = ListView.INVALID_POSITION;

    SQLiteHelper mSQLiteHelper;

    private ItemPairAdapter mAdapter;
    ArrayList<ItemPair> mList = new ArrayList<>();

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface ListCallbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(long id);
    }

    /**
     * A dummy implementation of the {@link ListCallbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static ListCallbacks sDummyCallbacks = new ListCallbacks() {
        @Override
        public void onItemSelected(long id) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CoinListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSQLiteHelper = new SQLiteHelper(this.getActivity());
        mAdapter = new ItemPairAdapter(this.getActivity(), R.layout.currency_pair_list_item, mList);

        setListAdapter(mAdapter);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View v = inflater.inflate(R.layout.fragment_coin_list, container);
        DynamicListView listView = (DynamicListView)v.findViewById(android.R.id.list);
        listView.enableDragAndDrop();
        listView.setDraggableManager(new DraggableManager() {
            @Override
            public boolean isDraggable(@NonNull View view, int i, float v, float v2) {
                return true;
            }
        });
        listView.setOnItemMovedListener(new OnItemMovedListener() {
            @Override
            public void onItemMoved(int i, int i2) {
                mSQLiteHelper.changeRow(i, i2);
            }
        });
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof ListCallbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (ListCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(mList.get(position).Id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {

        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    public void refreshList() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] columns = {"_id", "currency1", "currency2"};
        String[] selectionArgs = {"1"};
        CursorLoader loader = new CursorLoader(getActivity(), CoinExchangeProvider.CURRENCY_CONTENT_URI, columns, CurrencyPair.KEY_DISPLAYED + " = ?", selectionArgs, CurrencyPair.KEY_GRIDROW + " ASC");
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mList.clear();
        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            mList.add(new ItemPair(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mList.clear();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.currency_list_actions, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ADD_COIN_RESULT) {
            refreshList();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                CoinSelectFragment dialog = new CoinSelectFragment();
                dialog.setTargetFragment(this, ADD_COIN_RESULT);
                dialog.show(getFragmentManager(), "addDialog");
                return true;
            case R.id.action_help:
                AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this.getActivity());
                dlgAlert.setView(LayoutInflater.from(this.getActivity()).inflate(R.layout.help_dialog,null));
                dlgAlert.setTitle("HOW TO USE");
                dlgAlert.setPositiveButton("OK", null);
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}