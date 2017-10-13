package com.example.ravikiran357.mc_assignment_2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity{
    GraphView g;
    LinearLayout graph;
    AccelerometerReceiver accelerometerReceiver;

    float[] valuesx, valuesy, valuesz;
    int valueArraySize = 10;

    Thread movingGraph = null;
    int threadSleepTime = 1000;
    Boolean flag = null;
    Boolean threadStartedFlag=false;
    Boolean downloadButtonPressed=false;

    Button buttonRun;
    Button buttonStop;
    Button buttonUpload;
    Button buttonDownload;

    EditText widgetPatientName;
    EditText widgetPatientID;
    EditText widgetPatientAge;
    RadioGroup widgetPatientSex;

//    PatientInfo patientInfo;
    boolean isMale = true;

    SQLiteDatabase db;
    private static final int PERMISSION_STORAGE = 1;
    public String TABLE = "Name_ID_Age_Sex";
    public static final String DATABASE_NAME = "group11";
    public static final String FILE_PATH = Environment.getExternalStorageDirectory() +
            File.separator + "Android/Data/CSE535_ASSIGNMENT2";
    public static final String DOWNLOAD_PATH = FILE_PATH + "_Extra";// + File.separator + DATABASE_NAME;
    public static final String DATABASE_LOCATION = FILE_PATH + File.separator + DATABASE_NAME;
    public static final String SERVER_LOCATION = "http://10.218.110.136/CSE535Fall17Folder/";

    Handler threadHandle = new Handler(){
        @Override
        public void handleMessage(Message msg){
            getDataFromDatabase();
            g.invalidate();
            g.setValues(valuesx, valuesy, valuesz);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, AccelerometerService.class);
        // Starts the accelerometer service
        this.startService(intent);
        accelerometerReceiver = new AccelerometerReceiver();
        // If Android version is Marshmellow or above, get the required permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissions();
        } else {
            setupModules();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void getPermissions() {
        int permissionCheck1 = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = this.checkSelfPermission(Manifest.permission.INTERNET);
        if (permissionCheck1 == PackageManager.PERMISSION_GRANTED &&
                (permissionCheck2 == PackageManager.PERMISSION_GRANTED)) {
            setupModules();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.INTERNET}, PERMISSION_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupModules();
                } else {
                    Toast.makeText(this, "Please restart the app", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void setupModules(){
        try{
            File folder = new File(FILE_PATH);
            if (!folder.exists()) {
                folder.mkdir();
            }
            // Android takes care of closing DB connection implicitly
            db = SQLiteDatabase.openOrCreateDatabase(DATABASE_LOCATION, null);
        } catch (SQLException e){
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("exp-setupmodules",e.getMessage());
        }

        setButtonsActions();
        setGraphUI();
    }

    private void setButtonsActions() {
        widgetPatientName = (EditText) findViewById(R.id.PatientNameText);
        widgetPatientAge = (EditText) findViewById(R.id.PatientAgeText);
        widgetPatientID = (EditText) findViewById(R.id.PatientIDText);
        widgetPatientSex = (RadioGroup) findViewById(R.id.radioGroup);
        buttonRun = (Button)findViewById(R.id.buttonRun);
        buttonStop = (Button)findViewById(R.id.buttonStop);
        buttonDownload = (Button)findViewById(R.id.buttonDownload);
        buttonUpload = (Button)findViewById(R.id.buttonUpload);
        buttonStop.setEnabled(false);

        widgetPatientSex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                isMale = checkedId != R.id.radioFemale;
            }
        });

        buttonRun.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //start
                if (widgetPatientName.getText().toString().matches("") ||
                        widgetPatientAge.getText().toString().matches("") ||
                        widgetPatientID.getText().toString().matches("")) {
                    Toast.makeText(MainActivity.this, "Please complete all fields!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if(flag == null || !flag){
                    flag = true;

                    //disable relevant buttons
                    disableOrEnableButtons(false, true, false, false);

                    PatientInfo patientInfo = new PatientInfo(widgetPatientName.getText().toString(),
                            widgetPatientAge.getText().toString(),
                            widgetPatientID.getText().toString(), isMale);
                    TABLE = patientInfo.table_name;
                    TABLE = TABLE.replace(" ", "_");
                    Toast.makeText(MainActivity.this, "Table Name: " + TABLE, Toast.LENGTH_SHORT).show();

                    db.beginTransaction();
                    try {
                        db.execSQL("create table if not exists " + TABLE + " ("
                                + " created_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
                                + " x float, "
                                + " y float,"
                                + " z float"
                                + " ); ");
                        db.setTransactionSuccessful();
                    }
                    catch (SQLiteException e) {
                        Toast.makeText(MainActivity.this, "Some issue in creating: Table Name: " +
                                TABLE, Toast.LENGTH_SHORT).show();
                    } finally {
                        db.endTransaction();
                    }
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(AccelerometerService.INTENT_ACCELEROMETER_DATA);
                    registerReceiver(accelerometerReceiver, intentFilter);
                    if(!threadStartedFlag) {
                        movingGraph.start();
                        threadStartedFlag = true;
                    }
                }
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                flag = false;
                for (int i = 0; i < valueArraySize; i++) {
                    valuesx[i] = 0;
                    valuesy[i] = 0;
                    valuesz[i] = 0;
                }
                g.invalidate();
                g.setValues(valuesx, valuesy, valuesz);
                //disable relevant buttons
                disableOrEnableButtons(true, false, true, true);

                try {
                    unregisterReceiver(accelerometerReceiver);
                }catch (Exception e)    {}
            }
        });

        buttonUpload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Toast.makeText(Assignment2.this, "Upload Begins", Toast.LENGTH_SHORT).show();
                disableAllButtons();
                uploadFileToServer(DATABASE_LOCATION, SERVER_LOCATION + "UploadToServer.php",
                        DATABASE_NAME);
            }
        });

        buttonDownload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (widgetPatientName.getText().toString().matches("") ||
                        widgetPatientAge.getText().toString().matches("") ||
                        widgetPatientID.getText().toString().matches(""))    {
                    Toast.makeText(MainActivity.this, "Please fill all fields to show last 10 " +
                                    "records and press DOWNLOAD again", Toast.LENGTH_LONG).show();
                } else {
                    disableAllButtons();
                    downloadFileFromServer(SERVER_LOCATION + "group11", DOWNLOAD_PATH,
                            DATABASE_NAME);
                }
            }
        });
    }

    private void setGraphUI() {
        String[] hlabels= new String[valueArraySize];
        for (int i = 0; i < valueArraySize; i++) {
            int temp = (-4)*i + 18;         //to get in the range of 20 to -18
            hlabels[i]=String.valueOf(temp);
        }

        String[] vlabels= new String[valueArraySize];
        for (int i = 0; i < valueArraySize; i++){
            vlabels[i]=String.valueOf(i);
        }

        valuesx = new float[valueArraySize];
        valuesy = new float[valueArraySize];
        valuesz = new float[valueArraySize];
        g = new GraphView(MainActivity.this, valuesx, valuesy, valuesz, "Health Monitor 11",
                vlabels, hlabels, GraphView.LINE);
        graph = (LinearLayout)findViewById(R.id.graphll);
        graph.addView(g);

        movingGraph = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int k = 0;
                    while(true){
                        while(flag) {
                            k++;
                            Message msg = threadHandle.obtainMessage(1,Integer.toString(k));
                            threadHandle.sendMessage(msg);
                            Thread.sleep(threadSleepTime);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getDataFromDatabase() {
        String query = "SELECT  * FROM " + TABLE + " ORDER BY created_at desc LIMIT 10;";
        Cursor cursor = null;
        int i = valueArraySize - 1;
        db.beginTransaction();
        try {
            cursor = db.rawQuery(query, null);
            db.setTransactionSuccessful(); //commit your changes
        } catch (Exception e) {
            Log.d("exp-getDataFromDatabase",e.getMessage());
        } finally {
            db.endTransaction();
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    valuesx[i] = Float.parseFloat(cursor.getString(1));
                    valuesy[i] = Float.parseFloat(cursor.getString(2));
                    valuesz[i] = Float.parseFloat(cursor.getString(3));
                    g.invalidate();
                    g.setValues(valuesx, valuesy, valuesz);
                    i--;
                } while (cursor.moveToNext() && i >= 0);
            }
            Toast.makeText(this.getApplicationContext(),"Graph plotted", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e)    {
            if(downloadButtonPressed) {
                Toast.makeText(MainActivity.this, "Data not found for this person",
                        Toast.LENGTH_LONG).show();
                g.invalidate();
                g.setValues(new float[10], new float[10], new float[10]);
                downloadButtonPressed = false;
            }
        }
    }

    private static boolean createDownloadedDB(String path) {
        boolean ret = true;
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("createDownloadedDB :: ", "Problem creating the path " + path);
                ret = false;
            }
        }
        return ret;
    }

    private class AccelerometerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            float x = intent.getFloatExtra("X", 0.0f);
            float y = intent.getFloatExtra("Y", 0.0f);
            float z = intent.getFloatExtra("Z", 0.0f);
            db.beginTransaction();
            try {

                db.execSQL("insert into " + TABLE + " (x,y,z) values ('" + x + "', '" + y + "','"
                        + z + "' );");
                db.setTransactionSuccessful();
            }
            catch (SQLiteException e) {
                Log.d("exp-accelerometer",e.getMessage());
            }
            finally {
                db.endTransaction();
            }
            Log.d("accelerometerReceiver", "X - "+ Float.toString(x) + " | Y - "+ Float.toString(y)
                    + " | Z - "+ Float.toString(z));
        }

    }

    private void downloadFileFromServer(final String sourceServer, String dest, String fileName) {
        final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
        downloadTask.execute(sourceServer, dest, fileName);
    }

    private void uploadFileToServer(final String sourceFileUri, String serverLocation, String fileName) {
        final UploadTask uploadTask = new UploadTask(MainActivity.this);
        uploadTask.execute(sourceFileUri, serverLocation, fileName);
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private Context context;
        //private PowerManager.WakeLock mWakeLock;

        DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {return null;}
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
            } };

            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                URL url = new URL(params[0]);
//                connection = (HttpsURLConnection) url.openConnection();
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode() + " " +
                            connection.getResponseMessage();
                }
                if (!createDownloadedDB(params[1])) {
                    return "Failed to create download path: " + params[1];
                }
                // display download percentage or -1
                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                output = new FileOutputStream(params[1]);
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException e) {
                    Log.d("exp-AfterDownload","Check here");
                }
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                Toast.makeText(context,"Download error: "+result, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context,"Download Complete", Toast.LENGTH_SHORT).show();
                db = SQLiteDatabase.openOrCreateDatabase(DATABASE_LOCATION, null);
                PatientInfo patientInfo = new PatientInfo(widgetPatientName.getText().toString(),
                        widgetPatientAge.getText().toString(), widgetPatientID.getText().toString(),
                        isMale);
                TABLE = patientInfo.table_name;
                TABLE = TABLE.replace(" ", "_");
                downloadButtonPressed = true;
                getDataFromDatabase();
            }
            disableOrEnableButtons(true, false, true, true);
        }
    }

    private class UploadTask extends AsyncTask<String, Integer, String> {
        private Context context;
        //private PowerManager.WakeLock mWakeLock;

        UploadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            //referred from: http://androidexample.com/Upload_File_To_Server_-_Android_Example/index.php?view=article_discription&aid=83&aaid=106
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024 * 1024;
            File sourceFile = new File(params[0]);
            int serverResponseCode=0;

            if (!sourceFile.isFile()) {
                Log.d("D", "file to upload not found");
                Toast.makeText(MainActivity.this, "File to upload not found. Please enter " +
                        "atleast one person's data", Toast.LENGTH_SHORT).show();
            }
            else {
                try {
                    FileInputStream fileInputStream = new FileInputStream(sourceFile);
                    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {return null;}
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
                    };


                    try {
                        SSLContext sc = SSLContext.getInstance("SSL");
                        sc.init(null, trustAllCerts, new SecureRandom());
                        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                    } catch (Exception e) {
                        Log.d("D", "SSL problem");
                    }
                    URL url = new URL(params[1]);
//                    conn = (HttpsURLConnection) url.openConnection();
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" +
                            boundary);
                    conn.setRequestProperty("uploaded_file", params[2]);

                    dos = new DataOutputStream(conn.getOutputStream());
                    Log.d("Upload", "Uploading...");
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";" +
                            "filename=\"" + params[2] + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);
                    bytesAvailable = fileInputStream.available();
                    Log.d("Upload", "Size of data: "+bytesAvailable);
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();

                    Log.i("Upload", "Code: " + serverResponseCode + " . Message: " +
                            serverResponseMessage);

                    fileInputStream.close();
                    dos.flush();
                    dos.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                Toast.makeText(context,"Upload error: "+result, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context,"Database uploaded", Toast.LENGTH_SHORT).show();
            }
            disableOrEnableButtons(true, false, true, true);
        }
    }

    private void disableAllButtons()
    {
        disableOrEnableButtons(false,false,false,false);
    }

    private void enableAllButtons()
    {
        disableOrEnableButtons(true,true,true,true);
    }

    private void disableOrEnableButtons(boolean run,boolean stop,boolean upload,boolean download)
    {
        buttonRun.setEnabled(run);
        buttonStop.setEnabled(stop);
        buttonUpload.setEnabled(upload);
        buttonDownload.setEnabled(download);
    }
}