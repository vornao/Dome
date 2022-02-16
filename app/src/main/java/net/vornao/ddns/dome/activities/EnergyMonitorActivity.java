package net.vornao.ddns.dome.activities;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.devices.EnergyMonitor;
import net.vornao.ddns.dome.fragments.BottomSheetFragment;
import net.vornao.ddns.dome.handler.DatabaseHelper;
import net.vornao.ddns.dome.shared.Const;
import net.vornao.ddns.dome.views.ChartBarDrawer;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


import me.tankery.lib.circularseekbar.CircularSeekBar;

public class EnergyMonitorActivity extends AppCompatActivity {

    private RequestQueue requestQueue;

    private EditText deviceName;
    private EditText deviceDesc;
    private TextView energy;
    private TextView power;
    private TextView impact;
    private CircularSeekBar powerSeekBar;
    private EnergyMonitor energyMonitor;
    private ProgressBar loadingCircle;
    private static int MAX_POWER = 4000;

    private ProgressBar weeklyLoadingCircle;
    private boolean     firstStart = true;

    private final float[] energyConsumptions = new float[7];

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.emon_actionbar_menu, menu);
        return true;

    }

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_refresh:
                energy.setVisibility(View.INVISIBLE);
                loadingCircle.setVisibility(View.VISIBLE);
                getEnergyFromData();
                return true;
            case R.id.action_details:
                BottomSheetFragment b = new BottomSheetFragment();
                Bundle bundle = new Bundle();
                bundle.putString(Const.DEVICE_ID, String.valueOf(energyMonitor.getId()));
                bundle.putString(Const.MQTT_TOPIC_C, energyMonitor.getHome_id());
                b.setArguments(bundle);
                b.show(getSupportFragmentManager(), "bsfd_fragment");
                return true;
            default:
                return false;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.emon_activity);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        MAX_POWER = Integer.parseInt(
                sharedPreferences.getString("emon_max_power", "3300")) + 700;

        deviceName = findViewById(R.id.emon_name);
        deviceDesc = findViewById(R.id.emon_desc);
        energy = findViewById(R.id.energy_tv);
        energy.setVisibility(View.INVISIBLE);
        power  = findViewById(R.id.power_tv);
        impact = findViewById(R.id.impactTextView);
        powerSeekBar = findViewById(R.id.power_seekbar);
        powerSeekBar.setEnabled(false);
        loadingCircle = findViewById(R.id.progressbar);
        weeklyLoadingCircle = findViewById(R.id.summaryProgressbar);
        loadingCircle.setIndeterminate(true);

        // setup queue for HTTP history requests
        requestQueue = Volley.newRequestQueue(this);

        Intent intent  = getIntent();

        int deviceID = intent.getIntExtra(Const.DEVICE_ID, 0);
        Log.d("EMON_ACTIVITY", String.valueOf(deviceID));

        energyMonitor = (EnergyMonitor)MainActivity.devices.get(deviceID);

        try {
            assert energyMonitor != null;
        }catch (AssertionError e){
            Snackbar.make(getWindow().getDecorView(), R.string.generic_connection_error_message, Snackbar.LENGTH_SHORT);
        }

        power.setText(String.format(Locale.US, "%.1fW", energyMonitor.getPower()));
        powerSeekBar.setMax((float)MAX_POWER);
        powerSeekBar.setLockEnabled(true);          // prevent user from touching seekbar
        powerSeekBar.setNegativeEnabled(false);


        // fancy animation for value
        ValueAnimator anim;

        if(energyMonitor.getPower() > MAX_POWER)
            anim = ValueAnimator.ofFloat(0, MAX_POWER);
        else
            anim = ValueAnimator.ofFloat(0, energyMonitor.getPower());

        anim.setDuration(1000);
        anim.addUpdateListener(animation -> {
            float animProgress = (Float) animation.getAnimatedValue();
            powerSeekBar.setProgress(animProgress);

        });

        anim.start();


        if(energyMonitor.getName() != null) deviceName.setText(energyMonitor.getName());
        else deviceName.setText(R.string.power_meter_title);

        if(energyMonitor.getDescription() != null) deviceDesc.setText(energyMonitor.getDescription());
        else deviceDesc.setText(String.valueOf(energyMonitor.getId()));

        energyMonitor.setEmonCallback(() -> {
            float p = energyMonitor.getPower();
            power.setText(String.format(Locale.US, "%.1fW", p));
            impact.setText(String.valueOf(Const.getEnergyImpactString(p, MAX_POWER, this)));

            // we don't know if device is online or offline, wait for first packet to come.

            if(firstStart) {
                firstStart = false;
                getEnergyFromData();
            }
            if(energyMonitor.getPower() > MAX_POWER)
                anim.setFloatValues(powerSeekBar.getProgress(), MAX_POWER);
            else
                anim.setFloatValues(powerSeekBar.getProgress(), energyMonitor.getPower());

            anim.end();
            anim.start();

        });

        deviceName.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == EditorInfo.IME_ACTION_DONE){
                Log.i("EDITORACTION", deviceName.getText().toString());
                updateDeviceName(deviceName.getText().toString());
                deviceName.clearFocus();
                return false;
            }
            return true;
        });

        deviceDesc.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == EditorInfo.IME_ACTION_DONE){
                Log.i("EDITORACTION", deviceDesc.getText().toString());
                updateDeviceDescription(deviceDesc.getText().toString());
                deviceDesc.clearFocus();
                return false;
            }
            return true;
        });
    }


    /**
     * Request today energy from AWS server
     * When done, request weekly report from api
     */
    private void getEnergyFromData(){
        Calendar c = Calendar.getInstance();
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, energyMonitor.getDataUrl() + "/current-energy", null,
                response -> {
                    try {
                        Log.d("VOLLEY", response.toString());
                        JSONObject body =  response.getJSONObject("body");
                        float currentEnergy = (float)body.getDouble("energy");
                        loadingCircle.setVisibility(View.INVISIBLE);
                        energy.setText(String.format(Locale.US, "%.2f kWh", currentEnergy));
                        // update today value!
                        energyConsumptions[Const.realWeek.get(c.get(Calendar.DAY_OF_WEEK))-1] = currentEnergy;
                        energy.setVisibility(View.VISIBLE);
                        getEnergyHistory();

                    } catch (JSONException e) {

                        Log.d("VOLLEY", "JsonError");
                        e.printStackTrace();
                    }
                },
                error -> {
                    // TODO: Handle error
                    Log.d("VOLLEY", "ErrorRequest");

                });

        requestQueue.add(request);
    }

    /**
     * Database utils when user changes name
     * @param newName
     */
    private void updateDeviceName(String newName){

        this.energyMonitor.setName(newName);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.DEVICE_NAME, newName);

        String selection = DatabaseHelper.DEVICE_id + "= ?";
        String [] args      = {String.valueOf(energyMonitor.getId())};

        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        );
        db.close();
    }

    /**
     * same but changes description
     * @param newDescr
     */
    private void updateDeviceDescription(String newDescr){

        this.energyMonitor.setDescription(newDescr);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.DEVICE_INFO, newDescr);

        String selection = DatabaseHelper.DEVICE_id + "= ?";
        String [] args      = {String.valueOf(energyMonitor.getId())};

        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        );
        db.close();
    }

    private void getEnergyHistory() {

        // needed for aws api
        // java calendar util is quite horrifying
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dt;  // Start date

        Calendar cal = Calendar.getInstance(Locale.getDefault());

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);


        //first, let's get first day of current week
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

        dt = sdf.format(cal.getTime());  // dt is now the new date
        Log.d("CALENDAR", dt);

        for(int i = 0; i < 7; i++) {
            JSONObject requestBody = new JSONObject();

            try {
                requestBody.put("date", dt);
                requestBody.put("type", "summary");
                Log.d("VOLLEY", requestBody.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            int finalI = i;
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, energyMonitor.getDataUrl() + "history", requestBody,
                    response -> {
                        try {
                            JSONObject body = response.getJSONObject("body").getJSONObject("Item");
                            weeklyLoadingCircle.setVisibility(View.INVISIBLE);
                            energyConsumptions[finalI] = (float) body.getDouble("kwh");
                            Log.d("VOLLEY", body.getString("kwh"));
                            if(finalI == 6) plotChart();
                        } catch (JSONException e) {
                            Log.d("VOLLEY", "JsonError");
                            if(finalI == 6) plotChart();
                            e.printStackTrace();
                        }
                    },
                    error -> { Log.d("VOLLEY", "ErrorRequest"); if(finalI == 6) plotChart();});
            requestQueue.add(request);

            // increment calendar
            cal.add(Calendar.DATE, 1);
            dt = sdf.format(cal.getTime());

        }

    }

    private void createHistoryBarElem(float val, int dayOfWeek){
        LinearLayout ll = findViewById(R.id.summaryLayout);

        ChartBarDrawer cbd;

        // above each row
        LinearLayout dayLayout = new LinearLayout(this);
        dayLayout.setOrientation(LinearLayout.HORIZONTAL);

        //for dow
        TextView dowtv = new TextView(this);

        // for energy value
        TextView dayEnergyTv = new TextView(this);
        dayEnergyTv.setText(String.format(getString(R.string.chartbar_kwh), val));

        cbd = new ChartBarDrawer(this, val);
        dowtv.setText(Const.daysOfWeek.get(dayOfWeek));
        dowtv.setPadding(energy.getPaddingLeft(), energy.getPaddingLeft()/3, 0,energy.getPaddingLeft()/4);
        cbd.setPadding(energy.getPaddingLeft(), energy.getPaddingTop(), 0, energy.getPaddingLeft());
        dayEnergyTv.setBackgroundColor(getResources().getColor(android.R.color.transparent, getTheme()));


        // if we're plotting today make it bold
        if(dayOfWeek + 1 == Const.realWeek.get(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))){
            dowtv.setTypeface(null, Typeface.BOLD);
            dayEnergyTv.setTypeface(null, Typeface.BOLD);
        }

        // once measures are defined ->
        dowtv.post(() -> {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            // compute position for rect
            int rectMargin = (int) ((this.getWindow().getDecorView().getWidth() - energy.getPaddingLeft()) * val / 15);

            if(rectMargin > dowtv.getMeasuredWidth() + 40) {
                params.setMargins((rectMargin - dayEnergyTv.getWidth() / 2 - dowtv.getMeasuredWidth()),
                        0,
                        0,
                        0);
                dayEnergyTv.setLayoutParams(params);
                dayLayout.addView(dayEnergyTv);
            } else{
                params.setMargins(40, 0, 0, 0);

            }
        });
        dayLayout.addView(dowtv);
        ll.addView(dayLayout);
        ll.addView(cbd);
    }


    // for each value in array, plot bar with custom view.
    // if old views are there, discard them
    private void plotChart(){
        ((LinearLayout) findViewById(R.id.summaryLayout)).removeAllViews();
        weeklyLoadingCircle.setVisibility(View.GONE);
        for(int i = 0; i < 7; i++){
            createHistoryBarElem(energyConsumptions[i], i);
        }
    }
}
