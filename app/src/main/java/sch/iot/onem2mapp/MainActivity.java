package sch.iot.onem2mapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

import static sch.iot.onem2mapp.R.layout.activity_main;

public class MainActivity extends AppCompatActivity implements Button.OnClickListener, CompoundButton.OnCheckedChangeListener {

    //감시모드 여부 SharedPreference에 저장
    SharedPreferences.Editor preEditor;
    SharedPreferences pref;

    public Button btnRetrieve;
    public SwitchCompat Switch_MQTT;
    public CardView toCctv;
    public CardView toGrowUp;
    public ImageView toSetting;

    // added by J. Yun, SCH Univ.
    public TextView textDust;
    public TextView textPIR;
    public TextView textSound;
    public TextView textTemp;
    public TextView textHumid;
    public TextView security_status;

    public Handler handler;

    static Communication info = new Communication();

    private static CSEBase csebase = new CSEBase();
    private static AE ae = new AE();
    private static String TAG = "MainActivity";
    private String MQTTPort = "1883";

    private static String ServiceAEName = "sch20181512";
    private String MQTT_Req_Topic = "";
    private String MQTT_Resp_Topic = "";
    private MqttAndroidClient mqttClient = null;


    // Main
    public MainActivity() {
        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ServiceAEName = info.ae_name;
        Log.d("test_ae_create", ServiceAEName);
    }

    /* onCreate */
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(activity_main);

        ServiceAEName = info.ae_name;
        Log.d("test_ae_create", ServiceAEName);

        Switch_MQTT = findViewById(R.id.switch_mqtt);
        security_status = findViewById(R.id.security_status);
        toCctv = findViewById(R.id.cctv_card);
        toSetting = findViewById(R.id.to_setting);
        toGrowUp = findViewById(R.id.growup_card);

        textDust = findViewById(R.id.textDust);
        textTemp = findViewById(R.id.textTemp);
        textHumid = findViewById(R.id.textHumid);

        Switch_MQTT.setOnCheckedChangeListener(this);
        toCctv.setOnClickListener(this);
        toSetting.setOnClickListener(this);
        toGrowUp.setOnClickListener(this);

        preEditor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
        pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (pref.getString("checked", "no").equals("yes")) {
            Switch_MQTT.setChecked(true);
        } else {
            Switch_MQTT.setChecked(false);
        }

        // Create AE and Get AEID
        GetAEInfo();
        Timer timer = new Timer();

        TimerTask TT = new TimerTask() {
            @Override
            public void run() {
                RetrieveRequest req;
                req = new RetrieveRequest("dust");
                req.setReceiver(new IReceived() {
                    public void getResponseBody(final String msg) {
                        handler.post(new Runnable() {
                            public void run() {
                                String dust_text = getContainerContentXML(msg);
                                String[] dust = (dust_text.split(","));
                                int result = Integer.parseInt(dust[1]); //초미세먼지 (PM2.5)

                                if (0 < result && result <= 15) {
                                    textDust.setText("좋음");
                                } else if (15 < result && result <= 35) {
                                    textDust.setText("보통");
                                } else if (36 < result && result <= 75) {
                                    textDust.setText("나쁨");
                                } else if (75 < result) {
                                    textDust.setText("심각");
                                }
                            }
                        });
                    }
                });
                req.start();

                req = new RetrieveRequest("temp");
                req.setReceiver(new IReceived() {
                    public void getResponseBody(final String msg) {
                        handler.post(new Runnable() {
                            public void run() {
                                String temp_humid_text = getContainerContentXML(msg);
                                String[] temp_humid = (temp_humid_text.split(","));
                                double[] temp = new double[2];
                                int[] result = new int[2];

                                for (int i = 0; i < 2; i++) {
                                    temp[i] = Double.parseDouble(temp_humid[i]);
                                    result[i] = (int) temp[i];
                                }
                                textTemp.setText(result[0] + "°C");
                                textHumid.setText(result[1] + "%");
                            }
                        });
                    }
                });
                req.start();
            }
        };

        timer.schedule(TT, 0, 3000); //Timer 실행
    }

    /* AE Create for Androdi AE */
    public void GetAEInfo() {
        // You can put the IP address directly in code,
        // but also get it from EditText window
        // csebase.setInfo(Mobius_Address,"7579","Mobius","1883");
        csebase.setInfo("203.253.128.161", "7579", "Mobius", "1883");

        // AE Create for Android AE
        ae.setAppName("ncubeapp");
        aeCreateRequest aeCreate = new aeCreateRequest();
        aeCreate.setReceiver(new IReceived() {
            public void getResponseBody(final String msg) {
                handler.post(new Runnable() {
                    public void run() {
                        Log.d(TAG, "** AE Create ResponseCode[" + msg + "]");
                        if (Integer.parseInt(msg) == 201) {
                            MQTT_Req_Topic = "/oneM2M/req/Mobius2/" + ae.getAEid() + "_sub" + "/#";
                            MQTT_Resp_Topic = "/oneM2M/resp/Mobius2/" + ae.getAEid() + "_sub" + "/json";
                            Log.d(TAG, "ReqTopic[" + MQTT_Req_Topic + "]");
                            Log.d(TAG, "ResTopic[" + MQTT_Resp_Topic + "]");
                        } else { // If AE is Exist , GET AEID
                            aeRetrieveRequest aeRetrive = new aeRetrieveRequest();
                            aeRetrive.setReceiver(new IReceived() {
                                public void getResponseBody(final String resmsg) {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            Log.d(TAG, "** AE Retrive ResponseCode[" + resmsg + "]");
                                            MQTT_Req_Topic = "/oneM2M/req/Mobius2/" + ae.getAEid() + "_sub" + "/#";
                                            MQTT_Resp_Topic = "/oneM2M/resp/Mobius2/" + ae.getAEid() + "_sub" + "/json";
                                            Log.d(TAG, "ReqTopic[" + MQTT_Req_Topic + "]");
                                            Log.d(TAG, "ResTopic[" + MQTT_Resp_Topic + "]");
                                        }
                                    });
                                }
                            });
                            aeRetrive.start();
                        }
                    }
                });
            }
        });
        aeCreate.start();
    }


    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);

        if (isChecked) {
            security_status.setText("방범모드가 활성화 되었습니다");
            security_status.setTypeface(null, Typeface.BOLD);
            Log.d(TAG, "MQTT Create");
            MQTT_Create(true);

            if (Build.VERSION.SDK_INT >= 26) {
                getApplicationContext().startForegroundService(foreground);
            } else {
                getApplicationContext().startService(foreground);
            }
            preEditor.putString("checked", "yes");
            preEditor.apply();

        } else {
            stopService(foreground);
            security_status.setText("방범모드가 비활성화 되었습니다");
            security_status.setTypeface(null, Typeface.NORMAL);
            Log.d(TAG, "MQTT Close");
            MQTT_Create(false);

            preEditor.putString("checked", "false");
            preEditor.apply();
        }
    }

    /* MQTT Subscription */
    public void MQTT_Create(boolean mtqqStart) {
        if (mtqqStart && mqttClient == null) {
            /* Subscription Resource Create to Yellow Turtle */
            // added by J. Yun, SCH Univ.
            SubscribeResource subcribeResource = new SubscribeResource("pir");
            subcribeResource.setReceiver(new IReceived() {
                public void getResponseBody(final String msg) {
                }
            });
            subcribeResource.start();

            // added by J. Yun, SCH Univ.
            subcribeResource = new SubscribeResource("sound");
            subcribeResource.setReceiver(new IReceived() {
                public void getResponseBody(final String msg) {
                }
            });
            subcribeResource.start();

            /* MQTT Subscribe */
            mqttClient = new MqttAndroidClient(this.getApplicationContext(), "tcp://" + csebase.getHost() + ":" + csebase.getMQTTPort(), MqttClient.generateClientId());
            mqttClient.setCallback(mainMqttCallback);
            try {
                // added by J. Yun, SCH Univ.
                MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                mqttConnectOptions.setKeepAliveInterval(600);
                mqttConnectOptions.setCleanSession(false);


                IMqttToken token = mqttClient.connect(mqttConnectOptions);
//                IMqttToken token = mqttClient.connect();
                token.setActionCallback(mainIMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            /* MQTT unSubscribe or Client Close */
            mqttClient.setCallback(null);
            mqttClient.close();
            mqttClient = null;
        }
    }

    /* MQTT Listener */
    private IMqttActionListener mainIMqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d(TAG, "onSuccess");
            String payload = "";
            int mqttQos = 1; /* 0: NO QoS, 1: No Check , 2: Each Check */

            MqttMessage message = new MqttMessage(payload.getBytes());
            try {
                mqttClient.subscribe(MQTT_Req_Topic, mqttQos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.d(TAG, "onFailure");
        }
    };


    /* MQTT Broker Message Received */
    private MqttCallback mainMqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.d(TAG, "connectionLost");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);
            Log.d(TAG, "messageArrived");

            String cnt = getContainerName(message.toString());
            Log.d(TAG, "Received container name is " + cnt);
            //textViewData.setText(cnt);
            if (cnt.indexOf("pir") != -1) {
                //PIR 감지 시 foreground service 종료
                stopService(foreground);
                Switch_MQTT.setChecked(false);
                Log.d("detected_test","침입 감지");

                Intent intent = new Intent(getApplicationContext(), CctvActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);


                if (Build.VERSION.SDK_INT >= 26) {

                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    String id = "my_channel_01";
                    CharSequence name = "농장에 침입이 감지되었습니다!";
                    String description = "탭하여 CCTV 확인하기";

                    int importance = NotificationManager.IMPORTANCE_HIGH;

                    NotificationChannel mChannel = new NotificationChannel(id, name, importance);

                    mChannel.setDescription(description);
                    mChannel.enableLights(true);

                    mChannel.setLightColor(Color.RED);
                    mChannel.enableVibration(true);
                    mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

                    mNotificationManager.createNotificationChannel(mChannel);
                    mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    int notifyID = 1;
                    String CHANNEL_ID = "my_channel_01";
                    Notification notification = new Notification.Builder(getApplicationContext())
                            .setContentTitle("침입이 감지되었습니다!")
                            .setContentText("탭하여 CCTV 확인하기")
                            .setSmallIcon(R.drawable.alert)
                            .setChannelId(CHANNEL_ID)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .build();

                    mNotificationManager.notify(1, notification);

                } else {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "detected")
                            .setSmallIcon(R.drawable.alert)
                            .setContentTitle("침입이 감지되었습니다!")
                            .setContentText("탭하여 CCTV 확인하기")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(1004, builder.build());
                }


            } else if (cnt.indexOf("sound") != -1) {
            } else {
            }

            /* Json Type Response Parsing */
            String retrqi = MqttClientRequestParser.notificationJsonParse(message.toString());
            Log.d(TAG, "RQI[" + retrqi + "]");

            String responseMessage = MqttClientRequest.notificationResponse(retrqi);
            Log.d(TAG, "Recv OK ResMessage [" + responseMessage + "]");

            /* Make json for MQTT Response Message */
            MqttMessage res_message = new MqttMessage(responseMessage.getBytes());

            try {
                mqttClient.publish(MQTT_Resp_Topic, res_message);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "deliveryComplete");
        }

    };

    // Added by J. Yun, SCH Univ.
    private String getContainerName(String msg) {
        String cnt = "";
        try {
            JSONObject jsonObject = new JSONObject(msg);
            cnt = jsonObject.getJSONObject("pc").
                    getJSONObject("m2m:sgn").getString("sur");
            // Log.d(TAG, "Content is " + cnt);
        } catch (JSONException e) {
            Log.e(TAG, "JSONObject error!");
        }
        return cnt;
    }

    // Added by J. Yun, SCH Univ.
    private String getContainerContentJSON(String msg) {
        String con = "";
        try {
            JSONObject jsonObject = new JSONObject(msg);
            con = jsonObject.getJSONObject("pc").
                    getJSONObject("m2m:sgn").
                    getJSONObject("nev").
                    getJSONObject("rep").
                    getJSONObject("m2m:cin").
                    getString("con");
//            Log.d(TAG, "Content is " + con);
        } catch (JSONException e) {
            Log.e(TAG, "JSONObject error!");
        }
        return con;
    }

    // Added by J. Yun, SCH Univ.
    private String getContainerContentXML(String msg) {
        String con = "";
        try {
            XmlToJson xmlToJson = new XmlToJson.Builder(msg).build();
            JSONObject jsonObject = xmlToJson.toJson();
            con = jsonObject.getJSONObject("m2m:cin").getString("con");
//            Log.d(TAG, "Content is " + con);
        } catch (JSONException e) {
            Log.e(TAG, "JSONObject error!");
        }
        return con;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cctv_card: {
                Intent toCctv = new Intent(getApplicationContext(), CctvActivity.class);
                startActivity(toCctv);
                break;
            }

            case R.id.to_setting: {
                Intent toSetting = new Intent(getApplicationContext(), SettingActivity.class);
                startActivity(toSetting);
                break;
            }

            case R.id.growup_card: {
                Intent toGrowUp = new Intent(getApplicationContext(), GrowMonitorActivity.class);
                startActivity(toGrowUp);
                break;
            }

        }
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    /* Response callback Interface */
    public interface IReceived {
        void getResponseBody(String msg);
    }

    // Retrieve PIR and Sound Sensor, added by J. Yun, SCH Univ.
    class RetrieveRequest extends Thread {
        private final Logger LOG = Logger.getLogger(RetrieveRequest.class.getName());
        private IReceived receiver;
        //        private String ContainerName = "cnt-co2";
        private String ContainerName = "";


        public RetrieveRequest(String containerName) {
            this.ContainerName = containerName;
        }

        public RetrieveRequest() {
        }

        public void setReceiver(IReceived hanlder) {
            this.receiver = hanlder;
        }

        @Override
        public void run() {
            try {
                String sb = csebase.getServiceUrl() + "/" + ServiceAEName + "/" + ContainerName + "/" + "latest";

                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setDoOutput(false);

                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-Origin", ae.getAEid());
                conn.setRequestProperty("nmtype", "long");
                conn.connect();

                String strResp = "";
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String strLine = "";
                while ((strLine = in.readLine()) != null) {
                    strResp += strLine;
                }

                if (strResp != "") {
                    receiver.getResponseBody(strResp);
                }
                conn.disconnect();

            } catch (Exception exp) {
                LOG.log(Level.WARNING, exp.getMessage());
            }
        }
    }

    /* Request Control LED */
    public static class ControlRequest extends Thread {
        private final Logger LOG = Logger.getLogger(ControlRequest.class.getName());
        private IReceived receiver;
        //        private String container_name = "cnt-led";
        private String container_name = "ledm";


        public ContentInstanceObject contentinstance;

        public ControlRequest(String comm) {
            contentinstance = new ContentInstanceObject();
            contentinstance.setContent(comm);
        }

        public void setReceiver(IReceived hanlder) {
            this.receiver = hanlder;
        }

        @Override
        public void run() {
            try {
                String sb = csebase.getServiceUrl() + "/" + ServiceAEName + "/" + container_name;

                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(false);

                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("Content-Type", "application/vnd.onem2m-res+xml;ty=4");
                conn.setRequestProperty("locale", "ko");
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-Origin", ae.getAEid());

                String reqContent = contentinstance.makeXML();
                conn.setRequestProperty("Content-Length", String.valueOf(reqContent.length()));

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.write(reqContent.getBytes());
                dos.flush();
                dos.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String resp = "";
                String strLine = "";
                while ((strLine = in.readLine()) != null) {
                    resp += strLine;
                }
                if (resp != "") {
                    receiver.getResponseBody(resp);
                }
                conn.disconnect();

            } catch (Exception exp) {
                LOG.log(Level.SEVERE, exp.getMessage());
            }
        }
    }

    /* Request AE Creation */
    class aeCreateRequest extends Thread {
        private final Logger LOG = Logger.getLogger(aeCreateRequest.class.getName());
        String TAG = aeCreateRequest.class.getName();
        private IReceived receiver;
        int responseCode = 0;
        public ApplicationEntityObject applicationEntity;

        public void setReceiver(IReceived hanlder) {
            this.receiver = hanlder;
        }

        public aeCreateRequest() {
            applicationEntity = new ApplicationEntityObject();
            applicationEntity.setResourceName(ae.getappName());
            Log.d(TAG, ae.getappName() + "JJjj");
        }

        @Override
        public void run() {
            try {

                String sb = csebase.getServiceUrl();
                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(false);

                conn.setRequestProperty("Content-Type", "application/vnd.onem2m-res+xml;ty=2");
                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("locale", "ko");
                conn.setRequestProperty("X-M2M-Origin", "S" + ae.getappName());
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-NM", ae.getappName());

                String reqXml = applicationEntity.makeXML();
                conn.setRequestProperty("Content-Length", String.valueOf(reqXml.length()));

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.write(reqXml.getBytes());
                dos.flush();
                dos.close();

                responseCode = conn.getResponseCode();

                BufferedReader in = null;
                String aei = "";
                if (responseCode == 201) {
                    // Get AEID from Response Data
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String resp = "";
                    String strLine;
                    while ((strLine = in.readLine()) != null) {
                        resp += strLine;
                    }

                    ParseElementXml pxml = new ParseElementXml();
                    aei = pxml.GetElementXml(resp, "aei");
                    ae.setAEid(aei);
                    Log.d(TAG, "Create Get AEID[" + aei + "]");
                    in.close();
                }
                if (responseCode != 0) {
                    receiver.getResponseBody(Integer.toString(responseCode));
                }
                conn.disconnect();
            } catch (Exception exp) {
                LOG.log(Level.SEVERE, exp.getMessage());
            }

        }
    }

    /* Retrieve AE-ID */
    class aeRetrieveRequest extends Thread {
        private final Logger LOG = Logger.getLogger(aeCreateRequest.class.getName());
        private IReceived receiver;
        int responseCode = 0;

        public aeRetrieveRequest() {
        }

        public void setReceiver(IReceived hanlder) {
            this.receiver = hanlder;
        }

        @Override
        public void run() {
            try {
                String sb = csebase.getServiceUrl() + "/" + ae.getappName();
                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setDoOutput(false);

                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-Origin", "Sandoroid");
                conn.setRequestProperty("nmtype", "short");
                conn.connect();

                responseCode = conn.getResponseCode();

                BufferedReader in = null;
                String aei = "";
                if (responseCode == 200) {
                    // Get AEID from Response Data
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String resp = "";
                    String strLine;
                    while ((strLine = in.readLine()) != null) {
                        resp += strLine;
                    }

                    ParseElementXml pxml = new ParseElementXml();
                    aei = pxml.GetElementXml(resp, "aei");
                    ae.setAEid(aei);
                    //Log.d(TAG, "Retrieve Get AEID[" + aei + "]");
                    in.close();
                }
                if (responseCode != 0) {
                    receiver.getResponseBody(Integer.toString(responseCode));
                }
                conn.disconnect();
            } catch (Exception exp) {
                LOG.log(Level.SEVERE, exp.getMessage());
            }
        }
    }

    /* Subscribe Co2 Content Resource */
    class SubscribeResource extends Thread {
        private final Logger LOG = Logger.getLogger(SubscribeResource.class.getName());
        private IReceived receiver;
        //        private String container_name = "cnt-co2"; //change to control container name
        private String container_name; //change to control container name

        public ContentSubscribeObject subscribeInstance;

        public SubscribeResource(String containerName) {
            subscribeInstance = new ContentSubscribeObject();
            subscribeInstance.setUrl(csebase.getHost());
            subscribeInstance.setResourceName(ae.getAEid() + "_rn");
            subscribeInstance.setPath(ae.getAEid() + "_sub");
            subscribeInstance.setOrigin_id(ae.getAEid());

            // added by J. Yun, SCH Univ.
            this.container_name = containerName;
        }

        public void setReceiver(IReceived hanlder) {
            this.receiver = hanlder;
        }

        @Override
        public void run() {
            try {
                String sb = csebase.getServiceUrl() + "/" + ServiceAEName + "/" + container_name;

                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(false);

                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("Content-Type", "application/vnd.onem2m-res+xml; ty=23");
                conn.setRequestProperty("locale", "ko");
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-Origin", ae.getAEid());

                String reqmqttContent = subscribeInstance.makeXML();
                conn.setRequestProperty("Content-Length", String.valueOf(reqmqttContent.length()));

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.write(reqmqttContent.getBytes());
                dos.flush();
                dos.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String resp = "";
                String strLine = "";
                while ((strLine = in.readLine()) != null) {
                    resp += strLine;
                }

                if (resp != "") {
                    receiver.getResponseBody(resp);
                }
                conn.disconnect();

            } catch (Exception exp) {
                LOG.log(Level.SEVERE, exp.getMessage());
            }
        }
    }
}