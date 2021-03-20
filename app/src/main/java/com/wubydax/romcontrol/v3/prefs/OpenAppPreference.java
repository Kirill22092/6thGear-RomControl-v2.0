package com.wubydax.romcontrol.v3.prefs;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.wubydax.romcontrol.v3.MyApp;
import com.wubydax.romcontrol.v3.R;

import java.util.Objects;

public class OpenAppPreference extends Preference {
    private final String mReverseDependencyKey;
    private ResolveInfo mResolveInfo;
    private final Context mContext;
    private Intent mIntent;
    private final Drawable mIcon;
    private final String mTitle;

    public OpenAppPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = MyApp.getContext();
        String androidNS = "http://schemas.android.com/apk/res/android";
        int iconId = attrs.getAttributeResourceValue(androidNS, "icon", -1);
        mIcon = iconId != -1 ? ContextCompat.getDrawable(context, iconId) : null;
        mTitle = attrs.getAttributeValue(androidNS, "title");
        TypedArray typedArray = context
                .obtainStyledAttributes(attrs, R.styleable.OpenAppPreference);
        String componentName = typedArray
                .getString(R.styleable.OpenAppPreference_componentName);
        TypedArray generalTypedArray = context
                .obtainStyledAttributes(attrs, R.styleable.Preference);
        mReverseDependencyKey = generalTypedArray
                .getString(R.styleable.Preference_reverseDependency);
        generalTypedArray.recycle();
        initPreference(Objects.requireNonNull(componentName));
        typedArray.recycle();
    }

    private void initPreference(String componentName) {
        String[] components = componentName.split("/");
        mIntent = new Intent();
        ComponentName component = new ComponentName(components[0], components[1]);
        mIntent.setComponent(component);
        PackageManager packageManager = mContext.getPackageManager();
        mResolveInfo = packageManager.resolveActivity(mIntent, 0);
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
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        if (isInstalled()) {
            if (mIcon == null) {
                Drawable icon = mResolveInfo.activityInfo.loadIcon(mContext.getPackageManager());
                setIcon(icon);
            }
            if (mTitle == null) {
                setTitle(mResolveInfo.activityInfo.loadLabel(mContext.getPackageManager()));
            }
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        if (isInstalled()) {
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(mIntent);
        }
    }

    public boolean isInstalled() {
        return mResolveInfo != null;
    }
}
