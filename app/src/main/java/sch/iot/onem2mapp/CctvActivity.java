package sch.iot.onem2mapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CctvActivity extends AppCompatActivity implements Button.OnClickListener {
    public String src;
    public TextView led_on_off;
    public CardView capture_card;
    public CardView report_card;
    public CardView setting_card;
    public WebView webView;

    //화면 캡쳐하기
    public File ScreenShot(View view){
        view.setDrawingCacheEnabled(true);  //화면에 뿌릴때 캐시를 사용하게 한다

        Bitmap screenBitmap = view.getDrawingCache();   //캐시를 비트맵으로 변환

        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);

        String now = date.format(new Date()) + "_" + time.format(new Date());
        String filename = "farm_" + now + ".jpg";
        File file = new File(Environment.getExternalStorageDirectory()+"/Pictures", filename);
        FileOutputStream os = null;
        try{
            os = new FileOutputStream(file);
            screenBitmap.compress(Bitmap.CompressFormat.JPEG, 200, os);   //비트맵을 PNG파일로 변환
            os.close();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }

        view.setDrawingCacheEnabled(false);
        return file;
    }


    @Override
    public void onClick(View v) {
        led_on_off = findViewById(R.id.led_on_off_text);

        switch (v.getId()){
            case R.id.ledOnButton:
                if(((ToggleButton) v).isChecked()){
                    led_on_off.setText("LED ON");
                    MainActivity.ControlRequest redOn = new MainActivity.ControlRequest("1");
                    redOn.setReceiver(new MainActivity.IReceived() {
                        @Override
                        public void getResponseBody(String msg) {

                        }
                    });
                    redOn.start();

                    MainActivity.ControlRequest greenOn = new MainActivity.ControlRequest("3");
                    greenOn.setReceiver(new MainActivity.IReceived() {
                        @Override
                        public void getResponseBody(String msg) {

                        }
                    });
                    greenOn.start();

                    MainActivity.ControlRequest blueOn = new MainActivity.ControlRequest("5");
                    blueOn.setReceiver(new MainActivity.IReceived() {
                        @Override
                        public void getResponseBody(String msg) {

                        }
                    });
                    blueOn.start();
                }else{
                    led_on_off.setText("LED OFF");
                    MainActivity.ControlRequest redOff = new MainActivity.ControlRequest("2");
                    redOff.setReceiver(new MainActivity.IReceived() {
                        @Override
                        public void getResponseBody(String msg) {

                        }
                    });
                    redOff.start();

                    MainActivity.ControlRequest greenOff = new MainActivity.ControlRequest("4");
                    greenOff.setReceiver(new MainActivity.IReceived() {
                        @Override
                        public void getResponseBody(String msg) {

                        }
                    });
                    greenOff.start();

                    MainActivity.ControlRequest blueOff = new MainActivity.ControlRequest("6");
                    blueOff.setReceiver(new MainActivity.IReceived() {
                        @Override
                        public void getResponseBody(String msg) {

                        }
                    });
                    blueOff.start();
                }
                break;

            case R.id.capture_card:

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d("screen_test","권한 부여 안됨");
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1004);
                }else{
                    Log.d("screen_test","권한 부여");
                    File screenShot = ScreenShot(webView);
                    if(screenShot!=null){
                        //갤러리에 추가
                        Log.d("screen_test","캡쳐성공");
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(screenShot)));
                        Toast.makeText(this,"CCTV가 캡쳐되었습니다!", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(this,"캡쳐를 실패했습니다", Toast.LENGTH_LONG).show();
                    }
                }

                break;

            case R.id.report_card:
                Intent call = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
                startActivity(call);
                break;

            case R.id.setting_card:
                Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
                startActivity(intent);
                break;
        }
    }

    public ToggleButton led_toggle;

    @Override
    protected void onResume() {
        super.onResume();
        src = MainActivity.info.rpi_address;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cctv);

        led_toggle = findViewById(R.id.ledOnButton);
        led_toggle.setOnClickListener(this);

        capture_card =findViewById(R.id.capture_card);
        capture_card.setOnClickListener(this);

        report_card = findViewById(R.id.report_card);
        report_card.setOnClickListener(this);

        setting_card = findViewById(R.id.setting_card);
        setting_card.setOnClickListener(this);


        WebSettings webSettings;

        webView = (WebView)findViewById(R.id.cctv);
        webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        src = MainActivity.info.rpi_address;

        webView.loadData("<html><head><style type='text/css'>body{margin:auto auto;text-align:center;} " +
                        "img{width:100%25;} div{overflow: hidden;} </style></head>" +
                        "<body><div><img src='"+ src +"'/></div></body></html>" ,
                "text/html",  "UTF-8");

    }
}