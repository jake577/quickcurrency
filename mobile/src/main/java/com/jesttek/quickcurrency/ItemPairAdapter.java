package com.jesttek.quickcurrency;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nhaarman.listviewanimations.ArrayAdapter;

import java.util.ArrayList;

public class ItemPairAdapter extends ArrayAdapter {

    private final Context mContext;
    private final ArrayList<ItemPair> mData;
    private final int mLayoutResourceId;

    public ItemPairAdapter(Context context, int layoutResourceId, ArrayList<ItemPair> data) {
        super(data);
        this.mContext = context;
        this.mData = data;
        this.mLayoutResourceId = layoutResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.textView1 = (TextView)row.findViewById(R.id.text1);
            holder.textView2 = (TextView)row.findViewById(R.id.text2);

            row.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)row.getTag();
        }

        ItemPair pair = mData.get(position);

        holder.textView1.setText(pair.Item1Name);
        holder.textView2.setText(pair.Item2Name);

        return row;
    }

    @Override
    public long getItemId(final int location) {
        if(location >= this.getItems().size() || this.isEmpty()) {
            return -1;
        }
        return this.getItem(location).hashCode();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    static class ViewHolder
    {
        TextView textView1;
        TextView textView2;
        //ImageView imageView1;
        //ImageView imageView2;
    }
}
