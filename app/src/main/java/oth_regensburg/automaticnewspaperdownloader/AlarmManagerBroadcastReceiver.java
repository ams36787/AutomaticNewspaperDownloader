package oth_regensburg.automaticnewspaperdownloader;

        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.util.Log;

public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        // Will be executed when alarm goes off
        Log.d(ListviewFragment.LOG_TAG, "BroadcastReciever onRecieve() called");

        AutoStartUpService.getTheLock(context);

        // Init and Start Download
        AutoStartUpService.loadTodaysEdition(context, true);
        //Wake Lock Release in AsyncTask or in the end of loadTodaysEdition();
    }

}