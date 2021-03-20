package com.wubydax.romcontrol.v3.prefs;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.Utils;

import java.util.ArrayList;


public class MultipleChoicePreference extends DialogPreference {
    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final String mReverseDependencyKey;
    private final String[] mTitles;
    private final String[] mKeys;
    private final String[] mSummaries;
    private final int[] mDefaults;
    private final int mSelector;
    private MultipleChoiceAdapter mMultipleChoiceAdapter;
    private final boolean mIsRebootRequired;
    private final boolean mIsShowSelectAll;


    public MultipleChoicePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context
                .obtainStyledAttributes(attrs, R.styleable.MultipleChoicePreference);
        mTitles = context.getResources().getStringArray(typedArray.getResourceId(R.styleable
                                .MultipleChoicePreference_multipleChoiceTitles, -1));
        mKeys = context.getResources().getStringArray(typedArray.getResourceId(R.styleable
                        .MultipleChoicePreference_multipleChoiceKeys, -1));
        mSummaries = context.getResources().getStringArray(typedArray.getResourceId(R.styleable
                .MultipleChoicePreference_multipleChoiceSummaries, -1));
        mDefaults = context.getResources().getIntArray(typedArray.getResourceId(R.styleable
                .MultipleChoicePreference_multipleChoiceDefaults, -1));
        mSelector = typedArray.getInt(R.styleable.MultipleChoicePreference_choiceSelector,
                0);
        mIsShowSelectAll = typedArray.getBoolean(R.styleable.MultipleChoicePreference_showSelectAll,
                false);
        TypedArray generalTypedArray = context.obtainStyledAttributes(attrs, R.styleable
                .Preference);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mReverseDependencyKey = generalTypedArray.getString(R.styleable
                .Preference_reverseDependency);
        mIsRebootRequired = generalTypedArray.getBoolean(R.styleable.Preference_rebootDevice,
                false);
        generalTypedArray.recycle();
        typedArray.recycle();
        setDialogLayoutResource(R.layout.multiple_choice_preference_dialog_layout);
    }


    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        mMultipleChoiceAdapter = new MultipleChoiceAdapter(buildData(preferenceManager));
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        if (!TextUtils.isEmpty(mReverseDependencyKey)) {
            Preference preference = findPreferenceInHierarchy(mReverseDependencyKey);
            if ((preference instanceof MySwitchPreference || preference
                    instanceof MyCheckBoxPreference)) {
                ReverseDependencyMonitor reverseDependencyMonitor =
                        (ReverseDependencyMonitor) preference;
                reverseDependencyMonitor.registerReverseDependencyPreference(this);
            }
        }
    }

    private void setUpSelectAll(RelativeLayout parent) {
        final CompoundButton compoundButton = mSelector == 0 ? new CheckBox(getContext()) :
                new Switch(getContext());
        ((FrameLayout) parent.findViewById(R.id.select_all_compound_container))
                .addView(compoundButton, 0);
        compoundButton.setClickable(false);
        compoundButton.setFocusable(false);
        compoundButton.setChecked(false);
        final TextView selectAllText = parent.findViewById(R.id.select_all_text);
        compoundButton.setOnCheckedChangeListener((compoundButton1, isChecked) -> {
            if (mMultipleChoiceAdapter != null) mMultipleChoiceAdapter.selectAll(isChecked);
            selectAllText.setText(isChecked ? getContext().getString(R.string.deselect_all) : getContext().getString(R.string.select_all));
        });
        parent.setVisibility(View.VISIBLE);
        parent.setOnClickListener(view -> compoundButton.setChecked(!compoundButton.isChecked()));

    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog alertDialog = (AlertDialog) getDialog();
        Button cancel = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        cancel.setVisibility(View.GONE);
        Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setText(R.string.multiple_choice_preference_button_done);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        RecyclerView recyclerView = view.findViewById(R.id.multiple_choice_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mMultipleChoiceAdapter);
        if (mIsShowSelectAll) {
            RelativeLayout container = view.findViewById(R.id.select_all_main_container);
            setUpSelectAll(container);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mIsRebootRequired) {
                Utils.showRebootRequiredDialog(getContext());
            } else {
                if (mPackageToKill != null) {
                    if (Utils.isPackageInstalled(mPackageToKill)) {
                        if (mIsSilent) {
                            Utils.killPackage(mPackageToKill);
                        } else {
                            Utils.showKillPackageDialog(mPackageToKill, getContext());
                        }
                    }
                }
            }
        }
    }

    private ArrayList<Item> buildData(PreferenceManager preferenceManager) {
        ArrayList<Item> list = new ArrayList<>();
        if (isValidData()) {
            ContentResolver contentResolver = getContext().getContentResolver();
            SharedPreferences sharedPreferences = preferenceManager.getSharedPreferences();
            int length = mKeys.length;
            for (int i = 0; i < length; i++) {
                int value;
                String key = mKeys[i];
                try {
                    value = Settings.System.getInt(contentResolver, key);
                } catch (Settings.SettingNotFoundException e) {
                    value = mDefaults[i];
                    Settings.System.putInt(contentResolver, key, value);
                }

                boolean valueBoolean = value != 0;
                if (!sharedPreferences.contains(key) || !Boolean.valueOf(sharedPreferences
                        .getBoolean(key, false)).equals(valueBoolean))
                    sharedPreferences.edit().putBoolean(key, valueBoolean).apply();

                Item item = new Item();
                item.mKey = key;
                item.mTitle = mTitles[i];
                item.mIsSelected = valueBoolean;
                item.mSummary = mSummaries[i];
                list.add(item);
            }
        } else {
            throw new IllegalStateException("Data for preference is missing or improperly " +
                    "formatted. Please verify the arrays are all present and are" +
                    " all of equal size.");
        }
        return list;
    }

    private boolean isValidData() {
        if (mKeys == null) return false;
        int length = mKeys.length;
        return mDefaults != null
                && mSummaries != null
                && mTitles != null
                && mDefaults.length == length
                && mSummaries.length == length
                && mTitles.length == length;
    }

    private void updateDatabase(String key, boolean isChecked) {
        getPreferenceManager().getSharedPreferences().edit().putBoolean(key, isChecked).apply();
        Settings.System.putInt(getContext().getContentResolver(), key, isChecked ? 1 : 0);
    }

    private static class Item {
        String mTitle, mKey, mSummary;
        boolean mIsSelected;
    }

    private class MultipleChoiceAdapter extends RecyclerView.Adapter<MultipleChoiceAdapter
            .GearViewHolder> {
        private final ArrayList<Item> mItemArrayList;

        MultipleChoiceAdapter(ArrayList<Item> itemArrayList) {
            mItemArrayList = itemArrayList;
        }

        @NonNull
        @Override
        public GearViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.multiple_choice_item_layout,
                    parent, false);
            CompoundButton compoundButton = mSelector == 0 ? new CheckBox(context) :
                    new Switch(context);
            FrameLayout frameLayout = view.findViewById(R.id.compound_button_container);
            frameLayout.addView(compoundButton, 0);
            int id = View.generateViewId();
            compoundButton.setId(id);
            compoundButton.setClickable(false);
            compoundButton.setFocusable(false);
            return new GearViewHolder(view, id);
        }

        @Override
        public void onBindViewHolder(GearViewHolder holder, int position) {
            holder.bindViews(mItemArrayList.get(position));
        }

        @Override
        public int getItemCount() {
            return mItemArrayList != null ? mItemArrayList.size() : 0;
        }

        void selectAll(boolean isSelected) {
            for (int i = 0; i < getItemCount(); i++) {
                mItemArrayList.get(i).mIsSelected = isSelected;
                updateDatabase(mItemArrayList.get(i).mKey, isSelected);
            }
            notifyDataSetChanged();
        }

        class GearViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTitle;
            private final TextView mSummary;
            private final CompoundButton mCompoundButton;

            GearViewHolder(View itemView, int compoundId) {
                super(itemView);
                mTitle = itemView.findViewById(R.id.multiple_choice_item_title);
                mSummary = itemView.findViewById(R.id.multiple_choice_item_summary);
                mCompoundButton = itemView.findViewById(compoundId);
            }

            void bindViews(final Item item) {
                mTitle.setText(item.mTitle);
                mSummary.setText(item.mSummary);
                mCompoundButton.setOnCheckedChangeListener(null);
                mCompoundButton.setChecked(item.mIsSelected);
                mCompoundButton.setOnCheckedChangeListener((compoundButton, isChecked) ->
                        updateDatabase(item.mKey, isChecked));
                itemView.setOnClickListener(view -> {
                    mCompoundButton.setChecked(!mCompoundButton.isChecked());
                    item.mIsSelected = mCompoundButton.isChecked();
                });
            }
        }
    }
}
