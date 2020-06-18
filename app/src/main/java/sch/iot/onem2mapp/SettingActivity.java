package sch.iot.onem2mapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingActivity extends AppCompatActivity implements Button.OnClickListener {

    //설정 정보 저장용
    SharedPreferences.Editor preEditor;
    SharedPreferences pref;

    EditText rpi_address_edit;
    EditText ae_name_edit;

    String rpi_address;
    String ae_name;

    Button confirm_address;
    Button confirm_ae;

    Communication info = new Communication();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        rpi_address_edit = findViewById(R.id.rpi_address_edit_text);
        ae_name_edit = findViewById(R.id.ae_name_edit_text);

        preEditor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
        pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());


        confirm_address = findViewById(R.id.rpi_address_confirm_button);
        confirm_ae = findViewById(R.id.ae_name_confirm_button);

        confirm_address.setOnClickListener(this);
        confirm_ae.setOnClickListener(this);

        rpi_address_edit.setText(pref.getString("rpi_address", rpi_address));
        ae_name_edit.setText(pref.getString("ae_name", ae_name));

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rpi_address_confirm_button:
                rpi_address = rpi_address_edit.getText().toString();
                info.rpi_address = rpi_address;
                preEditor.putString("rpi_address",rpi_address);
                preEditor.apply();
                Toast.makeText(getApplicationContext(), "변경되었습니다!", Toast.LENGTH_LONG).show();
                break;
            case R.id.ae_name_confirm_button:
                ae_name = ae_name_edit.getText().toString();
                info.ae_name = ae_name;
                preEditor.putString("ae_name",ae_name);
                preEditor.apply();
                Toast.makeText(getApplicationContext(), "변경되었습니다!", Toast.LENGTH_LONG).show();
                break;
        }
    }
}