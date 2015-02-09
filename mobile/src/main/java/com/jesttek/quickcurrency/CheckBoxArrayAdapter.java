package com.jesttek.quickcurrency;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import java.util.ArrayList;

public class CheckBoxArrayAdapter extends ArrayAdapter<CheckboxListItem> {

    private final Context mContext;

    public CheckBoxArrayAdapter(Context context, ArrayList<CheckboxListItem> itemsArrayList) {
        super(context, R.layout.checkbox_list_row, itemsArrayList);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.checkbox_list_row, parent, false);
        CheckedTextView cb = (CheckedTextView) rowView.findViewById(R.id.checkBox);
        cb.setText(getItem(position).Text);
        cb.setChecked(getItem(position).Activated);
        return rowView;
    }
}
