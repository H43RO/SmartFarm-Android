package sch.iot.onem2mapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;

public class GrowMonitorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grow_monitor);

        Button down_button = findViewById(R.id.download);
        down_button.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                getFtpImage();
            }
        });
    }

    private void getFtpImage(){
        try{
            FTPClient mFtp = new FTPClient();
            mFtp.connect("192.168.0.246", 21);
            mFtp.login("pi","rlaguswns5");
            mFtp.setFileType(FTP.BINARY_FILE_TYPE);
            mFtp.enterLocalPassiveMode();

            String remote = "/image.jpg";
            File downloadFile = new File("/sdcard/GrowUp/");

            FileOutputStream local = new FileOutputStream(downloadFile);
            boolean aRtn = mFtp.retrieveFile(remote, local); //파일을 성공적으로 받으면 true
            Log.d("FTP_Test", Boolean.toString(aRtn));
            local.close();
            mFtp.disconnect();

        }catch (SocketException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}