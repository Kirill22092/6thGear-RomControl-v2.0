package com.wubydax.romcontrol.v3.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.Utils;


public class GlobalRestorePreference extends DialogPreference {
    private final boolean mIsRebootRequired, mIsSilent;
    private final String mPackageToKill;
    private final String[] mTitles, mKeys;
    private final int[] mValues;
    private final String mReverseDependencyKey;

    public GlobalRestorePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.GlobalRestorePreference);
        mTitles = context.getResources().getStringArray(typedArray
                .getResourceId(R.styleable.GlobalRestorePreference_titlesList, -1));
        mKeys = context.getResources().getStringArray(typedArray
                .getResourceId(R.styleable.GlobalRestorePreference_keysList, -1));
        mValues = context.getResources().getIntArray(typedArray
                .getResourceId(R.styleable.GlobalRestorePreference_valuesList, -1));
        typedArray.recycle();
        TypedArray generalTypedArray = context
                .obtainStyledAttributes(attrs, R.styleable.Preference);
        mIsRebootRequired = generalTypedArray
                .getBoolean(R.styleable.Preference_rebootDevice, false);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mReverseDependencyKey = generalTypedArray
                .getString(R.styleable.Preference_reverseDependency);
        generalTypedArray.recycle();
        setDialogLayoutResource(R.layout.global_restore_preference_dialog_layout);
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
        StringBuilder stringBuilder = new StringBuilder();
        int length = mTitles.length;
        for (int i = 0; i < length; i++) {
            stringBuilder.append(mTitles[i]);
            if (i != length - 1) {
                stringBuilder.append("\n");
            }
        }
        ((TextView) view.findViewById(R.id.restoreItems)).setText(stringBuilder.toString());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == AlertDialog.BUTTON_POSITIVE) {
            int length = mKeys.length;
            for (int i = 0; i < length; i++) {
                Utils.putKey(mKeys[i] + " " + mValues[i]);
                Preference preference = findPreferenceInHierarchy(mKeys[i]);
                if (preference instanceof ColorPickerPreference) {
                    ((ColorPickerPreference) preference).setColor(mValues[i]);
                }
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
        }
    }
}
