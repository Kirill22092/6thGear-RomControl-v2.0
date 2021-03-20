package com.wubydax.romcontrol.v3.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.provider.Settings;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;

import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.MultiSelectAdapter;
import com.wubydax.romcontrol.v3.utils.SelectionItem;
import com.wubydax.romcontrol.v3.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MyMultiSelectPreference extends DialogPreference
        implements MultiSelectAdapter.OnItemSelectedListener {
    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private final String mReverseDependencyKey;
    private final ContentResolver mContentResolver;
    private final String[] mEntries;
    private final String[] mValues;
    private MultiSelectAdapter mMultiSelectAdapter;
    private String mValue;
    private int mCount;
    private RadioButton mRadioButton;

    public MyMultiSelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.MyMultiSelectPreference);
        mEntries = context.getResources().getStringArray(typedArray
                .getResourceId(R.styleable.MyMultiSelectPreference_multiEntryList, -1));
        mValues = context.getResources().getStringArray(typedArray
                .getResourceId(R.styleable.MyMultiSelectPreference_multiValuesList, -1));
        typedArray.recycle();
        TypedArray generalTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.Preference);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mReverseDependencyKey = generalTypedArray
                .getString(R.styleable.Preference_reverseDependency);
        mIsRebootRequired = generalTypedArray
                .getBoolean(R.styleable.Preference_rebootDevice, false);
        generalTypedArray.recycle();
        setDialogLayoutResource(R.layout.multi_select_preference_dialog_layout);
        if (mEntries.length != mValues.length || mEntries.length == 0)
            throw new IllegalArgumentException("Data for preference is missing or improperly" +
                    " formatted. Please verify the arrays are all present and" +
                    " are all of equal size");
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String value = Settings.System.getString(mContentResolver, getKey());
        if (value == null) {
            value = defaultValue != null ? (String) defaultValue : "";
            Utils.putKey(getKey() + " " + value);
        }
        persistString(value);
        mValue = value;
        setSummary(getSummaryString());
    }

    private String getSummaryString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(mValue)) {
            String[] values = mValue.split(",");
            if (values.length > 0) {
                for (String value : values) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(mEntries[getEntryForValue(value)]);
                }
            }
        }
        return stringBuilder.toString();
    }

    private int getEntryForValue(String value) {
        List<String> listValues = Arrays.asList(mValues);
        return listValues.indexOf(value);
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

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mMultiSelectAdapter = new MultiSelectAdapter(getData(), this);
        mCount = 0;
        RecyclerView recyclerView = view.findViewById(R.id.multi_select_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mMultiSelectAdapter);
        mRadioButton = view.findViewById(R.id.select_all_radio_button);
        mMultiSelectAdapter.setSelectedItems(mValue);
        mRadioButton.setChecked(mCount == mEntries.length);
        view.findViewById(R.id.select_all_main_container).setOnClickListener(view1 -> {
            mRadioButton.setChecked(!mRadioButton.isChecked());
            mMultiSelectAdapter.selectAll(mRadioButton.isChecked());
            mCount = mRadioButton.isChecked() ? mEntries.length : 0;
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mValue = mMultiSelectAdapter.getSelectedItems();
            persistString(mValue);
            Utils.putKey(getKey() + " " + mValue);
            setSummary(getSummaryString());
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
        super.onClick(dialog, which);
    }

    private ArrayList<SelectionItem> getData() {
        ArrayList<SelectionItem> list = new ArrayList<>();
        for (int i = 0; i < mEntries.length; i++) {
            SelectionItem selectionItem = new SelectionItem();
            selectionItem.entry = mEntries[i];
            selectionItem.value = mValues[i];
            list.add(selectionItem);
        }
        return list;
    }

    @Override
    public void onItemSelected(boolean isSelected) {
        if (isSelected) {
            mCount++;
            if (mCount == mEntries.length) mRadioButton.setChecked(true);
        } else {
            if (mCount == mEntries.length) mRadioButton.setChecked(false);
            mCount--;
        }
    }
}
