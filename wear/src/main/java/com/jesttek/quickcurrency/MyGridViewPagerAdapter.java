package com.jesttek.quickcurrency;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.util.SparseArray;
import android.view.Gravity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MyGridViewPagerAdapter extends FragmentGridPagerAdapter {
    private final Context mContext;
    private SparseArray<SparseArray<WeakReference<Fragment>>> mFragments = new SparseArray<SparseArray<WeakReference<Fragment>>>();
    private ArrayList<ArrayList<GridPage>> mPages = null;

    public MyGridViewPagerAdapter(Context ctx, FragmentManager fm, ArrayList<ArrayList<GridPage>> pages) {
        super(fm);
        mContext = ctx;
        if(pages == null) {
            pages = new ArrayList<ArrayList<GridPage>>();
        }
        mPages = pages;
    }

    @Override
    public Fragment getFragment(int row, int col) {
        if(mPages == null || mPages.size() == 0) {
            CardFragment fragment = CardFragment.create("Loading", "");
            fragment.setCardGravity(Gravity.CENTER);
            fragment.setExpansionEnabled(true);
            fragment.setExpansionDirection(CardFragment.EXPAND_DOWN);
            fragment.setExpansionFactor(1.0f);
            return fragment;
        }
        else {
            GridPage page = mPages.get(row).get(col);
            if(page.poller) {
                //If this is the first fragment created, start it polling straight away. Other fragments are
                //just started when switching to the page they are on.
                boolean active = (row == 0 && col == 0);
                NetworkPollFragment fragment = NetworkPollFragment.create(page.id, page.currencyFrom, page.currencyTo, page.title, active);
                if (mFragments.get(row) == null) {
                    mFragments.put(row, new SparseArray<WeakReference<Fragment>>());
                }
                mFragments.get(row).put(col, new WeakReference<Fragment>(fragment));
                return fragment;
            }
            else {
                CardFragment fragment = CardFragment.create(page.title, page.message);
                fragment.setCardGravity(Gravity.CENTER);
                fragment.setExpansionEnabled(true);
                fragment.setExpansionDirection(CardFragment.EXPAND_DOWN);
                fragment.setExpansionFactor(1.0f);
                return fragment;
            }
        }
    }

    public Fragment getLoadedFragment(int row, int col) {
        SparseArray<WeakReference<Fragment>> rowList = mFragments.get(row, new SparseArray<WeakReference<Fragment>>());
        return rowList.get(col) == null ? null:rowList.get(col).get();
    }

    @Override
    public Drawable getBackgroundForRow(int row) {
        if(mPages != null && mPages.size() > 0 && mPages.get(row).get(0).poller) {
            GridPage currentPage = mPages.get(row).get(0);
            Drawable[] backgroundImages = new Drawable[3];
            backgroundImages[0] = new ColorDrawable(Color.WHITE);
            backgroundImages[1] = mContext.getResources().getDrawable(currentPage.currencyFrom.getIconResource());
            backgroundImages[2] = mContext.getResources().getDrawable(currentPage.currencyTo.getIconResource());
            LayerDrawable background = new LayerDrawable(backgroundImages);
            backgroundImages[1].setAlpha(50);
            backgroundImages[2].setAlpha(50);
            int scale = (mPages.get(row).size()-1)*15;
            background.setLayerInset(1,-75+scale,-25+scale,75+scale,25+scale);
            background.setLayerInset(2,75+scale,25+scale,-75+scale,-25+scale);

            Bitmap b = Bitmap.createBitmap(530, 530, Bitmap.Config.ARGB_8888);
            background.setBounds(0, 0, 530, 530);
            background.draw(new Canvas(b));

            return new BitmapDrawable(mContext.getResources(), b);
        }

        return mContext.getResources().getDrawable( R.drawable.default_background );
    }

    @Override
    public int getRowCount() {
        if(mPages != null && mPages.size() > 0) {
            return mPages.size();
        }
        return 1; //A default page is still displayed when the adapter is empty
    }

    @Override
    public int getColumnCount(int rowNum) {
        if(mPages != null && mPages.size() > 0) {
            return mPages.get(rowNum).size();
        }
        return 1; //A default page is still displayed when the adapter is empty
    }

    public void setPages(ArrayList<ArrayList<GridPage>> pages) {
        mPages = pages;
    }

    /**
     * Adds page as new row
     * @param page
     */
    public void addPage(GridPage page) {
        ArrayList<GridPage> newRow = new ArrayList<GridPage>();
        newRow.add(page);
        mPages.add(newRow);
    }

    /**
     * inserts page at given position
     * @param page
     * @param row
     * @param col
     */
    public void addPage(GridPage page, int row, int col) {
        mPages.get(row).add(col, page);
    }

    /**
     * deletes all pages
     */
    public void clearPages() {
        mPages.clear();
    }
}