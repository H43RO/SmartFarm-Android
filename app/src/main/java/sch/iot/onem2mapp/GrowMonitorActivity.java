package sch.iot.onem2mapp;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/*  File Format로 & Logic
        - 날짜별로 1회 촬영
        - YYYYMMDD.jpg 로 저장 ex) 20200616.jpg
        - Android 단에서 해당 날짜 파일 존재하지 않을 시 동기화 함 (FTP Client 통해서 사진 다운드)
        - 파일명 (날짜순)으로 정렬하여 ArrayList<String>에 파일 경로를 넣어서 RecyclerView Adpater 연결
 */

public class GrowMonitorActivity extends AppCompatActivity {
    private ConnectFTP ConnectFTP = new ConnectFTP();
    final String TAG = "Activity FTP";
    String currentPath;
    ImageView imageView;
    ImageView imageView2;

    RecyclerView recyclerView = findViewById(R.id.recycler_view);
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager layoutManager;

    public ProgressDialog progressDialog;
    public ArrayList<String> photo_path; //갤러리에 있는 모든 사진들에 대한 Path를 저장하여 Adapting 함

    String newFilePath = Environment.getExternalStorageDirectory() + "/GrowUpData";
    File file = new File(newFilePath);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grow_monitor);

        //파일 다운로더 클래스
        DownloadFileTask download = new DownloadFileTask();

        //오늘의 파일 명 구하기
        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        String now = date.format(new Date());
        String today_filename = now + ".jpg";

        File todayFile = new File(Environment.getExternalStorageDirectory() + "/GrowUpData/", today_filename);

        if (!todayFile.exists()) {
            download.execute(); //라즈베리파이 서버에 없으면 그냥 넘어가게 해놓음
        }

        try {
            String newFilePath = Environment.getExternalStorageDirectory() + "/GrowUpData/raspi5.jpg";
            File file2 = new File(newFilePath);

            if (file2.exists()) {
                Bitmap bitmap2 = BitmapFactory.decodeFile(file2.getAbsolutePath());
                Log.d("test_img_saving", file2.getAbsolutePath());
                imageView2.setImageBitmap(bitmap2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        ArrayList<String> data = new ArrayList<String>();
        data.add("20200612");
        data.add("20200613");
        data.add("20200614");
        data.add("20200615");

        adapter = new RecyclerAdapter(data);
        recyclerView.setAdapter(adapter);
    }

    private class DownloadFileTask extends AsyncTask<String, Void, Void> {

        ProgressDialog asyncDialog = new ProgressDialog(
                GrowMonitorActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("서버와 동기화 중입니다");

            // show dialog
            asyncDialog.show();
        }

        @Override
        protected Void doInBackground(String... strings) {
            boolean status = false;
            String host = "114.71.220.108";
            String username = "pi";
            String password = "rlaguswns5";
            status = ConnectFTP.ftpConnect(host, username, password, 21);

            if (status == true) {
                Log.d(TAG, "Connection 성공");
            } else {
                Log.d(TAG, "Connection 실패");
            }

            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
            String now = date.format(new Date());
            String today_filename = now + ".jpg";

            currentPath = ConnectFTP.ftpGetDirectory();
            newFilePath += "/" + today_filename;

            try {
                if (!file.exists()) {
                    file.mkdir();
                }
                file = new File(newFilePath);
                file.createNewFile();
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.d(TAG, "실패");
            }

            ConnectFTP.ftpDownloadFile(currentPath + "/Pictures/" + today_filename, newFilePath);
            ConnectFTP.ftpDisconnect(); //CCTV Activity에서 RTSP 통신해줘야하기 때문에 FTP Close함

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            imageView.setImageBitmap(bitmap);
            asyncDialog.dismiss();
            super.onPostExecute(aVoid);
        }
    }

    public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
        private ArrayList<String> grow_image_date;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public View view;
            public ViewHolder(View v) {
                super(v);
                view = v;
            }
        }

        public RecyclerAdapter(ArrayList<String> grow_image) {
            grow_image = grow_image;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.grow_data_item, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView title = holder.view.findViewById(R.id.title_date);
            ImageView image = holder.view.findViewById(R.id.image);

            //DataSet에는 이미지의 날짜 들어있음

            String imagePath = Environment.getExternalStorageDirectory() + "/GrowUpData/" + grow_image_date.get(position) + ".jpg";

            File file = new File(imagePath);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            image.setImageBitmap(bitmap);

            String year = grow_image_date.get(position).substring(0,3);
            String month = grow_image_date.get(position).substring(4,5);
            String day = grow_image_date.get(position).substring(6,7);

            String date = year + "년 " + month + "월 " + day + "일";
            title.setText(date);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return grow_image_date.size();
        }
    }

}