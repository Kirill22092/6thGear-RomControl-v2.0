package com.wubydax.romcontrol.v3;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.wubydax.romcontrol.v3.prefs.OpenAppPreference;
import com.wubydax.romcontrol.v3.prefs.UriSelectionPreference;
import com.wubydax.romcontrol.v3.utils.Constants;
import com.wubydax.romcontrol.v3.utils.Utils;

import java.util.Objects;


public class PrefsFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener,
        UriSelectionPreference.OnUriSelectionRequestedListener {
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private String mUriPreferenceKey;

    public PrefsFragment() {
        //empty public constructor
    }

    static PrefsFragment newInstance(String prefName) {
        PrefsFragment prefsFragment = new PrefsFragment();
        Bundle args = new Bundle();
        args.putString(Constants.PREF_NAME_KEY, prefName);
        prefsFragment.setArguments(args);
        return prefsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Runnable runnable = () -> {
            mContext = MyApp.getContext();
            String prefName = getArguments().getString(Constants.PREF_NAME_KEY);
            int prefId = mContext.getResources().getIdentifier(prefName, "xml",
                    mContext.getPackageName());
            if (prefId != 0) {
                getPreferenceManager().setSharedPreferencesName(prefName);
                addPreferencesFromResource(prefId);
                mSharedPreferences = getPreferenceManager().getSharedPreferences();
                iteratePrefs(getPreferenceScreen());
            }
        };
        Thread thread = new Thread(runnable);
        thread.setPriority(10);
        thread.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == 46) {
            Utils.putCommand(mUriPreferenceKey + " " + Objects
                    .requireNonNull(data.getData()).toString());
            mSharedPreferences.edit().putString(mUriPreferenceKey, data.getData().toString())
                    .apply();
            ((UriSelectionPreference) findPreference(mUriPreferenceKey))
                    .attemptToSetIcon(data.getData().toString());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void iteratePrefs(PreferenceGroup preferenceGroup) {
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                if (preference instanceof PreferenceScreen) {
                    preference.setOnPreferenceClickListener(this);
                }
                if (((PreferenceGroup) preference).getPreferenceCount() > 0) {
                    iteratePrefs((PreferenceGroup) preference);
                }
            } else if (preference instanceof OpenAppPreference) {
                if (!((OpenAppPreference) preference).isInstalled()) {
                    if (preferenceGroup.removePreference(preference)) {
                        i--;
                    }
                }
            } else if (preference instanceof UriSelectionPreference) {
                ((UriSelectionPreference) preference).setOnUriSelectionRequestedListener(this);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (((PreferenceScreen) preference).getPreferenceCount() > 0) {
            setUpNestedPreferenceLayout((PreferenceScreen) preference);
        } else if (preference.getIntent() != null) {
            if (MyApp.getContext().getPackageManager().resolveActivity(preference
                    .getIntent(), 0) != null) {
                startActivity(preference.getIntent());
            }
        }
        return true;

    }

    private void setUpNestedPreferenceLayout(PreferenceScreen preference) {
        final Dialog dialog = preference.getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            ViewGroup rootView = (ViewGroup) dialog.findViewById(android.R.id.list).getParent();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                rootView = (ViewGroup) rootView.getParent();
            }
            assert window != null;
            if (rootView != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
                Toolbar toolbar = (Toolbar) LayoutInflater.from(getActivity())
                        .inflate(R.layout.nested_preference_toolbar_layout, rootView,
                                false);
                toolbar.setTitle(preference.getTitle());
                rootView.addView(toolbar, 0);
                toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
            }
        }
    }

    @Override
    public void onUriSelectionRequested(String key) {
        mUriPreferenceKey = key;
        Intent getContentIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media
                .EXTERNAL_CONTENT_URI);
        startActivityForResult(getContentIntent, 46);
    }
}