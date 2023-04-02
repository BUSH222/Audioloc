package com.example.audioloc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.Scanner;
import java.net.URL;
import java.net.HttpURLConnection;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;


public class MainActivity extends Activity implements View.OnClickListener{




    WifiManager wifiManager;
    WifiScanReceiver wifiReciever;
    WifiInfo wifiInfo;
    Map<String, String> BSSIDS = new HashMap<>();
    String currentfile;
    String response;
    Boolean ErrorInThread;
    private MediaPlayer mp;



    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button playbutton1 = findViewById(R.id.playbutton);
        playbutton1.setOnClickListener(this);
        Button stopbutton1 = findViewById(R.id.stopbutton);
        stopbutton1.setOnClickListener(this);
        Button infobutton1 = findViewById(R.id.infobutton);
        infobutton1.setOnClickListener(this);


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReciever = new WifiScanReceiver();
        wifiInfo = wifiManager.getConnectionInfo();
        ErrorInThread = Boolean.FALSE;


        BSSIDS = new HashMap<>();

        currentfile = "nothing.mp3";

    }

    Thread DoRequest = new Thread() {
        @SuppressLint("SetTextI18n")
        public void run() {
            try {
                EditText medit = findViewById(R.id.ipinput);
                URL url = new URL("http://" + medit.getText().toString() + "/analyse");
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("Content-Type", "application/json");
                JSONObject BSSIDS_TO_JSON = new JSONObject(BSSIDS);
                httpConn.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
                writer.write(String.valueOf(BSSIDS_TO_JSON));
                writer.flush();
                writer.close();
                httpConn.getOutputStream().close();
                InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                        ? httpConn.getInputStream()
                        : httpConn.getErrorStream();
                Scanner s = new Scanner(responseStream).useDelimiter("\\A");
                response = s.hasNext() ? s.next() : "";
                System.out.println(response);
                BSSIDS.clear();
                currentfile = response;

                runOnUiThread(() -> Toast.makeText(getApplicationContext(), currentfile, Toast.LENGTH_SHORT).show());



                mp.reset();
                AssetFileDescriptor afd;
                afd = getAssets().openFd(currentfile);

                mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                mp.prepare();
                mp.start();



            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ERROR", "REQUEST ERROR");
                ErrorInThread = Boolean.TRUE;

            }

        }

    };

    @SuppressLint({"SetTextI18n", "NonConstantResourceId"})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playbutton: {
                Button pb = findViewById(R.id.playbutton);
                pb.setEnabled(false);
                wifiManager.startScan();
                break;

            }


            case R.id.stopbutton:{
                Button pb = findViewById(R.id.playbutton);
                pb.setEnabled(true);
                try {
                if(mp.isPlaying())
                {
                    mp.stop();
                    Toast.makeText(getApplicationContext(), "Stopping...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Nothing is playing", Toast.LENGTH_SHORT).show();
                }} catch (java.lang.NullPointerException e) {
                    Toast.makeText(getApplicationContext(), "Nothing is playing", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.infobutton:{
                EditText medit = findViewById(R.id.ipinput);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + medit.getText().toString() + "/mobileinfo"));
                startActivity(browserIntent);
                break;
            }
        }
    }




    protected void onPause() {
        unregisterReceiver(wifiReciever);

        super.onPause();
    }

    protected void onResume() {
        registerReceiver(wifiReciever, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    class WifiScanReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = wifiManager.getScanResults();
            BSSIDS.clear();
            Log.d("THETAG", "bruh scan");
            for (int i = 0; i < wifiScanList.size(); i++) {
                String bssid =  wifiScanList.get(i).BSSID;
                int rssi =  wifiScanList.get(i).level;
                BSSIDS.put(bssid, String.valueOf(rssi));
            }
            EditText medit = findViewById(R.id.ipinput);
            if (!medit.getText().toString().trim().equals("") && !BSSIDS.isEmpty()) {
                mp = new MediaPlayer();
                new Thread(DoRequest).start();

                if (!(ErrorInThread == Boolean.FALSE || Objects.equals(currentfile, "nothing.mp3"))) {
                    Toast.makeText(getApplicationContext(), "Something went wrong. Check the ip address.", Toast.LENGTH_LONG).show();
                    ErrorInThread = Boolean.FALSE;
                }
            } else if (medit.getText().toString().trim().equals("")){
                Toast.makeText(getApplicationContext(), "The ip input field is blank", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "There are either no networks nearby or you are pressing play too quickly", Toast.LENGTH_LONG).show();
            }

            currentfile = "nothing.mp3";
            response = "nothing.mp3";
            BSSIDS.clear();
            Button pb = findViewById(R.id.playbutton);
            pb.setEnabled(false);


        }
    }
}