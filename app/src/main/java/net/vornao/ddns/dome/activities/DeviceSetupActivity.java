package net.vornao.ddns.dome.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.shared.Const;

import java.util.Objects;

public class DeviceSetupActivity extends AppCompatActivity {

    private int currentState = 0;

    private  TextView step0;
    private  TextView step1;
    private  TextView step2;
    private  TextView step3;
    private  EditText ssid;
    private  EditText pass;

    private Button buttonNext;
    private Button buttonConf;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_device_layout);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Device setup");

        step0 = findViewById(R.id.step0);
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);

        ssid  = findViewById(R.id.wifiSSID);
        pass  = findViewById(R.id.wifipass);

        buttonNext = findViewById(R.id.buttonNext);
        buttonConf = findViewById(R.id.buttonConfigure);

        buttonNext.setOnClickListener(view -> {
            currentState++;
            changeView(currentState);
        });

        buttonConf.setOnClickListener(view -> sendConfiguration());

    }

    //fancy text change when next button pressed
    private void changeView(int state){
        switch (state){
            case 1:
                step0.setVisibility(View.GONE);
                step1.setVisibility(View.VISIBLE);
                break;
            case 2:
                step1.setVisibility(View.GONE);
                step2.setVisibility(View.VISIBLE);
                ssid.setVisibility(View.VISIBLE);
                pass.setVisibility(View.VISIBLE);
                break;
            case 3:
                step2.setVisibility(View.GONE);
                step3.setVisibility(View.VISIBLE);
                ssid.setVisibility(View.GONE);
                pass.setVisibility(View.GONE);
                buttonConf.setVisibility(View.VISIBLE);
                buttonNext.setVisibility(View.GONE);
                break;
            default:
                break;

        }
    }

    private void sendConfiguration(){

        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String topic = pm.getString(Const.MQTT_TOPIC_C, "");
        String name  = ssid.getText().toString();
        String pwd   = pass.getText().toString();

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String urlConfig = String.format(
                // cleartraffic allowed only for this domain on network preferences
                "http://192.168.1.1?ssid=%s&password=%s&home-uuid=%s",  // fixed uri to arduino board
                name,
                pwd,
                topic);
        StringRequest request = new StringRequest(Request.Method.GET, urlConfig,
            response -> {
                Log.d("DEVCONF", "SUCCESS");
                AlertDialog.Builder builder = new AlertDialog.Builder(DeviceSetupActivity.this);
                builder.setTitle(R.string.success_msg)
                        .setMessage(R.string.success_info)
                        .setNeutralButton(R.string.great_button, null)
                        .show();
        },
        error -> {
            Log.d("DEVCONF", "msg: " + error.getMessage());
            AlertDialog.Builder builder = new AlertDialog.Builder(DeviceSetupActivity.this);
            builder.setTitle(R.string.configuration_failed)
                    .setMessage(R.string.generic_connection_error_message)
                    .setNeutralButton(R.string.try_again, null)
                    .show();

        });

        queue.add(request);
    }

}
