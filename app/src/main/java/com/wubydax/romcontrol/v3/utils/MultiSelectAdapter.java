package com.wubydax.romcontrol.v3.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.wubydax.romcontrol.v3.R;

import java.util.ArrayList;

public class MultiSelectAdapter
        extends RecyclerView.Adapter<MultiSelectAdapter.MultiSelectViewHolder> {
    private final ArrayList<SelectionItem> mItemsList;
    private final OnItemSelectedListener mOnItemSelectedListener;

    public MultiSelectAdapter(ArrayList<SelectionItem> list, OnItemSelectedListener listener) {
        mItemsList = list;
        mOnItemSelectedListener = listener;
    }

    public String getSelectedItems() {
        StringBuilder stringBuilder = new StringBuilder();
        for (SelectionItem selectionItem : mItemsList) {
            if (selectionItem.isSelected) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(",");
                }
                stringBuilder.append(selectionItem.value);
            }
        }
        return stringBuilder.toString();
    }

    public void setSelectedItems(String selectedItems) {
        if (!TextUtils.isEmpty(selectedItems)) {
            String[] items = selectedItems.split(",");
            if (items.length > 0) {
                for (String item : items) {
                    for (SelectionItem selectionItem : mItemsList) {
                        if (selectionItem.value.equals(item)) {
                            selectionItem.isSelected = true;
                            mOnItemSelectedListener.onItemSelected(true);
                        }
                    }
                }
                notifyDataSetChanged();
            }
        }
    }


    public void selectAll(boolean select) {
        for (SelectionItem selectionItem : mItemsList) {
            selectionItem.isSelected = select;
        }
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public MultiSelectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MultiSelectViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.multi_select_preference_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(MultiSelectViewHolder holder, int position) {
        holder.bindViews(mItemsList.get(position));
    }

    @Override
    public int getItemCount() {
        return mItemsList != null ? mItemsList.size() : 0;
    }

    public interface OnItemSelectedListener {
        void onItemSelected(boolean isSelected);
    }

    class MultiSelectViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView textView;

        MultiSelectViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.multi_select_checkbox);
            textView = itemView.findViewById(R.id.multi_select_entry_text);
        }

        void bindViews(SelectionItem selectionItem) {
            textView.setText(selectionItem.entry);
            checkBox.setChecked(selectionItem.isSelected);
            itemView.setOnClickListener(view -> {
                checkBox.setChecked(!checkBox.isChecked());
                mItemsList.get(getAdapterPosition()).isSelected = checkBox.isChecked();
                mOnItemSelectedListener.onItemSelected(checkBox.isChecked());
            });
        }
    }


}
