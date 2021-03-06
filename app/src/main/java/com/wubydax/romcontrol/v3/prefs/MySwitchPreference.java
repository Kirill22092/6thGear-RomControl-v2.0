package com.wubydax.romcontrol.v3.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.Utils;

import java.util.ArrayList;


public class MySwitchPreference extends SwitchPreference implements ReverseDependencyMonitor,
        SwitchMaterial.OnCheckedChangeListener {
    private final ContentResolver mContentResolver;
    private final String mPackageToKill;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private ArrayList<Preference> mReverseDependents;
    private final String mReverseDependencyKey;
    private int dbInt;

    public MySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mPackageToKill = typedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = typedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mIsRebootRequired = typedArray.getBoolean(R.styleable.Preference_rebootDevice,
                false);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
        mContentResolver = context.getContentResolver();
        setWidgetLayoutResource(R.layout.switch_preference_layout);
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
        dbInt = 0;
        try {
            dbInt = Settings.System.getInt(mContentResolver, getKey());
        } catch (Settings.SettingNotFoundException e) {
            if (defaultValue != null) {
                dbInt = (boolean) defaultValue ? 1 : 0;
                Utils.putKey(getKey() + " " + dbInt);
            }
        }
        persistBoolean(dbInt != 0);
        setChecked(dbInt != 0);
    }

    @Override
    protected void onBindView(View view) {
        SwitchMaterial switchmaterial = view.findViewById(R.id.switch_material);
        switchmaterial.setChecked(dbInt != 0);
        switchmaterial.setOnCheckedChangeListener(this);
        super.onBindView(view);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        onPreferenceChange(isChecked);
        setChecked(isChecked);
    }

    public void onPreferenceChange(boolean isTrue) {
        dbInt = isTrue ? 1 : 0;
        Utils.putKey(getKey() + " " + dbInt);
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
        if (mReverseDependents != null && mReverseDependents.size() > 0) {
            for (Preference pref : mReverseDependents) {
                pref.setEnabled(!isTrue);
            }
        }
    }


    @Override
    public void registerReverseDependencyPreference(Preference preference) {
        if (mReverseDependents == null) {
            mReverseDependents = new ArrayList<>();
        }
        if (preference != null && !mReverseDependents.contains(preference)) {
            mReverseDependents.add(preference);
            preference.setEnabled(!isChecked());
        }
    }
}
