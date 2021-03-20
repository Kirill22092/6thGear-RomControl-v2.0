package com.wubydax.romcontrol.v3.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.Utils;

import java.util.Arrays;
import java.util.List;


public class MyListPreference extends ListPreference
        implements Preference.OnPreferenceChangeListener {
    private final String mPackageToKill, mDependentValue;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private final String mReverseDependencyKey;
    private final ContentResolver mContentResolver;
    private final List<CharSequence> mEntries;
    private final List<CharSequence> mValues;


    public MyListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        mEntries = Arrays.asList(getEntries());
        mValues = Arrays.asList(getEntryValues());
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mPackageToKill = typedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = typedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mIsRebootRequired = typedArray.getBoolean(R.styleable.Preference_rebootDevice,
                false);
        mDependentValue = typedArray.getString(R.styleable.Preference_dependentValue);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
        setOnPreferenceChangeListener(this);
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
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String dbValue = Settings.System.getString(mContentResolver, getKey());
        String value = "";
        if (!restoreValue) {
            if (dbValue != null) {
                value = dbValue;
                persistString(value);
            } else {
                if (defaultValue != null) {
                    value = (String) defaultValue;
                    Utils.putKey(getKey() + " " + defaultValue);
                }
            }
        } else {
            value = getPersistedString(null);
            if (dbValue != null && !dbValue.equals(value)) {
                persistString(dbValue);
                value = dbValue;
            }
        }

        int index = mValues.indexOf(value);
        if (index != -1) {
            setSummary(mEntries.get(index));
            setValue(value);
        }
    }

    @Override
    public void setValue(String value) {
        String oldValue = getValue();
        super.setValue(value);
        if (!value.equals(oldValue)) {
            notifyDependencyChange(shouldDisableDependents());
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        return super.shouldDisableDependents() || getValue() == null ||
                getValue().equals(mDependentValue);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Utils.putKey(getKey() + " " + newValue);
        System.out.println(getKey() + " " + newValue);
        int index = mValues.indexOf(newValue);
        if (index != -1) {
            setSummary(mEntries.get(index));
        }
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
        return true;
    }
}
