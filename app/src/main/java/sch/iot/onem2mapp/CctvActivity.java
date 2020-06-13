package sch.iot.onem2mapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class CctvActivity extends AppCompatActivity implements Button.OnClickListener {
    public String src;
    public TextView led_on_off;
    public CardView capture_card;
    public CardView report_card;
    public CardView setting_card;

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


        WebView webView;
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