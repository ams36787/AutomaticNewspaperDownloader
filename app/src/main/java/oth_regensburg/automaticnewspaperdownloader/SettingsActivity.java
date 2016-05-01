package oth_regensburg.automaticnewspaperdownloader;

        import android.app.Activity;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.preference.Preference;
        import android.preference.PreferenceManager;
        import android.util.Log;
        import android.widget.Toast;

public class SettingsActivity extends Activity {



    SharedPreferences prefs;
    //public static Context mContext;
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    public static final String KEY_PREF_NEWSPAPER_DOWNLOAD = "pref_newpaperDownloadType";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); //get the preferences that are allowed to be given

        listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
                    // This function gets called whenever a Setting changes
                    {
                        Boolean bUpdateAlarm=false;
                        Boolean bMoveFilesToSdcard=false;

                        if(key==getResources().getString(R.string.preference_login_username_key))
                            {
                                LogString(prefs, key);
                            }
                        else if (key==getResources().getString(R.string.preference_login_password_key))
                        {
                            LogString(prefs, key);
                        }
                        else if (key==getResources().getString(R.string.preference_debugMode_key))
                        {
                            LogBoolean(prefs, key);
                        }
                        else if (key==getResources().getString(R.string.preference_download_automatic_key))
                        {
                            LogBoolean(prefs, key);
                            bUpdateAlarm=true; // Update Alarm (on or off)
                        }
                        else if (key==getResources().getString(R.string.preference_download_time_key))
                        {
                            LogString(prefs, key);
                            bUpdateAlarm=true; // Update Alarm with new download time
                        }
                        else if (key==getResources().getString(R.string.preference_DownloadInWifiOnly_key))
                        {
                            LogBoolean(prefs, key);
                        }
                        else if (key==getResources().getString(R.string.preference_MoveFilesToSdcard_key))
                        {
                            LogBoolean(prefs, key);
                            // refresh view
                        }
                        else if (key==getResources().getString(R.string.preference_filestokeep_key))
                        {
                            LogString(prefs, key);
                            //bMoveFilesToSdcard=true; // Move Files to Sd card immediately
                            // update: do not move files immidiatly. only after manual refrehs via swipe
                        }
                        else if (key==getResources().getString(R.string.preference_enable_autostart_key))
                        {
                            LogBoolean(prefs, key);
                        }

                        // update Settings
                        AutoStartUpService.updateSettings(AutoStartUpService.mContext);

                        if(bUpdateAlarm) // only setAlarm execute if necessary and show
                            {
                                AutoStartUpService.setAlarm(AutoStartUpService.mContext, true);
                            }

                        if(bMoveFilesToSdcard) //only execute if necessary
                            {
                                MainActivity.moveFilesToSdCard(AutoStartUpService.mContext);
                            }
                        else
                            {
                                // refresh view with internal (new) folder
                                ListviewFragment.arrayListeFiles = MainActivity.scanSdCardFolder();
                                AutoStartUpService.removeOldFiles(ListviewFragment.arrayListeFiles);
                            }


                        // todo update summarys
                        // Set summary to be the user-description for the selected value

                        //String sNewSummary = "";
                        //SettingsFragment.setSummary(key, sNewSummary);
                        //findPreference(key).setSummary(prefs.getString(, ""));

                        // Issue: findPreference can only be called from the PreferenceFragment
                        // findPreference(key); can only be called from non static method
                        // but OnChangeListener seems to be an static method!
                        //                         Preference pref = findPreference(key);


                    }
                };
        prefs.registerOnSharedPreferenceChangeListener(listener); //set the listener to listen for changes in the preferences



        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    private void LogString(SharedPreferences prefs, String key) {
        String sTemp = prefs.getString(key, null);
        Log.d(ListviewFragment.LOG_TAG, "onSharedPreferenceChanged called; Key is " + key + " New Value is: " + sTemp);

        if(AutoStartUpService.bSetDebugMode)  // if debug mode is on show toast with updated setting
            {
                MainActivity.MakeToast("Changed Setting: " + key + " New Value is: " + sTemp);
            }
    }


    private void LogBoolean(SharedPreferences prefs, String key) {
        Boolean bTemp = prefs.getBoolean(key, false);
        Log.d(ListviewFragment.LOG_TAG, "onSharedPreferenceChanged called; Key is " + key + " New Value is: " + bTemp);

        if(AutoStartUpService.bSetDebugMode)  // if debug mode is on show toast with updated setting
            {
                MainActivity.MakeToast("Changed Setting: " + key + " New Value is: " + bTemp);
            }
    }




}