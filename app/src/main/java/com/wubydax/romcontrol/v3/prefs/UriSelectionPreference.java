package com.wubydax.romcontrol.v3.prefs;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.wubydax.romcontrol.v3.R;
import com.wubydax.romcontrol.v3.utils.Utils;


public class UriSelectionPreference extends Preference {

    private OnUriSelectionRequestedListener mOnUriSelectionRequestedListener;
    private final ContentResolver mContentResolver;
    private final String mReverseDependencyKey;


    public UriSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mReverseDependencyKey = typedArray.getString(R.styleable.Preference_reverseDependency);
        typedArray.recycle();
    }

    public OnUriSelectionRequestedListener getOnUriSelectionRequestedListener() {
        return mOnUriSelectionRequestedListener;
    }

    public void setOnUriSelectionRequestedListener(OnUriSelectionRequestedListener
                                                           onUriSelectionRequestedListener) {
        mOnUriSelectionRequestedListener = onUriSelectionRequestedListener;
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
        String uriString = Settings.System.getString(mContentResolver, getKey());
        if (uriString != null) {
            persistString(uriString);
            attemptToSetIcon(uriString);
        }
    }


    public void attemptToSetIcon(String uriString) {
        Uri uri = Uri.parse(uriString);
        if (uri != null) {
            SetImage setImage = new SetImage();
            setImage.execute(uri);
        }
    }

    @Override
    protected void onClick() {
        if (mOnUriSelectionRequestedListener != null) {
            mOnUriSelectionRequestedListener.onUriSelectionRequested(getKey());
        }
    }

    public interface OnUriSelectionRequestedListener {
        void onUriSelectionRequested(String key);
    }

    @SuppressLint("StaticFieldLeak")
    private class SetImage extends AsyncTask<Uri, Void, Drawable> {


        @Override
        protected Drawable doInBackground(Uri... params) {
            return Utils.getIconDrawable(params[0]);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            setIcon(drawable);
        }

    }
}
