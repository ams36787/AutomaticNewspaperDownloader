package oth_regensburg.automaticnewspaperdownloader;

        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.preference.PreferenceManager;
        import android.util.Log;

        import oth_regensburg.automaticnewspaperdownloader.R;

/**
 * Created by Stefan on 12.02.2016.
 */
public class StartupBroadcastReceiver extends BroadcastReceiver{

    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();

        if (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED))
        {
            // Gets called after device boot
            // Toast.makeText(context, "onRecieve() was called from StartupBroadcastReciever", Toast.LENGTH_LONG).show(); // Its important to use the context which was passed to the onRecieve Method!

            String LOG_TAG = "de.othregensburg.AND";
            Log.d(LOG_TAG, "AutoStartService was sucessfully called after device boot");

            boolean bSetAutoStart;
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            bSetAutoStart = sharedPref.getBoolean(context.getResources().getString(R.string.preference_download_automatic_key), false);

            if (bSetAutoStart) // only autostart if set in options
            {
                Intent i = new Intent(context, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }
    }
}