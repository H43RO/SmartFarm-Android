package sch.iot.onem2mapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class GrowMonitorActivity extends AppCompatActivity {
    private ConnectFTP ConnectFTP = new ConnectFTP();
    final String TAG = "Activity FTP";
    String currentPath;
    ImageView imageView;
    public List<Bitmap> image_list;

    String newFilePath = Environment.getExternalStorageDirectory() + "/GrowUpData";
    File file = new File(newFilePath);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grow_monitor);

        imageView = findViewById(R.id.imageView);
        DownloadFileTask down = new DownloadFileTask();
        down.execute();
    }

    private class DownloadFileTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            boolean status = false;
            String host = "192.168.0.246";
            String username = "pi";
            String password = "rlaguswns5";
            status = ConnectFTP.ftpConnect(host, username, password, 21);
            if (status == true) {
                Log.d(TAG, "Connection 성공");
            } else {
                Log.d(TAG, "Connection 실패");
            }
            currentPath = ConnectFTP.ftpGetDirectory();


            newFilePath += "/raspi5.jpg";
            try {
                if(!file.exists()){
                    file.mkdir();
                }
                file = new File(newFilePath);
                file.createNewFile();
            } catch (Exception e) {
                Log.d(TAG, "실패");
            }

            ConnectFTP.ftpDownloadFile(currentPath + "/2020-06-11-081018_1920x1080_scrot.png", newFilePath);
            ConnectFTP.ftpDisconnect(); //CCTV Activity에서 RTSP 통신해줘야하기 때문에 FTP Close함

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(getApplicationContext(), "다운 성공", Toast.LENGTH_LONG).show();

            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Log.d("test_img",file.getAbsolutePath());
            imageView.setImageBitmap(bitmap);
        }
    }
}