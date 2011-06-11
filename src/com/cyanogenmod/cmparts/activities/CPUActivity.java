/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

//
// CPU Related Settings
//
public class CPUActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    public static final String GOV_PREF = "pref_cpu_gov";
    public static final String GOVERNORS_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
    public static final String GOVERNOR = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    public static final String MIN_FREQ_PREF = "pref_freq_min";
    public static final String MAX_FREQ_PREF = "pref_freq_max";
    public static final String FREQ_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies";
    public static final String FREQ_MAX_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    public static final String FREQ_MIN_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
    public static final String SOB_PREF = "pref_set_on_boot";
    public static final String SCHED_LATENCY_PREF = "pref_sched_latency_ns";
    public static final String SCHED_MINGRAN_PREF = "pref_sched_min_granularity";
    public static final String SCHED_LATENCY_FILE = "/proc/sys/kernel/sched_latency_ns";
    public static final String SCHED_MINGRAN_FILE = "/proc/sys/kernel/sched_min_granularity_ns";
    public static final int SCHED_LATENCY_MINGRAN_RATIO = 10;
    public static final int SCHED_MINGRAN_MAX = 6000000;
    public static final int[] SCHED_LATENCY_OPTIONS = {1000000,2000000,4000000,6000000,10000000,15000000,20000000,30000000,40000000,60000000};

    private static final String TAG = "CPUSettings";

    private String mGovernorFormat;
    private String mMinFrequencyFormat;
    private String mMaxFrequencyFormat;
    private String mSchedLatencyFormat;

    private ListPreference mGovernorPref;
    private ListPreference mMinFrequencyPref;
    private ListPreference mMaxFrequencyPref;
    private ListPreference mSchedLatencyPref;

    public SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mGovernorFormat = getString(R.string.cpu_governors_summary);
        mMinFrequencyFormat = getString(R.string.cpu_min_freq_summary);
        mMaxFrequencyFormat = getString(R.string.cpu_max_freq_summary);
        mSchedLatencyFormat = getString(R.string.cpu_sched_latency_summary);

        String[] availableGovernors = readOneLine(GOVERNORS_LIST_FILE).split(" ");
        String[] availableFrequencies = new String[0];
        String availableFrequenciesLine = readOneLine(FREQ_LIST_FILE);
        if (availableFrequenciesLine != null)
             availableFrequencies = availableFrequenciesLine.split(" ");
        String[] frequencies;
        String temp;

        String[] latencyOptions = new String[SCHED_LATENCY_OPTIONS.length];
        String[] latencies = new String[SCHED_LATENCY_OPTIONS.length];
        for (int i = 0; i < latencies.length; i++) {
            latencyOptions[i] = Integer.toString(SCHED_LATENCY_OPTIONS[i]);
            latencies[i] = toMs(SCHED_LATENCY_OPTIONS[i]);
        }

        frequencies = new String[availableFrequencies.length];
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = toMHz(availableFrequencies[i]);
        }

        setTitle(R.string.cpu_title);
        addPreferencesFromResource(R.xml.cpu_settings);

        PreferenceScreen PrefScreen = getPreferenceScreen();

        temp = readOneLine(GOVERNOR);

        mGovernorPref = (ListPreference) PrefScreen.findPreference(GOV_PREF);
        mGovernorPref.setEntryValues(availableGovernors);
        mGovernorPref.setEntries(availableGovernors);
        mGovernorPref.setValue(temp);
        mGovernorPref.setSummary(String.format(mGovernorFormat, temp));
        mGovernorPref.setOnPreferenceChangeListener(this);

        temp = readOneLine(FREQ_MIN_FILE);

        mMinFrequencyPref = (ListPreference) PrefScreen.findPreference(MIN_FREQ_PREF);
        mMinFrequencyPref.setEntryValues(availableFrequencies);
        mMinFrequencyPref.setEntries(frequencies);
        mMinFrequencyPref.setValue(temp);
        mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz(temp)));
        mMinFrequencyPref.setOnPreferenceChangeListener(this);

        temp = readOneLine(FREQ_MAX_FILE);

        mMaxFrequencyPref = (ListPreference) PrefScreen.findPreference(MAX_FREQ_PREF);
        mMaxFrequencyPref.setEntryValues(availableFrequencies);
        mMaxFrequencyPref.setEntries(frequencies);
        mMaxFrequencyPref.setValue(temp);
        mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz(temp)));
        mMaxFrequencyPref.setOnPreferenceChangeListener(this);

        temp = readOneLine(SCHED_LATENCY_FILE);

        mSchedLatencyPref = (ListPreference) PrefScreen.findPreference(SCHED_LATENCY_PREF);
        if (latencyAvailable()) {
            mSchedLatencyPref.setEntryValues(latencyOptions);
            mSchedLatencyPref.setEntries(latencies);
            mSchedLatencyPref.setValue(temp);
            mSchedLatencyPref.setSummary(String.format(mSchedLatencyFormat, toMs(Integer.valueOf(temp))));
            mSchedLatencyPref.setOnPreferenceChangeListener(this);
        } else {
            PrefScreen.removePreference(mSchedLatencyPref);
            mSchedLatencyPref = null;
        }
    }

    @Override
    public void onResume() {
        String temp;

        super.onResume();

        temp = readOneLine(FREQ_MAX_FILE);
        mMaxFrequencyPref.setValue(temp);
        mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz(temp)));

        temp = readOneLine(FREQ_MIN_FILE);
        mMinFrequencyPref.setValue(temp);
        mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz(temp)));

        temp = readOneLine(GOVERNOR);
        mGovernorPref.setValue(temp);
        mGovernorPref.setSummary(String.format(mGovernorFormat, temp));

        if (mSchedLatencyPref != null) {
            temp = readOneLine(SCHED_LATENCY_FILE);
            mSchedLatencyPref.setValue(temp);
            mSchedLatencyPref.setSummary(String.format(mSchedLatencyFormat, toMs(Integer.valueOf(temp))));
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String fname = "";

        if (newValue != null) {
            if (preference == mGovernorPref) {
                fname = GOVERNOR;
            } else if (preference == mMinFrequencyPref) {
                fname = FREQ_MIN_FILE;
            } else if (preference == mMaxFrequencyPref) {
                fname = FREQ_MAX_FILE;
            } else if (preference == mSchedLatencyPref) {
                return changeLatency((String) newValue);
            }

            if (writeOneLine(fname, (String) newValue)) {
                if (preference == mGovernorPref) {
                    mGovernorPref.setSummary(String.format(mGovernorFormat, (String) newValue));
                } else if (preference == mMinFrequencyPref) {
                    mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat,
                            toMHz((String) newValue)));
                } else if (preference == mMaxFrequencyPref) {
                    mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat,
                            toMHz((String) newValue)));
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static String readOneLine(String fname) {
        BufferedReader br;
        String line = null;

        try {
            br = new BufferedReader(new FileReader(fname), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "IO Exception when reading /sys/ file", e);
        }
        return line;
    }

    public static boolean writeOneLine(String fname, String value) {
        try {
            FileWriter fw = new FileWriter(fname);
            try {
                fw.write(value);
            } finally {
                fw.close();
            }
        } catch (IOException e) {
            String Error = "Error writing to " + fname + ". Exception: ";
            Log.e(TAG, Error, e);
            return false;
        }
        return true;
    }

    private String toMHz(String mhzString) {
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000).append(" MHz").toString();
    }

    private String toMs(int value) {
        return new StringBuilder().append(Math.round(value / 1000000.0)).append(" ms").toString();
    }

    private boolean latencyAvailable() {
        return new File(SCHED_LATENCY_FILE).canWrite();
    }

    private boolean changeLatency(String newValue) {
        int latencyNs = Integer.valueOf(newValue);
        int minGranNs = Math.min(SCHED_MINGRAN_MAX, latencyNs / SCHED_LATENCY_MINGRAN_RATIO);
        String minGran = Integer.toString(minGranNs);

        if (writeOneLine(SCHED_LATENCY_FILE, newValue)) {
            writeOneLine(SCHED_MINGRAN_FILE, minGran);
            Editor mEdit = mPrefs.edit();
            mEdit.putString(SCHED_MINGRAN_PREF, minGran);
            mEdit.commit();
            mSchedLatencyPref.setSummary(String.format(mSchedLatencyFormat, toMs(latencyNs)));
            return true;
        }
        else return false;
    }

}
