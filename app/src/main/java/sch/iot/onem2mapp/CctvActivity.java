package sch.iot.onem2mapp;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class CctvActivity extends AppCompatActivity {
    public String src;

    @Override
    protected void onResume() {
        super.onResume();
        src = MainActivity.info.rpi_address;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cctv);

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