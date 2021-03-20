package com.wubydax.romcontrol.v3.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.wubydax.romcontrol.v3.R;

public class MyPreferenceCategory extends PreferenceCategory {

    private final String mReverseDependencyKey;

    public MyPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
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
}
