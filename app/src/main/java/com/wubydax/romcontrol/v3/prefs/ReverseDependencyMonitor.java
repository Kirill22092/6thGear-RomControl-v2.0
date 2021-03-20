package com.wubydax.romcontrol.v3.prefs;

import android.preference.Preference;


interface ReverseDependencyMonitor {
    void registerReverseDependencyPreference(Preference preference);
}
