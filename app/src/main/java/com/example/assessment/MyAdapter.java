package com.example.assessment;

import android.content.ContentValues;
import android.content.Context;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MyAdapter extends ArrayAdapter<Number> {

    MyAdapter(Context context) {
        super(context, R.layout.item_name);
    }

    @Override
    public View getView(final int position, View view, @NonNull ViewGroup parent) {
        if (view == null)
            view = View.inflate(getContext(), R.layout.item_name, null);

        final Number number = getItem(position);

        TextView tv = (TextView)view.findViewById(R.id.number);
        tv.setText(Number.wildcardsDbToView(number.number));

        tv = (TextView)view.findViewById(R.id.name);
        tv.setText(number.name);

        tv = (TextView)view.findViewById(R.id.rule);
        if(number.allow == 1) {
            tv.setText(R.string.allow);
        }
        else {
            tv.setText(R.string.block);
        }

        tv = (TextView)view.findViewById(R.id.stats);
        if (number.lastCall != null) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(getContext().getResources().getQuantityString(R.plurals.blacklist_call_details, number.timesCalled,
                    number.timesCalled, SimpleDateFormat.getDateTimeInstance().format(new Date(number.lastCall))));
        } else
            tv.setVisibility(View.GONE);

        return view;
    }
}
