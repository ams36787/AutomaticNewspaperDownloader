package oth_regensburg.automaticnewspaperdownloader;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends ActionBarActivity {
    // ToDo: update from ActionBarActivity to AppCompatActivity (ActionBarActivity is deprecated)

    private static File listOfFiles[] = null;
    private static File directory = null;
    private static Context mContext = null;
    private static FilenameFilter filter = new FilenameFilter(){
        public boolean accept(File dir, String string)
        {   // typical syntax of filename: 20160217_01_Allgemeine_Laber_Zeitung.pdf
            // ToDo: Modify here, if different Newspaper is used.

            // return string.endsWith("Allgemeine_Laber_Zeitung.pdf");

            // But this does not include Files that are downloaded twice.
            // In Example 20160217_01_Allgemeine_Laber_Zeitung(2).pdf is not recognised

            // This could be done using the following 2 code lines:
            String sRegEx = "(.+?)Allgemeine_Laber_Zeitung(.*?).pdf";
            return string.matches(sRegEx);

            // RegEx String for:
            // - (.+?)                      : String Starts with at least one character (any)
            // - Allgemeine_Laber_Zeitung   :  Followed by this String
            // - (.*?)                      : String may contain any more characters
            // - .pdf                       : string ends with .pdf
        }
    };

    // Todo: this is not the ideal way to do, should not be public, but it is required for now
    public static String sDelFilePath = null; // create variables for deleting files with deleteFile();
    public static String sDelFileName = null; // create variables for deleting files with deleteFile();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // move files into app folder
        moveFilesToSdCard(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Start Preferences Intent
            Log.d(AutoStartUpService.LOG_TAG, "Start Preferences-Intent");
            startActivity(new Intent(this, SettingsActivity.class));


            //todo: remove following line
            BadgeDecrement(this);

            return true;
        }

        if (id == R.id.action_contact) {
            // generate intent and start with expicit intent

            //todo: remove following line
            BadgeIncrement(this);

            Intent intentContact = new Intent(this, ContactActivity.class);
            startActivity(intentContact);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    public static void BadgeIncrement(Context context)
    {
        int iBadgeCounter;
        iBadgeCounter = BadgeRead(context);
        iBadgeCounter++;
        BadgeUpdate(iBadgeCounter, context);
    }

    public static void BadgeDecrement(Context context)
    {
        int iBadgeCounter;
        iBadgeCounter = BadgeRead(context);
        if(iBadgeCounter!=0)
        {
            iBadgeCounter--;
            BadgeUpdate(iBadgeCounter, context);
        }
    }

    public static int BadgeRead(Context context)
    {

        int iBadgeCount=0;
        // This is the content uri for the BadgeProvider
        Uri uri = Uri.parse("content://com.sec.badge/apps");

        Cursor c = context.getContentResolver().query(uri, null, null, null, null);

// This indicates the provider doesn't exist and you probably aren't running
// on a Samsung phone running TWLauncher. This has to be outside of try/finally block
        if (c == null) {
            return 0;
        }

        try {
            if (!c.moveToFirst()) {
                // No results. Nothing to query
                return 0;
            }

            c.moveToPosition(-1);
            while (c.moveToNext()) {
                String pkg = c.getString(1);
                String clazz = c.getString(2);
                int badgeCount = c.getInt(3);
                Log.d("BadgeTest", "package: " + pkg + ", class: " + clazz + ", count: " + String.valueOf(badgeCount));
                if (clazz.equals("oth_regensburg.automaticnewspaperdownloader.MainActivity"))
                {
                    Log.d("BadgeTest", "The badge count is currently " + String.valueOf(badgeCount));
                    iBadgeCount=badgeCount;
                }
            }
        } finally {
            c.close();
        }
        return iBadgeCount;
    }


    public static boolean BadgeUpdate(int iNewBadgeValue, Context context)
        {
        //todo:
        // debug for samsung badge support
        ContentValues cv = new ContentValues();
        cv.put("package", context.getPackageName());
        // Name of your activity declared in the manifest as android.intent.action.MAIN.
        // Must be fully qualified name as shown below
        cv.put("class", "oth_regensburg.automaticnewspaperdownloader.MainActivity");
        cv.put("badgecount", iNewBadgeValue); // integer count you want to display

        // Execute insert
        context.getContentResolver().insert(Uri.parse("content://com.sec.badge/apps"), cv);
            return true;
    }



    public static boolean BadgeRemove(Context context)
    {
        //todo:
        // debug for samsung badge support
        ContentValues cv = new ContentValues();
        cv.put("badgecount", 0);
        context.getContentResolver().update(Uri.parse("content://com.sec.badge/apps"), cv, "package=?", new String[] {context.getPackageName()});
        return true;
    }

    public static boolean moveFilesToSdCard(Context context)
    {
        mContext = context;
        // public static
        String DIRECTORY_DOWNLOADS = "/storage/extSdCard/Download/";

        // find all files that contain Allgemeine_Laber_Zeitung

        // Permission required!
        // <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
        // <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

        // Searches for Standard download directory and sets directory-variable
        ScanDefaultDownloadDirectory();

        if(!AutoStartUpService.bSetMoveFilesToSdcard)
        {
            // do not move files
            // no update required, but still do it..
            ListviewFragment.arrayListeFiles = scanSdCardFolder();
            AutoStartUpService.removeOldFiles(ListviewFragment.arrayListeFiles);

            // Finish Swipe Refresh
            ListviewFragment.mSwipeRefreshLayout.setRefreshing(false);

            return false;
        }
        // else: do move files

        listOfFiles = directory.listFiles(filter);

        if(listOfFiles==null)
        {
            Log.d(AutoStartUpService.LOG_TAG, "directory is empty"); // ToDo: is that really the cye?
            return false; // file is no directory
        }

        Log.d(AutoStartUpService.LOG_TAG, "directory is not empty. It contains " + listOfFiles.length + " files");

        // check if there is an folder on the sd card
        File listOfExtFolders[] = context.getExternalFilesDirs(null);

        for (int i=0; i<listOfExtFolders.length; i++ )
            {
                if(listOfExtFolders[i]!=null)
                { // File Nr.0: /storage/emulated/0/Android/data/oth_regensburg.automaticnewspaperdownloader/files
                    Log.d(AutoStartUpService.LOG_TAG, "Folder Nr." + i + " Path: " + listOfExtFolders[i].getAbsolutePath());
                }
                else
                {
                    Log.d(AutoStartUpService.LOG_TAG, "Folder Nr." + i + " is currently not available (check if SD Card is inserted properly)");
                }
            }

        File fTempDir = new File(AutoStartUpService.sFilePath);

        if(listOfFiles.length>=1            //if there are files to be moved
                && fTempDir.isDirectory() ) // only move files if target folder exists (equals is a directory)
            {

                //inform user about the moving process
                Toast.makeText(context, R.string.toast_move_files,
                        Toast.LENGTH_SHORT).show();

                MoveFilesAsyncTask asyncTaskMoveFiles = new MoveFilesAsyncTask();      // Generate AsyncTask
                asyncTaskMoveFiles.execute("text");                         // Start AsyncTask
                // It would be possible to pass strings to the asyncTask within the ""
            }
        else
            {
                // no update required, but snl do it..
                ListviewFragment.arrayListeFiles = scanSdCardFolder();
                AutoStartUpService.setContext(mContext);
                AutoStartUpService.removeOldFiles(ListviewFragment.arrayListeFiles);

                // Finish Swipe Refresh
                ListviewFragment.mSwipeRefreshLayout.setRefreshing(false);

            }

        return true;
    }

    public static void ScanDefaultDownloadDirectory()
    {
        directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // /storage/emulated/0/Download

        Log.d(AutoStartUpService.LOG_TAG, "Default Download Directory: " + directory.getAbsolutePath());
        Log.d(AutoStartUpService.LOG_TAG, "File is a Directory: " + directory.isDirectory());

        // Set File Destination for Internal Memory
        AutoStartUpService.sFilePathIntMemoryFolder = directory.getAbsolutePath() + "/";
        return;
    }


    // Source: http://stackoverflow.com/questions/4178168/how-to-programmatically-move-copy-and-delete-files-and-directories-on-sd
    static public void moveFile(String inputPath, String inputFile, String outputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {
            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }

            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + outputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + inputFile).delete();
        }

        catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
    }

    public static void deleteFile()
    {
        if ( (sDelFileName!=null) && (sDelFileName!=null) )
        {
            deleteFile(sDelFilePath, sDelFileName);
        }
        else
        {
            Log.d("LOG_TAG", "Canceled deleteFile() because input is null");
        }
    }

    // Source: http://stackoverflow.com/questions/4178168/how-to-programmatically-move-copy-and-delete-files-and-directories-on-sd
    static public void deleteFile(String inputPath, String inputFile) {
        try {
            Log.d("LOG_TAG", "Delete File: " + inputPath + inputFile);
            // delete the original file
            new File(inputPath + inputFile).delete();

            // }
            //catch (FileNotFoundException fnfe1) {
            //    Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
    }


    // Innere Klasse MoveFilesAsyncTask führt den asynchronen Task auf eigenem Arbeitsthread aus
    public static class MoveFilesAsyncTask extends AsyncTask<String, Integer, String[]> {

        @Override
        protected String[] doInBackground(String... strings) {
            // passed Strings can be acessed with strings[0], strings[1], etc.

            // move  and list files from sdcard
            for (int i=0; i<listOfFiles.length; i++ )
            {
                Log.d(AutoStartUpService.LOG_TAG, "Move from DownloadFolder to ExtSdCardFolder: File Nr." + i + ": " + listOfFiles[i].getName());
                Log.d(AutoStartUpService.LOG_TAG, "Source: " + directory.getAbsolutePath());
                Log.d(AutoStartUpService.LOG_TAG, "Target: " + AutoStartUpService.sFilePath);


                // publish progress
                publishProgress(i + 1, listOfFiles.length);

                moveFile(directory.getAbsolutePath() + "/", listOfFiles[i].getName(), listOfFiles[i].getName(), AutoStartUpService.sFilePath);
            }


            String returnarray[] = new String[5];
            returnarray[0] = listOfFiles.length + " ";
            return returnarray;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // Here it is possible to show a Toast with the current progress using
            // publishProgress(int...) in doInBackground(String...)
            //  Toast.makeText(getActivity(), "Progress: " + values[0] + "%",
            //         Toast.LENGTH_SHORT).show();

            Toast.makeText(mContext, "Verschiebe Datei "+ values[0] + " von " + values[1] + " ...",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(String[] strings) {
            // Wir löschen den Inhalt des ArrayAdapters und fügen den neuen Inhalt ein
            // Der neue Inhalt ist der Rückgabewert von doInBackground(String...) also
            // der StringArray gefüllt mit Beispieldaten
            Toast.makeText(mContext, listOfFiles.length + " Files moved!",
                    Toast.LENGTH_SHORT).show();

            Log.d(AutoStartUpService.LOG_TAG, "strings[0]:" + strings[0]);

            ListviewFragment.arrayListeFiles = scanSdCardFolder();

            AutoStartUpService.removeOldFiles(ListviewFragment.arrayListeFiles );

            // Finish Swipe Refresh
            ListviewFragment.mSwipeRefreshLayout.setRefreshing(false);

            Toast.makeText(mContext, "View Reloaded",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public static String[] scanSdCardFolder(){

        // Scan ExtSdCard folder for files
        File fExtSdCardDir = new File(AutoStartUpService.sFilePath);

        if(fExtSdCardDir == null)  //folder does not exist yet
        {
            fExtSdCardDir.mkdirs(); //create the folder
        }

        File listOfExtSdCardFiles[] = fExtSdCardDir.listFiles(filter);

        Log.d(AutoStartUpService.LOG_TAG, "ExtSdCard: Default Download Directory: " + fExtSdCardDir.getAbsolutePath());
        Log.d(AutoStartUpService.LOG_TAG, "ExtSdCard: File is a Directory: " + fExtSdCardDir.isDirectory());

        if(listOfExtSdCardFiles==null )  // File.listFiles(filter) returns null if this file is not a directory
        {
            Log.d(AutoStartUpService.LOG_TAG, "File is not a directory");
            String returnErrorArray[] = new String[1];

            returnErrorArray[0] = "Fehler: Das aktuelle Verzeichnis konnte nicht geladen werden. Überprüfen Sie bitte ob eine SD-Karte eingelegt ist bzw. deaktivieren Sie die entsprechende Option unter Einstellungen";
            return returnErrorArray; // file is no directory                    //todo move string to xml file
        }
        else if(listOfExtSdCardFiles.length == 0)
        {
            Log.d(AutoStartUpService.LOG_TAG, "directory is empty");
            String returnErrorArray[] = new String[1];
            returnErrorArray[0] = "Fehler: Im aktuellen Verzeichnis konnte keine Ausgabe gefunden werden. Bitte überprüfen Sie ob eine Datei Vorhanden ist"; //todo move string to xml file
            return returnErrorArray; // directory is empty
        }


        String returnArray[] = new String[listOfExtSdCardFiles.length];
        Log.d(AutoStartUpService.LOG_TAG, "directory is not empty");

        Log.d(AutoStartUpService.LOG_TAG, "Number of Files found: " + listOfExtSdCardFiles.length);

        for (int i=0; i<listOfExtSdCardFiles.length; i++ )
        {
            Log.d(AutoStartUpService.LOG_TAG, "Files in ExtSdCardFolder: File Nr." + i + ": " + listOfExtSdCardFiles[i].getName());
            returnArray[i] = listOfExtSdCardFiles[i].getName();
        }

        // sort returnArray into reverse-alphabetic order.. (=newest File first)
        Arrays.sort(returnArray);

        // reverse array
        String returnArray2[] = new String[returnArray.length];
        for(int i=0; i<returnArray.length; i++)
            {
                returnArray2[i]= returnArray[returnArray.length - i - 1];
            }
        return returnArray2;

    }

    public static void MakeToast(String string){
        Toast.makeText(mContext, string, Toast.LENGTH_SHORT).show();
    }

}