package com.jesttek.quickcurrency;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;


/**
 * An activity representing a list of Coins. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link CoinDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link CoinListFragment} and the item details
 * (if present) is a {@link CoinDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link CoinListFragment.ListCallbacks} interface
 * to listen for item selections.
 */
public class CoinListActivity extends FragmentActivity implements CoinListFragment.ListCallbacks, CoinDetailFragment.DetailCallbacks {

    private static final int DETAIL_RESULT = 0;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_list);

        if (findViewById(R.id.coin_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((CoinListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.coin_list))
                    .setActivateOnItemClick(true);
        }
    }

    /**
     * Callback method from {@link CoinListFragment.ListCallbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(long id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putLong(CoinDetailFragment.COINPAIR_KEY, id);
            CoinDetailFragment fragment = new CoinDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.coin_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, CoinDetailActivity.class);
            detailIntent.putExtra(CoinDetailFragment.COINPAIR_KEY, id);
            startActivityForResult(detailIntent, DETAIL_RESULT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        if(resultCode == DETAIL_RESULT) {
            CoinListFragment listFrag = (CoinListFragment)getSupportFragmentManager().findFragmentById(R.id.coin_list);
            listFrag.refreshList();
        }
    }

    @Override
    public void onItemDeleted() {
        if (mTwoPane) {
            CoinListFragment listFrag = (CoinListFragment)getSupportFragmentManager().findFragmentById(R.id.coin_list);
            listFrag.refreshList();
            CoinDetailFragment detailFrag = (CoinDetailFragment)getSupportFragmentManager().findFragmentById(R.id.coin_detail_container);
            getSupportFragmentManager().beginTransaction().hide(detailFrag).commit();
        } else {
            // In single-pane mode this activity shouldn't be open and this method won't be called. Just don't do anything.
        }
    }
}