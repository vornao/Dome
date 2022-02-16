package net.vornao.ddns.dome.activities;

import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.devices.Thermostat;
import net.vornao.ddns.dome.fragments.BottomSheetFragment;
import net.vornao.ddns.dome.handler.DatabaseHelper;
import net.vornao.ddns.dome.shared.Const;

import java.util.Locale;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class ThermostatActivity extends AppCompatActivity {

    @Nullable
    private Thermostat thermostat;

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thermostat_actionbar_menu, menu);
        return true;

    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // action with ID action_refresh was selected
        if (item.getItemId() == R.id.action_details) {
            BottomSheetFragment b = new BottomSheetFragment();
            Bundle bundle = new Bundle();
            bundle.putString(Const.DEVICE_ID, String.valueOf(thermostat.getId()));
            bundle.putString(Const.MQTT_TOPIC_C, thermostat.getHome_id());
            b.setArguments(bundle);
            b.show(getSupportFragmentManager(), "bsfd_fragment");

        }
        return false;
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thermostat_activity);
        Button modeSelector = findViewById(R.id.mode_selector);
        modeSelector.setElevation((float)5);

        EditText deviceDescription = findViewById(R.id.thermostat_descr);
        EditText deviceName = findViewById(R.id.thermostat_name);
        TextView humValue   = findViewById(R.id.hum_value_tw);
        TextView tempValue  = findViewById(R.id.temp_val_tw);
        TextView tempSet    = findViewById(R.id.temp_set);

        CircularSeekBar circularSeekBar = findViewById(R.id.cs);

        Intent intent  = getIntent();

        int deviceID = intent.getIntExtra(Const.DEVICE_ID, 0);
        Log.d("THERMOSTAT", String.valueOf(deviceID));

        thermostat = (Thermostat)MainActivity.devices.get(deviceID);
        try {
            assert thermostat != null;
        }catch (AssertionError e){
            Snackbar.make(getWindow().getDecorView(), R.string.generic_connection_error_message, Snackbar.LENGTH_SHORT);
        }
        if(thermostat.getName() == null) deviceName.setText(R.string.thermostat);
        else deviceName.setText(thermostat.getName());

        if(thermostat.getDescription() == null) deviceDescription.setText(R.string.update_dev_descr);
        else deviceDescription.setText(thermostat.getDescription());

        deviceName.setImeActionLabel("Save", KeyEvent.KEYCODE_ENTER);
        deviceName.setImeOptions(EditorInfo.IME_ACTION_DONE);

        deviceDescription.setImeActionLabel("edit", KeyEvent.KEYCODE_ENTER);
        deviceDescription.setImeOptions(EditorInfo.IME_ACTION_DONE);


        deviceName.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == EditorInfo.IME_ACTION_DONE){
                Log.i("EDITORACTION", deviceName.getText().toString());
                updateDeviceName(deviceName.getText().toString());
                deviceName.setCursorVisible(false);
                deviceName.clearFocus();
                return false; // hide keyboard and cursor
            }
            return true;
        });

        deviceDescription.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == EditorInfo.IME_ACTION_DONE){
                Log.i("EDITORACTION", deviceDescription.getText().toString());
                updateDeviceDescription(deviceDescription.getText().toString());
                deviceDescription.setCursorVisible(false);
                deviceDescription.clearFocus();
                return false; //hide keyboard and cursor
            }
            return true;
        });

        // set all textviews
        tempValue.setText(String.format(Locale.US,"%.2f%s", thermostat.getTemp(), "°C"));
        humValue.setText(String.format(Locale.US, "%.2f%s", thermostat.getHum(), "%"));

        tempSet.setText(String.format(Locale.US, "%.1f%s", thermostat.getThreshold(),"°C"));
        modeSelector.setText(Const.thermostatModes.get(thermostat.getMode()));
        circularSeekBar.setMax((float) 35);

        ValueAnimator anim = ValueAnimator.ofFloat(0, thermostat.getThreshold());
        anim.setDuration(1000);

        anim.addUpdateListener(animation -> {
            float animProgress = (Float) animation.getAnimatedValue();
            circularSeekBar.setProgress(animProgress);

        });

        anim.start();

        circularSeekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                tempSet.setText(String.format(Locale.ENGLISH, "%.1f°C", progress));

            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                thermostat.setThermostat(2, circularSeekBar.getProgress());
                modeSelector.setText(Const.thermostatModes.get(2));
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
            }

        });

        modeSelector.setOnClickListener(v -> {

            switch(modeSelector.getText().toString()){
                case "Thermostat on":
                    thermostat.setThermostat(2, circularSeekBar.getProgress());
                    break;
                case "Thermostat off":
                    thermostat.setThermostat(1, circularSeekBar.getProgress());
                    break;
                case "Thermostat auto":
                    thermostat.setThermostat(0, circularSeekBar.getProgress());
                    break;
            }
            modeSelector.setText(Const.thermostatModes.get(thermostat.getMode()));
        });

    }

    private void updateDeviceName(String newName){

        this.thermostat.setName(newName);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.DEVICE_NAME, newName);

        String selection = DatabaseHelper.DEVICE_id + "= ?";
        String [] args      = {String.valueOf(thermostat.getId())};

        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        );
        db.close();
    }

    private void updateDeviceDescription(String newDescr){

        this.thermostat.setDescription(newDescr);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.DEVICE_INFO, newDescr);

        String selection = DatabaseHelper.DEVICE_id + "= ?";
        String [] args      = {String.valueOf(thermostat.getId())};

        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        );
        db.close();
    }
}
