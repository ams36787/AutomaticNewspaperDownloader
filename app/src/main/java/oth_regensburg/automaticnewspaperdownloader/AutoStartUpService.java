package oth_regensburg.automaticnewspaperdownloader;

        import android.app.AlarmManager;
        import android.app.Notification;
        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.app.Service;
        import android.content.ContentValues;
        import android.content.Context;
        import android.content.SharedPreferences;
        import android.net.ConnectivityManager;
        import android.net.NetworkInfo;
        import android.net.Uri;
        import android.os.AsyncTask;
        import android.os.Handler;
        import android.os.IBinder;
        import android.os.PowerManager;
        import android.preference.PreferenceManager;
        import android.util.Log;
        import android.widget.Toast;
        import android.content.Intent;

        import oth_regensburg.automaticnewspaperdownloader.R;

        import org.jsoup.Connection;
        import org.jsoup.Jsoup;
        import org.jsoup.nodes.Document;
        import org.jsoup.select.Elements;

        import java.io.IOException;
        import java.net.URL;
        import java.text.SimpleDateFormat;
        import java.util.Calendar;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;

public class AutoStartUpService extends Service {

    public static PowerManager mPowerManager; //abc
    public static PowerManager.WakeLock mWakeLock = null;
    public static final int mWakeLockState = PowerManager.PARTIAL_WAKE_LOCK;
    public static Context mContext = null;
    public static Context applicationContext = null;

    public static String LOG_TAG = "de.othregensburg.AND";

    private static String sSetLoginUsername;
    private static String sSetLoginPassword;
    public static boolean bSetDebugMode;
    private static boolean bSetAutoDownload;
    private static boolean bSetAutoStart; // Not used yet here; gets directly fetched from the settings in the BroadcastReciever
    private static boolean bSetDownloadWifiOnly;
    public static boolean bSetMoveFilesToSdcard;
    private static String sSetDownloadTime;
   // public static String sFilesToKeep;
    public static int iFilesToKeep;
    public static String sFilePath = "/storage/extSdCard/Android/data/oth_regensburg.automaticnewspaperdownloader/files/";
    public static String sFilePathExtSdcardFolder = "/storage/extSdCard/Android/data/oth_regensburg.automaticnewspaperdownloader/files/";
    public static String sFilePathIntMemoryFolder;


    private static PendingIntent pendingIntent;
    private static AlarmManager alarmManager;
    private static Intent intentAlarm;

    private NotificationManager mManager;

    private static String sNotificationTitle = "Default-Title";
    private static String sNotificationText = "Default-Text";

    private static int mNotificationId = 001; // Sets an ID for the notification

    public static long t_debug_time = 60 * 60 * 1000; //time interval for debugging in milliseconds


    @Override
    public IBinder onBind(Intent intent) {
        // The system calls this method when another component wants to bind
        // with the service (such as to perform RPC), by calling bindService().
        // In your implementation of this method, you must provide an interface
        // that clients use to communicate with the service, by returning an
        // IBinder. You must always implement this method, but if you don't want
        // to allow binding, then you should return null.
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(ListviewFragment.LOG_TAG, "onCreate() called from AutoStartUpService");
        // The system calls this method when the service is first created,
        // to perform one-time setup procedures (before it calls either
        // onStartCommand() or onBind()). If the service is already running,
        // this method is not called.

        super.onCreate();

        InitService(this);  // initialisation of serveral variables
        setAlarm(this, false);     // set alarm once started up; do not show the toast
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(ListviewFragment.LOG_TAG, "onStartCommand() called from AutoStartUpService");
        // The system calls this method when another component,
        // such as an activity, requests that the service be started,
        // by calling startService(). Once this method executes, the
        // service is started and can run in the background indefinitely.
        // If you implement this, it is your responsibility to stop the service
        // when its work is done, by calling stopSelf() or stopService().
        // (If you only want to provide binding, you don't need to implement this method.)

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(ListviewFragment.LOG_TAG, "onDestroy() called  from AutoStartUpService");
        // The system calls this method when the service is no longer used
        // and is being destroyed. Your service should implement this to clean
        // up any resources such as threads, registered listeners, receivers,
        // etc. This is the last call the service receives.

        releaseTheLock();
        super.onDestroy();
    }

    static void setContext(Context context) {
        mContext = context;
        applicationContext = mContext.getApplicationContext();
    }

    static void getTheLock(Context context) {
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(mWakeLockState, "NEWSPAPER_DOWNLOADER_WAKE_LOCK_TAG");
        if (mWakeLock != null) {
            mWakeLock.acquire();
            Log.d(LOG_TAG, "Wake Lock aquired");
        }
    }


    static void releaseTheLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
            Log.d(LOG_TAG, "Wake Lock released");
        }
    }

    public static void InitService(Context context) {
        Log.d(LOG_TAG, "InitService(); called");
        InitAlarmManager(context);
        updateSettings(context);
    }

    public static void InitAlarmManager(Context context) {
        intentAlarm = new Intent(context, AlarmManagerBroadcastReceiver.class); // requires packageContext
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intentAlarm, PendingIntent.FLAG_CANCEL_CURRENT);
        mContext = context;
        Log.d(LOG_TAG, "AlarmManager Initialised");
    }

    public static void updateSettings(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sSetLoginUsername = sharedPref.getString(context.getResources().getString(R.string.preference_login_username_key), null);
        sSetLoginPassword = sharedPref.getString(context.getResources().getString(R.string.preference_login_password_key), null);
        bSetDebugMode = sharedPref.getBoolean(context.getResources().getString(R.string.preference_debugMode_key), true);
        bSetAutoDownload = sharedPref.getBoolean(context.getResources().getString(R.string.preference_download_automatic_key), true);
        bSetAutoStart = sharedPref.getBoolean(context.getResources().getString(R.string.preference_enable_autostart_key), true);
        bSetDownloadWifiOnly = sharedPref.getBoolean(context.getResources().getString(R.string.preference_DownloadInWifiOnly_key), true);
        bSetMoveFilesToSdcard = sharedPref.getBoolean(context.getResources().getString(R.string.preference_MoveFilesToSdcard_key), true);
        sSetDownloadTime = sharedPref.getString(context.getResources().getString(R.string.preference_download_time_key), null);
        String sFilesToKeep = sharedPref.getString(context.getResources().getString(R.string.preference_filestokeep_key), "0");
        iFilesToKeep = Integer.parseInt(sFilesToKeep); // convert String to Integer

        // Searches for Standard download directory and sets directory-variable
        MainActivity.ScanDefaultDownloadDirectory();

        if(bSetMoveFilesToSdcard)
            {   // Set File Path to ExtSdCard
                sFilePath = sFilePathExtSdcardFolder;
            }
        else
            {   // Set File Path to DefaultSdCardFolder
                sFilePath = sFilePathIntMemoryFolder;
            }
        // ToDo: Throws NumberFormatException if string cannot be parsed as an integer value.

        Log.d(LOG_TAG, "Settings updated");
    }

    public static boolean setAlarm(Context context, boolean bDisplayToast) {
        if (pendingIntent != null) {// Cancel existing alarms
            alarmManager.cancel(pendingIntent);}
        if (!bSetAutoDownload) // Check if automatic Download is active
            {
                Log.d(ListviewFragment.LOG_TAG, "Automatic Download is disabled. bSetAutoDownload is " + bSetAutoDownload + " and bDisplayToast is " + bDisplayToast);
                if(bDisplayToast)
                    {
                       Toast.makeText(context, "Automatic Download is disabled. No Alarm has been set", Toast.LENGTH_SHORT).show(); //todo move into strings
                    }
                return true; // done if automatic downloading is disabled; do not set any new alarm
            }

        long downloadTime = getDownloadTime(context, bDisplayToast); //calculate new Download Time
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, downloadTime, pendingIntent);    // Set new Alarm
        Log.d(ListviewFragment.LOG_TAG, "Alarm has been set on " + downloadTime + " ms");
        return true;
    }

    public static int loadTodaysEdition(Context context, boolean bShowToast) { // todo seperate internet connection state from this fucntion
        // Check for Internet and WiFi Connection
        int DownloadStarted=0;
        if (haveNetworkConnection(context)) {
            boolean bStartDownload = true;

            if (bSetDownloadWifiOnly)                           // if Wifi is required
            {
                bStartDownload = haveWifiConnection(context);
            } // check if it is available

            if (bStartDownload) {   // Start Download
                Toast.makeText(context, "Starte Download", Toast.LENGTH_SHORT).show();

                DownloadNewspaperTask asyncTaskDownloadTodaysEditoin = new DownloadNewspaperTask();      // Generate AsyncTask
                asyncTaskDownloadTodaysEditoin.execute("Text");                         // Start AsyncTask
                // It would be possible to pass strings to the asyncTask within the ""

                sNotificationTitle = "Download started";                // Set Notification Strings
                sNotificationText = "via WiFi " + getDateStringNow();  // Set Notification Strings
                DownloadStarted = 0; // Download started successfully

            } else {   // Do Not Start Download, because there is no Wifi available
                sNotificationTitle = "Download failed";                             // Set Notification Strings
                sNotificationText = "No WiFi Connection " + getDateStringNow();    // Set Notification Strings

                Toast.makeText(context, "No WiFi available!",
                        Toast.LENGTH_SHORT).show();

                DownloadStarted = 1; // Download not started; Internet connection available, but no wifi connected

                releaseTheLock();
            }
        } else {   // Do Not Start Download, because there is no internet connection available
            Toast.makeText(context, "No Internet Connection available!",
                    Toast.LENGTH_SHORT).show();
            sNotificationTitle = "Download failed";                                         // Set Notification Strings
            sNotificationText = "No Network Connection available " + getDateStringNow();   // Set Notification Strings
            DownloadStarted = 2; // Download not started; No internet connection available

        }

        issueNotification(context);

        // Set new alarm (for the next day)
        setAlarm(context, bShowToast);
        return DownloadStarted;
    }

    public static void issueNotification(Context context) { // todo: add on click open app
        Intent resultIntent = new Intent(context, MainActivity.class);
// Because clicking the notification opens a new ("special") activity, there's
// no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // generate Notifiaction
        Notification noti = new Notification.Builder(context)
                .setContentTitle(sNotificationTitle)
                .setContentText(sNotificationText)
                .setSmallIcon(R.drawable.ic_news_52)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .build();

        // Gets an instance of the NotificationManager service
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issues the notificatoin
        mNotificationManager.notify(mNotificationId, noti);
    }

    // Innere Klasse DownloadNewspaperTask führt den asynchronen Task auf eigenem Arbeitsthread aus
    public static class DownloadNewspaperTask extends AsyncTask<String, Integer, String[]> { //ToDo cleanup this function

        @Override
        protected String[] doInBackground(String... strings) {

            // passed Strings can be acessed with strings[0], strings[1], etc.

                    /*
                    // Expected order of links-types
                    // String sUrl1 = "http://edition.idowa.de/edition";
                    // String sUrl2 = "http://edition.idowa.de/edition/login.jsp;jsessionid=EE7BD5936AE4FC8EEDE9B256AA27C9F7";
                    // String sUrl3 = "http://edition.idowa.de/edition/servlet/users;jsessionid=EE7BD5936AE4FC8EEDE9B256AA27C9F7?application=http://edition.idowa.de/edition&j_password=12345678&abonummer=123456";
                    // String sUrl4 = "http://edition.idowa.de/edition/data/20160104/01/Allgemeine_Laber_Zeitung/index.jsp;jsessionid=EE7BD5936AE4FC8EEDE9B256AA27C9F7?&application=http://edition.idowa.de/edition";
                    // String sUrl5 = "http://edition.idowa.de/edition/servlet/downloadPDF;jsessionid=EE7BD5936AE4FC8EEDE9B256AA27C9F7?edatum=20160104&amp;edition=Allgemeine_Laber_Zeitung&amp;file=/data/20160104/pdf/LAB_20160104_4311.pdf";
                    */

            int iSecTimeoutMil = 1000;

            // Technical variables for idowa.de
            String sUrl1 = "http://edition.idowa.de/edition";
            String sSelect1 = "script";
            String sSearch1 = "http:(.+?)login(.+?)de/edition";    // Regex for the value of the key

            String sUrl2 = "0";
            String sSelect2 = "script";
            String sSearch2 = "http:(.+?)servlet(.+?)de/edition";    // Regex for the value of the key

            String sUrl3 = "0";
            String sSelect3 = "script";
            String sSearch3 = "http:(.+?)Allgemeine(.+?)de/edition"; // Regex for the value of the key

            String sUrl4 = "0";
            String sSelect4 = "a[id=getpdf_pict]";
            String sSearch4 = "http:(.+?)downloadPDF(.+?)[.]pdf";    // Regex for the value of the key

            String sUrl5 = "0";

            // Start of SW
            Log.i(LOG_TAG, "sUrl1: " + sUrl1);

            // Search for URL2
            // ###############
            // expected type of URL:
            // http://edition.idowa.de/edition/login.jsp;jsessionid=4930D5B9359086D6C0C2BAE58617D6E1
            // ?application=http://edition.idowa.de/edition

            try {
                sUrl2 = searchUrlwoPw(
                        sUrl1,
                        iSecTimeoutMil,
                        sSelect1,
                        sSearch1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.i(LOG_TAG, "sUrl2: " + sUrl2);

            // Search for URL3
            // ###############
            // expected type of URL:
            // http://edition.idowa.de/edition/servlet/users;jsessionid=4930D5B9359086D6C0C2BAE58617D6E1
            // ?application=http://edition.idowa.de/edition

            //&j_password=12345678&abonummer=123456";
            try {
                sUrl3 = searchUrlwoPw(
                        sUrl2,
                        iSecTimeoutMil,
                        sSelect2,
                        sSearch2);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.i(LOG_TAG, "sUrl3: " + sUrl3);

            // Search for URL4
            // ###############
            // expected type of URL:
            // http://edition.idowa.de/edition/data/20160105/01/Allgemeine_Laber_Zeitung/index.jsp;
            // jsessionid=4930D5B9359086D6C0C2BAE58617D6E1?&application=http://edition.idowa.de/edition
            try {
                sUrl4 = searchUrlwPw(
                        sUrl3,
                        sSelect3,
                        sSearch3,
                        sSetLoginUsername,
                        sSetLoginPassword);
            } catch (IOException e) {
                e.printStackTrace();
            }


            Log.i(LOG_TAG, "sUrl4: " + sUrl4);


            // Search for URL5
            // ###############
            // expected type of URL:
            // http://edition.idowa.de/edition/data/20160105/01/Allgemeine_Laber_Zeitung/index.jsp;
            // jsessionid=4930D5B9359086D6C0C2BAE58617D6E1?&application=http://edition.idowa.de/edition
            try {
                sUrl5 = searchUrlwPw(
                        sUrl4,
                        sSelect4,
                        sSearch4,
                        sSetLoginUsername,
                        sSetLoginPassword);
            } catch (IOException e) {
                e.printStackTrace();
            }


            Log.i(LOG_TAG, "sUrl5: " + sUrl5);

            // URL5 is ready to be downloaded i.e. with Firefox (-> shows progress bar)
            //####################################################
            String[] ergebnisArray = new String[20];
            ergebnisArray[0] = sUrl5;
            ergebnisArray[1] = sUrl3 + "&j_password=" + sSetLoginPassword + "&abonummer=" + sSetLoginUsername;
            return ergebnisArray;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onPostExecute(String[] strings) {
            // Wir löschen den Inhalt des ArrayAdapters und fügen den neuen Inhalt ein
            // Der neue Inhalt ist der Rückgabewert von doInBackground(String...) also
            // der StringArray gefüllt mit Beispieldaten
            if (strings != null) {

                if (strings[0] != "0" && strings[1]!= "0" )
                {
                    // only for debug:
                    Log.d(ListviewFragment.LOG_TAG, "Strings.length: " + strings.length);
                    for (int i = 0; i < strings.length; i++) {
                        Log.d(ListviewFragment.LOG_TAG, "Strings[" + i + "]: " + strings[i]);
                    }

                    // UrlExtraction has been finished, inform user
                    Toast.makeText(mContext, "URL erfolgreich extrahiert! \n Starte Browser für Download",
                            Toast.LENGTH_SHORT).show();



                    // Launch browser with this code
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(strings[1]));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);

                    final String Urlstring2 = strings[0];

                    // Execute some code after 2 seconds have passed
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            Intent intent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(Urlstring2));
                            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            //intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            //intent.setComponent(new ComponentName("org.mozilla.firefox", "org.mozilla.firefox.App"));
                            //intent.setAction("org.mozilla.gecko.BOOKMARK");
                            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            //intent.putExtra("args", "--url=" + strings[0]);
                            //intent.setData(Uri.parse(strings[0]));
                            mContext.startActivity(intent2);
                        }
                    }, 5000);


                    // ToDo: check what this code does exactyl. somehow it is a doublicate
                    Intent intent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(strings[0]));
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    //intent.setComponent(new ComponentName("org.mozilla.firefox", "org.mozilla.firefox.App"));
                    //intent.setAction("org.mozilla.gecko.BOOKMARK");
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //intent.putExtra("args", "--url=" + strings[0]);
                    //intent.setData(Uri.parse(strings[0]));
                    mContext.startActivity(intent2);

                    // Duwnload sucessfully started -> increment counter on Samsung TouchWiz surface
                    MainActivity.BadgeIncrement(mContext);

                } else {

                    // Error in URL Extraction
                    Log.d(ListviewFragment.LOG_TAG, "Error in URL-Extraction. See log");
                    Toast.makeText(mContext, "Fehler bei URL-Extrahierung! \n Überprüfen Sie bitte die Login-Daten!",
                            Toast.LENGTH_SHORT).show();

                }
            }

            releaseTheLock();
        }


    }

    private static String searchUrlwPw( // Search Url with Password //ToDo cleanup this function
                                        String sUrlCurrent,
                                        String sSelect,
                                        String sSearch,
                                        String sUsername,
                                        String sPassword)
            throws IOException {

        Pattern p;
        Matcher m;
        Elements eSearchResult;
        Document DocCurrent;
        Connection.Response loginForm;
        String sUrlRet = "http://www.google.de";

        // Connect to new URL
        loginForm = Jsoup.connect(sUrlCurrent)
                .method(Connection.Method.GET)
                .execute();

        DocCurrent = Jsoup.connect(sUrlCurrent)
                .method(Connection.Method.POST)
                        // .data("application", "http://edition.idowa.de/edition")
                .data("j_password", sPassword)
                .data("abonummer", sUsername)
                        // .data("application", "http://edition.idowa.de/edition")
                        // .cookies(loginForm.cookies())
                .post();
        //System.out.println(DocCurrent);

        eSearchResult = DocCurrent.select(sSelect);
        //System.out.println(eSearchResult);

        // find new URL
        p = Pattern.compile(sSearch); // Regex for the value of the key
        m = p.matcher(eSearchResult.outerHtml()); // you have to use html here and NOT text! Text will drop the 'key' part

        while (m.find()) {
            //System.out.println(m.group()); // the whole key ('key = value'
            //ToDo: to be transfered into android message
            sUrlRet = (m.group());
        }

							/*if(sUrlRet=="http://www.google.de")
							{
								// then error: link could not be found.
								// send error message with description
							}
							*/


        return sUrlRet;
    }


    private static String searchUrlwoPw(// Search Url without Password //ToDo cleanup this function
                                        String sUrlCurrent,
                                        int iSecTimeoutMil,
                                        String sSelect,
                                        String sSearch)
            throws IOException {

        Pattern p;
        Matcher m;
        Elements eSearchResult;
        Document DocCurrent;
        Connection.Response loginForm;
        String sUrlRet = "http://www.google.de";

        URL UrlCurrent = new URL(sUrlCurrent);        // transfer string into data type url (required for Jsoup.parse() method)
        DocCurrent = Jsoup.parse(UrlCurrent, iSecTimeoutMil);
        // System.out.println(DocCurrent);


        eSearchResult = DocCurrent.select(sSelect);
        //System.out.println(eSearchResult);

        // find new URL
        p = Pattern.compile(sSearch); // Regex for the value of the key
        m = p.matcher(eSearchResult.outerHtml()); // you have to use html here and NOT text! Text will drop the 'key' part

        while (m.find()) {
            //System.out.println(m.group()); // the whole key ('key = value')
            //ToDo: to be transfered into android message
            sUrlRet = (m.group());
        }

						/*if(sUrlRet=="http://www.google.de")
						{
							// then error: link could not be found.
							// send error message with description
						}
						*/

        return sUrlRet;
    }


    public static boolean haveNetworkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null) && netInfo.isConnected();        //true if network connectivity exists, false otherwise.
    }

    public static boolean haveWifiConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null) // detect that no active network is available
            {
                return false;
            }
        if (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
            {
                return true;
            }      // WiFi is available
        else
            {
                return false;
            }     // WiFi is not available
    }

    public static String getDateStringNow() {
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        int Day = c.get(Calendar.DAY_OF_MONTH);
        int Month = 1 + c.get(Calendar.MONTH); // January is Zero
        int Year = c.get(Calendar.YEAR);
        int Hour = c.get(Calendar.HOUR_OF_DAY);
        int Minutes = c.get(Calendar.MINUTE);
        String date = String.format("%02d.%02d.%04d %02d:%02d", Day, Month, Year, Hour, Minutes);
        return date;
    }

    public static long getDownloadTime(Context context, boolean bDisplayToast) {
        Calendar c = Calendar.getInstance();

        // get download time and convert into milliseconds
        if(sSetDownloadTime==null){sSetDownloadTime="06:00";} //if no initiallisation is found //todo
        String[] timef = sSetDownloadTime.split(":");
        int iHour = Integer.parseInt(timef[0]);
        int iMinute = Integer.parseInt(timef[1]);
        long lDownloadTime = ((60 * iMinute) + (3600 * iHour)) * 1000;  //in milliseconds

        // get current time in milliseconds
        int t_minute = c.get(Calendar.MINUTE);
        int t_hour = c.get(Calendar.HOUR_OF_DAY);
        int t_second = c.get(Calendar.SECOND);
        long time_today_in_millisec = (t_second + t_minute * 60 + t_hour * 3600) * 1000;

        //Get Time in millisecs of todays time
        long t_now = c.getTimeInMillis();
        long t_date = t_now - time_today_in_millisec;

        // calculate download time
        int day_of_week = c.get(Calendar.DAY_OF_WEEK);
        if (time_today_in_millisec >= lDownloadTime)   // next download tomorrow
        {
            t_date += 86400000;                         // plus 1day in milliseconds
            if (day_of_week == 7)
                t_date += 86400000;      // Skip download on Sunday // SATURDAY is 7
        } else                                            // next download today
        {
            if (day_of_week == 1) t_date += 86400000;      // Skip download on Sunday // SUNDAY is 1
        }

        long triggerAtMillis = t_date + lDownloadTime;  // calculate download time

        if (bSetDebugMode == true)                             // If in debug mode, modifly trigger time
        {
            triggerAtMillis = t_now + t_debug_time;
        }

        if(bDisplayToast)
        {
            String sDate = getDateString(triggerAtMillis, "cccc dd.MM.yyyy HH:mm");
            Toast.makeText(context, "Next automatic Download is scheduled on: " + sDate, Toast.LENGTH_SHORT).show();
        }

        return triggerAtMillis;

    }

    /**
     * // Source: http://stackoverflow.com/questions/7953725/how-to-convert-milliseconds-to-date-format-in-android
     * Return date in specified format.
     *
     * @param milliSeconds Date in milliseconds
     * @param dateFormat   Date format
     * @return String representing date in specified format
     */

    public static String getDateString(long milliSeconds, String dateFormat) {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);         // Set time
        return formatter.format(calendar.getTime());    // Return formatted String
    }


    public static void removeOldFiles(String strings[]) {

        Log.d(ListviewFragment.LOG_TAG, "removeOldFiles() has been called, iFilesToKeep ist" + iFilesToKeep);

        if (iFilesToKeep!=0)
        {
            if (strings.length > iFilesToKeep) {
                int NumberOfFilesToDelete = strings.length - iFilesToKeep;

                Log.d(ListviewFragment.LOG_TAG, "Nr of Files in AppFolder to delete: " + NumberOfFilesToDelete );

                for (int i = 0; i < NumberOfFilesToDelete; i++) {

                    int index = strings.length -1 -i; // get correct index, in order to delete the oldest editions
                    Log.d(ListviewFragment.LOG_TAG, "Delete file in AppFolder: " + strings[index]);

                    Toast.makeText(mContext, "Lösche Datei " + (i+1) + " von " + NumberOfFilesToDelete, Toast.LENGTH_SHORT).show();

                    //String inputPath = "/storage/extSdCard/Android/data/de.othregensburg.AutomaticNewspaperDownloader/files/";
                    MainActivity.deleteFile(sFilePath, strings[index]); // delete file[index]
                }

                Toast.makeText(mContext, NumberOfFilesToDelete + " Dateien erfolgreich gelöscht", Toast.LENGTH_SHORT).show();
            }
        }
        // UpdateListView
        ListviewFragment.arrayListeFiles = MainActivity.scanSdCardFolder();
        ListviewFragment.refreshView(ListviewFragment.arrayListeFiles, mContext);
        return;

    }
}