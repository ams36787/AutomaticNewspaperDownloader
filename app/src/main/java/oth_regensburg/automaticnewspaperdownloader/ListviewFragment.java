package oth_regensburg.automaticnewspaperdownloader;

        import android.app.AlertDialog;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.net.Uri;
        import android.os.Bundle;
        import android.util.Log;
        import android.support.v4.app.Fragment;
        import android.view.LayoutInflater;

        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.AdapterView;
        import android.widget.ArrayAdapter;
        import android.widget.ListView;
        import android.widget.Toast;

        import java.io.File;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Date;
        import java.util.List;
        import java.util.Locale;

        import android.support.v4.widget.SwipeRefreshLayout;


public class ListviewFragment extends Fragment {

    // Der ArrayAdapter ist jetzt eine Membervariable der Klasse ListviewFragment
    public static ArrayAdapter<String> mNewspaperListAdapter;
    public static SwipeRefreshLayout mSwipeRefreshLayout;

    public static Context fragmentContext;
    public static Context applicationContext;

    public static String[] arrayListeFiles;
    public static String[] arrayListeAddInfo;

    public static String LOG_TAG = "de.othregensburg.AND";



    public ListviewFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Menü bekannt geben, dadurch kann unser Fragment Menü-Events verarbeiten
        setHasOptionsMenu(true);

        // create an Intent and set the class which will execute when Alarm triggers, here we have
        // given AlarmReciever in the Intent, the onRecieve() method of this class will execute when
        // alarm triggers and
        // we will write the code inside onRecieve() method pf Alarmreciever class

        Context context = getActivity();  // getActivtiy() returns the Activity the Fragment is currently associated with.
        //InitAlarmManager(context);

        fragmentContext =  getActivity();
        applicationContext = fragmentContext.getApplicationContext();

        Intent intentService = new Intent(context, AutoStartUpService.class);
        // intentService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intentService);

        //setAlarm(getActivity()); //set alarm once started up
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_listefragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Wir prüfen, ob Menü-Element mit der ID "action_daten_aktualisieren"
        // ausgewählt wurde und geben eine Meldung aus
        int id = item.getItemId();

        if (id == R.id.action_load_todays_edition)
        {
            AutoStartUpService.loadTodaysEdition(getActivity(), false);
        }

        // ToDo: manual download of certain newspaper edition using CalendarDatePicker from Betterpickers

        return super.onOptionsItemSelected(item);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        arrayListeFiles = MainActivity.scanSdCardFolder();


        List<String> userlist = new ArrayList<>(Arrays.asList(arrayListeFiles));

        mNewspaperListAdapter =
                new ArrayAdapter<>(
                        getActivity(),                          // Die aktuelle Umgebung (diese Activity)
                        R.layout.list_item_liste,              // ID der XML-Layout Datei
                        R.id.list_item_newspaperlist_textview,  // ID des TextViews
                        userlist);                              // Beispieldaten in einer ArrayList

        View rootView = inflater.inflate(R.layout.fragment_liste, container, false);

        //update settings before updating the view
        AutoStartUpService.updateSettings(getActivity()); //todo check if this works

        // update View with correct Layout
        refreshView(arrayListeFiles);


// Eine Referenz zu unserem ListView, und Verbinden des ArrayAdapters mit dem ListView
// Anschließend Registrieren eines OnItemClickListener für den ListView
        ListView newspaperListView = (ListView) rootView.findViewById(R.id.listview_userlist);
        newspaperListView.setAdapter(mNewspaperListAdapter);

        newspaperListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                String sFileName = arrayListeFiles[position];
                Log.d(ListviewFragment.LOG_TAG, "Selected File: " + sFileName + " at Position: " + position);

                // former implementation:
                // String sFileName = (String) adapterView.getItemAtPosition(position);

                openSelectedPdfFile(AutoStartUpService.sFilePath, sFileName);
            }
        });

        newspaperListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                // This is a quick & dirty way to implement the AlertDialog. According to the Android Documentation an addtional AlertFragement should be used.
                // See: http://developer.android.com/guide/topics/ui/dialogs.html

                // todo set global variable with path
                //String sFilePath = "/storage/extSdCard/Android/data/de.othregensburg.AutomaticNewspaperDownloader/files/";

                String sFileName = arrayListeFiles[position];
                Log.d(ListviewFragment.LOG_TAG, "Selected File: " + sFileName + " at Position: " + position);


                MainActivity.sDelFilePath = AutoStartUpService.sFilePath; // set variables for deleting the file
                MainActivity.sDelFileName = sFileName; // set variables for deleting the file

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                String sAlertQuestion = getResources().getString(R.string.alert_delete);
                builder.setMessage(sAlertQuestion + sFileName)
                        .setPositiveButton(R.string.altert_yes, dialogClickListener)
                        .setNegativeButton(R.string.alert_no, dialogClickListener).show();

                return true;
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout_list);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AutoStartUpService.updateSettings(getActivity());
                AutoStartUpService.setAlarm(getActivity(), false);
                MainActivity.moveFilesToSdCard(getActivity());
            }
        });

        return rootView;

    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked

                    Toast.makeText(getActivity(), R.string.alert_delete_confirmed,
                            Toast.LENGTH_SHORT).show();

                    // Delete File
                    MainActivity.deleteFile();

                    // Refresh View
                    arrayListeFiles = MainActivity.scanSdCardFolder();
                    refreshView(arrayListeFiles);

                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked

                    Toast.makeText(getActivity(), R.string.alert_delete_aborted,
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public static void refreshView(String strings[])
    {   // updates the View
        ListviewFragment.mNewspaperListAdapter.clear();

        String[] TemparrayListeAddInfo = new String[arrayListeFiles.length];

        for (int i=0; i<arrayListeFiles.length; i++)
        {
            // Update View with the following to variables:
            // 1. Filenames:        arrayListeFiles
            // 2. AdditionalInfo:   TemparrayListeAddInfo

            TemparrayListeAddInfo[i] = getAddInfoFromFilename(arrayListeFiles[i]); // 20160217_01_Allgemeine_Laber_Zeitung.pdf
            Log.d(ListviewFragment.LOG_TAG, "getAddInfoFromFilename: " + TemparrayListeAddInfo[i] );
            ListviewFragment.mNewspaperListAdapter.add(TemparrayListeAddInfo[i] + " \n" + arrayListeFiles[i]);

        } // ToDo: Continue here!

        arrayListeAddInfo = TemparrayListeAddInfo;


    }

    /**
     * Checks if any apps are installed that supports reading of PDF files.
     * @param context
     * @return
     * Source: http://stackoverflow.com/questions/2456344/display-pdf-within-app-on-android
     */
    public static boolean isPDFSupported( Context context, String path, String file ) {
        Intent i = new Intent( Intent.ACTION_VIEW );
        final File tempFile = new File( path, file );
        i.setDataAndType( Uri.fromFile(tempFile), "application/pdf" );
        return context.getPackageManager().queryIntentActivities( i, PackageManager.MATCH_DEFAULT_ONLY ).size() > 0;
    }


    public static final void openPDF(Context context, Uri localUri ) {
        Intent i = new Intent( Intent.ACTION_VIEW );
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        // If set, the new activity is not kept in the history stack. As soon as the user navigates away from it, the activity is finished. This may also be set with the noHistory attribute.
        // Source: Android Documentation
        // http://developer.android.com/reference/android/content/Intent.html#FLAG_ACTIVITY_CLEAR_TOP
        i.setDataAndType(localUri, "application/pdf");
        context.startActivity(i);
    }



    public static String getAddInfoFromFilename(String string)
    {   // Expecting Strings in the following Format
        // 20160217_01_Allgemeine_Laber_Zeitung.pdf
        //do
        String sReturnString = null;
        String strings[];
        strings = string.split("_");
        // create Calender
        String sdf = "";

        Log.d(ListviewFragment.LOG_TAG, "DateString " + strings[0]);

        if(strings[0].startsWith("Fehler")) // if error occours, do not try to generate String //todo move into xml file
        {
            sReturnString = "Achtung, es ist ein Fehler aufgetreten!"; //todo move into xml file
            return sReturnString;
        }

        try {
            Date date = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).parse(strings[0]);
            sReturnString = AutoStartUpService.getDateString(date.getTime(), "'Ausgabe vom 'EEEE', dem 'dd'. 'MMMM yyyy");
        } catch (Exception  e) {
            //Handle exception here, most of the time you will just log it.
            e.printStackTrace();
        }
        return sReturnString;
        // Returns String in the following format
        // Ausgabe vom Montag, dem 10. Februar 2015
        //
    }


    private void openSelectedPdfFile(String path, String sDetailName)
    {
        // Todo cleanup this code mess
        // check if file exists
        File fPdf = new File(path + sDetailName);
        if ( !(fPdf.exists()) )
        {
            // file does not exist
            Toast.makeText(getActivity(), "Error: File does not exist",
                    Toast.LENGTH_SHORT).show();
            Log.d(ListviewFragment.LOG_TAG, "Error: File does not exist");
            return;
        }

        // check if pdf is supported; no app for viewing pdfs installed
        if ( !(isPDFSupported(getActivity(), path, sDetailName)) )
        {
            // PDF is not supported
            Toast.makeText(getActivity(), "Error: No supported PDF-Reader installed",
                    Toast.LENGTH_SHORT).show();
            Log.d(ListviewFragment.LOG_TAG, "Error: No supported PDF-Reader installed");
            return;
        }

        // open PDF
        Uri uriPdfFile = Uri.fromFile(fPdf);
        Log.d(ListviewFragment.LOG_TAG, "localUri: " + uriPdfFile.toString() );
        Toast.makeText(getActivity(), "Opening File: " + sDetailName,
                Toast.LENGTH_SHORT).show();
        openPDF(getActivity(), uriPdfFile);
        return;
    }


}