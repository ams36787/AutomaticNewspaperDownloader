package oth_regensburg.automaticnewspaperdownloader;

        import android.os.Bundle;
        import android.preference.Preference;
        import android.preference.PreferenceFragment;

        import oth_regensburg.automaticnewspaperdownloader.R;

public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display Preferences
        addPreferencesFromResource(R.xml.preferences);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {

        return true;
    }

public void setSummary(String key, String sNewSummary)
{
    findPreference(key);
    return;
}

}