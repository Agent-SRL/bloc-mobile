package org.sdf.becquerel.bloc;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import java.util.Collections;

/**
 * Created by stephen on 22/12/15.
 */
public class SettingsActivity extends Activity {
    //Maps page IDs to the relevant preference item
    public static final java.util.Map<Integer, String> policyPrefIDs;
    static {
        java.util.Map<Integer, String> aMap = new java.util.HashMap<Integer, String>();
        aMap.put(R.layout.domestic, "domestic_policies");
        aMap.put(R.layout.military, "military_policies");
        policyPrefIDs = Collections.unmodifiableMap(aMap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}



