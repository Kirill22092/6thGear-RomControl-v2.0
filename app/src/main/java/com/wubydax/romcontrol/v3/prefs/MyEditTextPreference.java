package com.wubydax.romcontrol.v3.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.Utils;

public class MyEditTextPreference extends EditTextPreference
        implements Preference.OnPreferenceChangeListener {
    private final boolean mIsSilent;
    private final String mPackageToKill;
    private final boolean mIsRebootRequired;
    private final ContentResolver mContentResolver;
    private final String mReverseDependencyKey;

    public MyEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mPackageToKill = typedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = typedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mIsRebootRequired = typedArray.getBoolean(R.styleable.Preference_rebootDevice,
                false);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
        mContentResolver = context.getContentResolver();
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
        super.onSetInitialValue(restoreValue, defaultValue);
        String value = "";
        if (!restoreValue && defaultValue != null) {
            String dbValue = Settings.System.getString(mContentResolver, getKey());
            if (dbValue != null && !dbValue.equals(defaultValue)) {
                value = dbValue;
            } else if (dbValue == null) {
                value = (String) defaultValue;
                Utils.putKey(getKey() + " " + defaultValue);
            }
        } else {
            value = getPersistedString(null);
        }
        setSummary(value);
    }

    @Override
    public String getText() {
        String value = Settings.System.getString(mContentResolver, getKey());
        String persistedString = getPersistedString(null);
        if (value != null) {
            if (value.equals(persistedString)) {
                return persistedString;
            } else {
                persistString(value);
                return value;
            }
        } else {
            return persistedString;
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        String value = Settings.System.getString(mContentResolver, getKey());
        String persistedString = getPersistedString(null);
        if (value != null && !value.equals(persistedString)) {
            persistString(value);
            setSummary(value);
        }
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Utils.putKey(getKey() + " " + newValue);
        setSummary((String) newValue);
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
