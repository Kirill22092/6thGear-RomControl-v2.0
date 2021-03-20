package com.wubydax.romcontrol.v3.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;
import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.Utils;

import java.util.Locale;

public class MySeekBarPreference extends Preference implements
        Slider.OnSliderTouchListener, Slider.OnChangeListener {
    private static final String LOG_TAG = MySeekBarPreference.class.getSimpleName();
    private final String mPackageToKill;
    private final String mOtherValueString;
    private final String mOtherValueStringText;
    private final boolean mIsSilent;
    private final boolean mIsRebootRequired;
    private final boolean mIsDiscrete;
    private final int mMinValue;
    private final int mMaxValue;
    private final int mDefaultValue;
    private String mUnitValue;
    private final String mFormat = "%d%s";
    private TextView mValueText;
    private final ContentResolver mContentResolver;
    private final String mReverseDependencyKey;


    public MySeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.MySeekBarPreference);
        mMinValue = typedArray.getInt(R.styleable.MySeekBarPreference_minValue, 0);
        mMaxValue = typedArray.getInt(R.styleable.MySeekBarPreference_maxValue, 100);
        mOtherValueString = typedArray.getString(R.styleable.MySeekBarPreference_otherValueString);
        mOtherValueStringText = typedArray
                .getString(R.styleable.MySeekBarPreference_otherValueStringText);
        mUnitValue = typedArray.getString(R.styleable.MySeekBarPreference_unitsValue);
        if (mUnitValue == null) mUnitValue = "";
        mIsDiscrete = typedArray.getBoolean(R.styleable.MySeekBarPreference_isDiscrete,
                false);
        TypedArray generalTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.Preference);
        mIsRebootRequired = generalTypedArray.getBoolean(R.styleable.Preference_rebootDevice,
                false);
        mPackageToKill = generalTypedArray.getString(R.styleable.Preference_packageNameToKill);
        mIsSilent = generalTypedArray.getBoolean(R.styleable.Preference_isSilent, true);
        mReverseDependencyKey = generalTypedArray
                .getString(R.styleable.Preference_reverseDependency);
        mDefaultValue = mMaxValue / 2;
        setWidgetLayoutResource(R.layout.seekbar_preference_layout);
        typedArray.recycle();
        generalTypedArray.recycle();
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
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, mDefaultValue);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        int value;
        try {
            value = Settings.System.getInt(mContentResolver, getKey());
        } catch (Settings.SettingNotFoundException e) {
            value = !restorePersistedValue && defaultValue != null ? (int) defaultValue :
                    getPersistedInt(mDefaultValue);
            Utils.putKey(getKey() + " " + value);
        }
        persistInt(value);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LinearLayout view = (LinearLayout) super.onCreateView(parent);
        view.setOrientation(LinearLayout.VERTICAL);
        View widgetView = view.findViewById(android.R.id.widget_frame);
        widgetView.setPadding(0, 0, 0, 0);
        return view;
    }

    @Override
    protected void onBindView(View view) {
        Slider seekBar = view.findViewById(R.id.seekBarPrefSlider);
        seekBar.setValueTo(mMaxValue);
        seekBar.setValueFrom(mMinValue);
        seekBar.setTickVisible(mIsDiscrete);
        seekBar.setStepSize(1);
        TextView maxText = view.findViewById(R.id.maxValueText);
        TextView minText = view.findViewById(R.id.minValueText);
        mValueText = view.findViewById(R.id.valueText);
        maxText.setText(String.format(Locale.getDefault(), mFormat, mMaxValue, mUnitValue));
        minText.setText(String.format(Locale.getDefault(), mFormat, mMinValue, mUnitValue));
        mValueText.setText(String.format(Locale.getDefault(), mFormat,
                getPersistedInt(mDefaultValue), mUnitValue));
        if (mOtherValueString != null && mOtherValueStringText != null) {
            ifOtherProgress();
            if (minText.getText().toString().equals(mOtherValueString)) {
                minText.setText(mOtherValueStringText);
            }
            if (maxText.getText().toString().equals(mOtherValueString)) {
                maxText.setText(mOtherValueStringText);
            }
        }
        seekBar.addOnSliderTouchListener(this);
        seekBar.addOnChangeListener(this);
        seekBar.setValue(getPersistedInt(mDefaultValue));
        super.onBindView(view);
    }

    @Override
    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
        int updatedProgress = Math.round(value);
        mValueText.setText(String.format(Locale.getDefault(), mFormat, updatedProgress,
                mUnitValue));
        if (mOtherValueString != null && mOtherValueStringText != null) {
            ifOtherProgress();
        }
    }

    @Override
    public void onStartTrackingTouch(@NonNull Slider slider) {
    }

    @Override
    public void onStopTrackingTouch(@NonNull Slider slider) {
        persistInt(Math.round(slider.getValue()));
        onPreferenceChange(Math.round(slider.getValue()));
    }

    public void ifOtherProgress() {
        if (mValueText.getText().toString().equals(mOtherValueString)) {
            mValueText.setText(mOtherValueStringText);
        }
    }

    private void onPreferenceChange(int newValue) {
        Utils.putKey(getKey() + " " + newValue);
        Log.d(LOG_TAG, "onPreferenceChange is called and reboot required is "
                + mIsRebootRequired);
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
