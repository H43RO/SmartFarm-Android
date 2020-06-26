package sch.iot.onem2mapp;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*  File(Data) Format & Logic
        - 날짜별로 1회 촬영
        - YYYYMMDD.jpg 로 저장 ex) 20200616.jpg
        - Android 단에서 해당 날짜 파일 존재하지 않을 시 동기화 함 (FTP Client 통해서 사진 다운드)
        - 파일명 (날짜순)으로 정렬하여 ArrayList<String>에 파일 경로를 넣어서 RecyclerView Adpater 연결
 */

public class GrowMonitorActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private ConnectFTP ConnectFTP = new ConnectFTP();
    private ArrayList<String> data = new ArrayList<String>();
    private SimpleDateFormat today = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
    private String check_date = today.format(new Date()); //초기엔 오늘 날짜 들어있음 (데이터셋 생성 위한 기준 == 오늘의 날짜)
    private File checkFile = new File(Environment.getExternalStorageDirectory() + "/GrowUpData/", check_date + ".jpg");

    private String currentPath;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ImageView searchData;

    final String TAG = "Activity FTP";

    private String newFilePath = Environment.getExternalStorageDirectory() + "/GrowUpData";
    private File file = new File(newFilePath);

    //다운이 다 되면 RecyclerView를 뿌릴 수 있도록 상태변수를 둠
    private Boolean download_flag = false;

    private String pickedDate;

    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
    String now = date.format(new Date());

    //오늘
    int now_year = Integer.parseInt(now.substring(0, 4));
    int now_month = Integer.parseInt(now.substring(4, 6));
    int now_day = Integer.parseInt(now.substring(6, 8));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grow_monitor);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        searchData = findViewById(R.id.search_data);

        final RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(this) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };

        DatePickerDialog.OnDateSetListener date_pick = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                pickedDate = String.format("%d%02d%02d", year, month + 1, dayOfMonth);

                int index = adapter.getItemCount() - (Integer.parseInt(pickedDate) - Integer.parseInt(data.get(adapter.getItemCount() - 1))) - 1;
                Log.d("index_check", String.valueOf(index));
                smoothScroller.setTargetPosition(index);
                layoutManager.startSmoothScroll(smoothScroller);
            }
        };

        //FTP 파일 다운로더 클래스 생성
        DownloadFileTask download = new DownloadFileTask();

        //오늘의 파일 명 구하기
        String today_filename = now + ".jpg";
        File todayFile = new File(Environment.getExternalStorageDirectory() + "/GrowUpData/", today_filename);

        if (!todayFile.exists()) {
            download.execute();
        } else {
            while (checkFile.exists()) {
                data.add(check_date);
                //날짜를 하나씩 줄이면서 데이터 추가함
                int temp = Integer.parseInt(check_date);
                temp--;
                check_date = String.valueOf(temp);
                checkFile = new File(Environment.getExternalStorageDirectory() + "/GrowUpData/", check_date + ".jpg");
            }

            //데이터셋 완성 후 Adapter에 연결 및 ViewHolder Binding
            adapter = new RecyclerAdapter(data);
            recyclerView.setAdapter(adapter);

            int first_year = Integer.parseInt(data.get(data.size() - 1).substring(0, 4));
            int first_month = Integer.parseInt(data.get(data.size() - 1).substring(4, 6));
            int first_day = Integer.parseInt(data.get(data.size() - 1).substring(6, 8));

            //오늘 날짜로 DatePickerDialog 생성
            final DatePickerDialog dialog = new DatePickerDialog(this, R.style.DatePicker, date_pick, now_year, now_month, now_day);

            Calendar minDate = Calendar.getInstance();
            Calendar maxDate = Calendar.getInstance();
            minDate.set(first_year, first_month - 1, first_day);
            maxDate.set(now_year, now_month - 1, now_day);

            dialog.getDatePicker().setMinDate(minDate.getTime().getTime());
            dialog.getDatePicker().setMaxDate(maxDate.getTime().getTime());

            searchData.setOnClickListener(new ImageView.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.show();
                }
            });
        }

    }

    //실행해도 만약 라즈베리파이에서 데이터 없으면 그냥 넘어가게 해놓음
    //Download 시퀀스가 끝나면 안정적으로 RecyclerView Binding 함 (onPostExecute() 동작)
    private class DownloadFileTask extends AsyncTask<String, Void, Void> {
        //로딩중 다이얼로그
        ProgressDialog asyncDialog = new ProgressDialog(
                GrowMonitorActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //다이얼로그 띄움
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("서버와 동기화 중입니다");

            // show dialog
            asyncDialog.show();
        }

        @Override
        protected Void doInBackground(String... strings) {
            boolean status = false;
            String host = "192.168.0.8";
            String username = "pi";
            String password = "raspberry";
            status = ConnectFTP.ftpConnect(host, username, password, 21);

            if (status == true) {
                Log.d(TAG, "FTP Connection 성공");
            } else {
                Log.d(TAG, "FTP Connection 실패");
            }

            //로드해야할 파일명 정의
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

            //라즈베리파이 서버에 있는 신규 이미지 다운로드
            ConnectFTP.ftpDownloadFile(currentPath + "/raspicam_example_without_opencv/build//" + today_filename, newFilePath);
            ConnectFTP.ftpDisconnect(); //CCTV Activity에서 RTSP 통신해줘야하기 때문에 FTP Close

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            asyncDialog.dismiss();
            super.onPostExecute(aVoid);

            //서버와 데이터 동기화를 마치면, RecyclerView Binding을 위한 데이터셋 생성
            //오늘 날짜로부터 하루씩 감소하며 데이터가 존재하지 않을 때까지 ArrayList 추가
            while (checkFile.exists()) {
                data.add(check_date);
                //날짜를 하나씩 줄이면서 데이터 추가함
                int temp = Integer.parseInt(check_date);
                temp--;
                check_date = String.valueOf(temp);
                checkFile = new File(Environment.getExternalStorageDirectory() + "/GrowUpData/", check_date + ".jpg");
            }

            //데이터셋 완성 후 Adapter에 연결 및 ViewHolder Binding
            adapter = new RecyclerAdapter(data);
            recyclerView.setAdapter(adapter);
        }
    }

    //생장 데이터를 시각화하기위한 RecyclerView Adapter
    public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
        private ArrayList<String> grow_image_date;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public View view;

            public ViewHolder(View v) {
                super(v);
                view = v;
            }
        }

        //Constructor
        public RecyclerAdapter(ArrayList<String> grow_image) {
            this.grow_image_date = grow_image;
        }

        //ViewHolder에 item view 추가
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.grow_data_item, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView title = holder.view.findViewById(R.id.title_date);
            ImageView image = holder.view.findViewById(R.id.image);

            //데이터셋에는 이미지의 날짜 들어있음
            String imagePath = Environment.getExternalStorageDirectory() + "/GrowUpData/" + grow_image_date.get(position) + ".jpg";
            File file = new File(imagePath); //해당 날짜 이미지 파일 생성

            //Using Glide for Image RecyclerView
            Glide.with(getApplicationContext())
                    .load(file)
                    .thumbnail(0.1f)
                    .override(2000, 1500)
                    .into(image);

            if (!file.exists()) {
                title.setText("데이터가 존재하지 않습니다");
            } else {
                String year = grow_image_date.get(position).substring(0, 4);
                String month = grow_image_date.get(position).substring(4, 6);
                String day = grow_image_date.get(position).substring(6, 8);

                String date = year + "년 " + month + "월 " + day + "일";
                title.setText(date);
            }
        }

        @Override
        public int getItemCount() {
            return grow_image_date.size();
        }
    }

}