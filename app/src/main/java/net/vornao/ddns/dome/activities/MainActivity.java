package net.vornao.ddns.dome.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.adapters.RVAdapter;
import net.vornao.ddns.dome.callbacks.UpdateDeviceCallback;
import net.vornao.ddns.dome.callbacks.UpdatesCallback;
import net.vornao.ddns.dome.devices.Device;
import net.vornao.ddns.dome.devices.EnergyMonitor;
import net.vornao.ddns.dome.devices.RemoteSwitch;
import net.vornao.ddns.dome.devices.Thermostat;
import net.vornao.ddns.dome.fragments.HouseSelectionFragment;
import net.vornao.ddns.dome.handler.DatabaseHelper;
import net.vornao.ddns.dome.handler.DeviceHandler;
import net.vornao.ddns.dome.shared.Const;
import net.vornao.ddns.dome.shared.Secrets;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private DeviceHandler deviceHandler;
    public static final HashMap<Integer, Device> devices = new HashMap<>();
    @androidx.annotation.Nullable
    private  ArrayList<Device> rvDevices = null;

    private String MQTT_TOPIC;

    private int MAX_POWER;
    private boolean EMON_NOTIFICATION_ACTIVE = true;
    private boolean HEAT_NOTIFICATION_ACTIVE = true;
    private String  HEAT_NOTIFICATION_TIME = "18:25";
    private TextView emptyView;
    private SwipeRefreshLayout refreshLayout;
    private final AtomicBoolean blockUpdates = new AtomicBoolean(false);
    @androidx.annotation.Nullable
    private RVAdapter adapter;

    // databse stuff
    @androidx.annotation.Nullable
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    // nfc section
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    final Gson gson = new Gson();

    // cannot make anonymus innner class: sharedpreference register listeners using weakereference
    private final OnSharedPreferenceChangeListener prefListener = new OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences, @NonNull String key) {
            switch (key){
                case Const.MQTT_TOPIC_C:
                    MQTT_TOPIC = sharedPreferences.getString(Const.MQTT_TOPIC_C, "");
                    Log.d("PREFLIST", MQTT_TOPIC);
                    refreshDevices();
                    break;
                case "emon_notification":
                    if(sharedPreferences.getBoolean(key, true))
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(MQTT_TOPIC);
                    else
                        FirebaseMessaging.getInstance().subscribeToTopic(MQTT_TOPIC);
                    break;
                default:
                    break;
            }
        }
    };

    final UpdatesCallback updatesCallback = new UpdatesCallback(){

        @Override
        public void enableUpdates() {
            blockUpdates.set(false);
        }

        @Override
        public void disableUpdates() {
            blockUpdates.set(true);
        }

    };

    final UpdateDeviceCallback updateDeviceCallback = new UpdateDeviceCallback() {
        @Override
        public void onSuccess() {
            if(!blockUpdates.get()) adapter.notifyDataSetChanged();
        }

        @Override
        public void onFailure() {
            if(!blockUpdates.get()) adapter.notifyDataSetChanged();
            Snackbar sb = Snackbar.make(getWindow().getDecorView(),
                    R.string.snackbar_conn_error,
                    Snackbar.LENGTH_LONG);
            sb.show();
        }

        @Override
        public void onSuccess(int id) {
            if(!blockUpdates.get())
            adapter.notifyItemChanged(rvDevices.indexOf(devices.get(id)));
        }

        @Override
        public void onConnSuccess() {
            Snackbar sb = Snackbar.make(getWindow().getDecorView(),
                    R.string.snackbar_update_success,
                    Snackbar.LENGTH_LONG);
            sb.show();
            if(EMON_NOTIFICATION_ACTIVE) subscribeToNotification();
        }

        @Override
        public void onNewDevice(int id) {
            Log.d("NDEV", "added");
            rvDevices.add(devices.get(id));
            devices.get(id).setViewPosition(rvDevices.indexOf(devices.get(id)));
            adapter.notifyItemInserted(rvDevices.indexOf(devices.get(id)));
            emptyView.setVisibility(View.GONE);
            addDeviceToDB(id);
        }
    };

    /**
     * Handle drag and remove of recyclerview cards.
     * swipe action is not handled
     */

    final ItemTouchHelper.SimpleCallback touchHelperCallback =
            new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.START | ItemTouchHelper.END | ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                    ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    int from = viewHolder.getAdapterPosition();
                    int to   = target.getAdapterPosition();
                    rvDevices.get(from).setViewPosition(to);
                    rvDevices.get(to).setViewPosition(from);
                    Collections.swap(rvDevices, from, to);
                    adapter.notifyItemMoved(from, to);

                    // update device position in list
                    updateDevicePosition(rvDevices.get(from).getId(), rvDevices.get(from).getViewPosition());
                    updateDevicePosition(rvDevices.get(to).getId(),rvDevices.get(to).getViewPosition());

                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    if (direction == ItemTouchHelper.RIGHT) {
                        if(!rvDevices.get(viewHolder.getAdapterPosition()).isOffline()){
                            Snackbar.make(getWindow().getDecorView(), R.string.delete_device_error, Snackbar.LENGTH_SHORT).show();
                            errorClickFeedback();
                            // tell rvadapter to put elem back in view
                            adapter.notifyDataSetChanged();
                        }
                        else {
                            longClickFeedback();
                            deleteDatabaseEntry(rvDevices.remove(viewHolder.getAdapterPosition()).getId());
                            adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                            Snackbar.make(getWindow().getDecorView(), R.string.item_deleted_msg, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    blockUpdates.set(false);
                    refreshLayout.setEnabled(true);
                }

                public void onSelectedChanged(RecyclerView.ViewHolder vh, int state) {
                    super.onSelectedChanged(vh, state);

                    if (state == ItemTouchHelper.ACTION_STATE_DRAG) {
                        // we need to disable updates: if rv is updated during card positioning
                        // action would be suddenly interrupted.
                        // (same for remote switch renaming)
                        refreshLayout.setEnabled(false);
                        blockUpdates.set(true);
                        longClickFeedback();
                        Snackbar.make(
                                getWindow().getDecorView(),
                                getResources().getString(R.string.drag_card_snackbar),
                                Snackbar.LENGTH_SHORT).show();
                    }
                    else if(state == ItemTouchHelper.ACTION_STATE_SWIPE){
                        refreshLayout.setEnabled(false);
                        blockUpdates.set(true);
                    }
                }
            };

    /** Menu buttons handler */
    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu, menu);
        ((MenuBuilder)menu).setOptionalIconsVisible(true);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // action with ID action_refresh was selected
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshDevices();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            case R.id.actionJoin:
                Intent intentJoin = new Intent(this, NfcReadActivity.class);
                this.startActivity(intentJoin);
                return true;
            case R.id.actionAddDevice:
                Intent intentAdd = new Intent(this, DeviceSetupActivity.class);
                this.startActivity(intentAdd);
                return true;
            case R.id.actionChange:
                HouseSelectionFragment b = new HouseSelectionFragment();
                Bundle bundle = new Bundle();
                bundle.putStringArray("available-topics", getTopicsAsStringArray());
                b.setArguments(bundle);
                b.show(getSupportFragmentManager(), "select-topic-fragment");
            default:
                return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // arraylists makes recyclerview management a lot easier.
        // since in rv we need to access elements by position, we copy
        // device reference into a brand new arraylist.
        // HashMap is preferred when accessing data by id, such as during MQTT updates.
        rvDevices = new ArrayList<>();

        // display message if no devices are registered to current environment.
        emptyView = findViewById(R.id.empty_view);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());


        // if first start, set default user and topic
        if(sharedPreferences.getBoolean("first-start", true)){
            sharedPreferences.edit().putString(Const.MQTT_BROKER_C, Secrets.DEFAULT_SERVER)
            .putString(Const.MQTT_PORT_C, Secrets.DEFAULT_PORT)
            .putString(Const.MQTT_USER_C, Secrets.DEFAULT_USER)
            .putString(Const.MQTT_PASS_C, Secrets.DEFAULT_PASS)
            .putBoolean("first-start", false)
            .apply();
        }

        String MQTT_SERVER = sharedPreferences.getString(Const.MQTT_BROKER_C, Secrets.DEFAULT_SERVER);
        String MQTT_PORT = sharedPreferences.getString(Const.MQTT_PORT_C, Secrets.DEFAULT_PORT);
        String MQTT_USERNAME = sharedPreferences.getString(Const.MQTT_USER_C, Secrets.DEFAULT_USER);
        String MQTT_PASSWORD = sharedPreferences.getString(Const.MQTT_PASS_C, Secrets.DEFAULT_PASS);

        MQTT_TOPIC = sharedPreferences.getString(Const.MQTT_TOPIC_C, null);
        MAX_POWER = Integer.parseInt(sharedPreferences.getString("emon_max_power", "3300")) + 700;
        EMON_NOTIFICATION_ACTIVE = sharedPreferences.getBoolean("emon_notification", true);
        HEAT_NOTIFICATION_ACTIVE = sharedPreferences.getBoolean("heat_notification_check", true);
        HEAT_NOTIFICATION_TIME   = sharedPreferences.getString("heat_notification_time", "22:00");


        if(MQTT_TOPIC == null){
            MQTT_TOPIC = createNewDomeTopic();
            Snackbar.make(getWindow().getDecorView(), "Created new dome environment add new devices!", Snackbar.LENGTH_LONG).show();
            sharedPreferences.edit().putString("mqtt-topic", MQTT_TOPIC).apply();
        }

        // MQTT Wrapper setup
        deviceHandler = new DeviceHandler(
                getApplicationContext(),
                String.format("tcp://%s:%s", MQTT_SERVER, MQTT_PORT),
                MQTT_USERNAME,
                MQTT_PASSWORD,
                devices,
                MQTT_TOPIC,
                updateDeviceCallback
        );

        // Start MQTT for selected home
        deviceHandler.connect();

        LinearLayoutManager llm = new LinearLayoutManager(this);
        refreshLayout = findViewById(R.id.swipeRefresh);
        refreshLayout.setColorSchemeColors(getResources().getColor(R.color.teal_700, getTheme()));
        refreshLayout.setOnRefreshListener(() -> {
            refreshDevices();
            refreshLayout.setRefreshing(false);
        });

        // set up recyclerview to show devices
        // RVadapter setup

        RecyclerView rv = findViewById(R.id.rv);
        adapter = new RVAdapter(rvDevices, MAX_POWER, rv, updatesCallback, this);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        rv.setHasFixedSize(false);
        rv.setLayoutManager(llm);
        rv.setAdapter(adapter);
        ItemTouchHelper ith = new ItemTouchHelper(touchHelperCallback);
        ith.attachToRecyclerView(rv);

        // restore cached devices -> run on new thread, heavy operation
        new Thread(() -> {
            dbHelper = DatabaseHelper.getInstance(getApplicationContext());
            db = dbHelper.getWritableDatabase();
            deviceFactory();
            addTopicToDB(MQTT_TOPIC);
            runOnUiThread(() -> {
                if(rvDevices.isEmpty()) emptyView.setVisibility(View.VISIBLE);
                else emptyView.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            });
        }).start();

        firebaseSetup();
        nfcSetup();
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);

    }

    protected void onNewIntent(@androidx.annotation.Nullable Intent intent) {
        super.onNewIntent(intent);
        if(intent != null)
            setIntent(intent);
            handleNfcTag(intent);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        // keep MQTT and db open as long as possible
        deviceHandler.close();
        dbHelper.close();
        db.close();
    }

    @Override
    protected void onPause(){
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent,null,null);
        dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        db = dbHelper.getWritableDatabase();
    }


     //Here starts database stuff
    /** Restore previous devices: if a device is not online, we'll wait for it
     * !!!!!!!!!!!!  DO NOT UPDATE UI HERE, RUN BY NON UI THREAD   !!!!!!!!!!!
     */
    private void deviceFactory(){
        Cursor cursor;
        try {
            cursor = db.query(
                    DatabaseHelper.DEVICE_TABLE,
                    null, DatabaseHelper.DEVICE_HOUSE + " = ?", new String[]{MQTT_TOPIC},
                    null,
                    null,
                    null);
        }catch (IllegalStateException e){
            Snackbar.make(getWindow().getDecorView(), R.string.generic_connection_error_message, Snackbar.LENGTH_SHORT).show();
            return;
        }


        while(cursor.moveToNext()){

            int devID = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_id));
            String devType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_TYPE));
            String devName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_NAME));
            String homeID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_HOUSE));
            int pos   = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.RV_POSITION));

            Log.d("DATABASE", String.valueOf(devID));
            switch (devType) {
                case Const.THERMOSTAT:
                    Device t = new Thermostat(devID, devType, deviceHandler, homeID, devName);
                    devices.put(devID, t);
                    rvDevices.add(t);
                    break;
                case Const.ENERGY_MONITOR:
                    Device e = new EnergyMonitor(devID, devType, deviceHandler, homeID, devName);
                    devices.put(devID, e);
                    rvDevices.add(e);
                    break;
                case Const.REMOTE_SWITCH:
                    Device r =  new RemoteSwitch(devID, devType, deviceHandler, homeID, devName);
                    devices.put(devID, r);
                    rvDevices.add(r);
                    break;
            }

            devices.get(devID).setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEVICE_INFO)));
            devices.get(devID).setViewPosition(pos);
        }
        cursor.close();
        Collections.sort(rvDevices);
    }

    /** called when new house is added from main */
    private void addTopicToDB(String topic){
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.HOUSE_ID, topic);
        db.insert(DatabaseHelper.HOUSES_TABLE, null, values);
    }

    /** Called when new device detected */
    private void addDeviceToDB(int id){
        Device n = devices.get(id);
        ContentValues values = new ContentValues();

        assert n != null;
        values.put(DatabaseHelper.DEVICE_id, n.getId());
        values.put(DatabaseHelper.DEVICE_NAME, n.getName());
        values.put(DatabaseHelper.DEVICE_INFO, (byte[]) null);
        values.put(DatabaseHelper.DEVICE_HOUSE, n.getHome_id());
        values.put(DatabaseHelper.DEVICE_TYPE, n.getType());
        values.put(DatabaseHelper.RV_POSITION, n.getViewPosition());
        values.put(DatabaseHelper.DEVICE_HOUSE, MQTT_TOPIC);
        db.insert(DatabaseHelper.DEVICE_TABLE, null, values);
    }

    /** Called when device is moved */
    private void updateDevicePosition(int id, int pos){

        Device n = devices.get(id);
        ContentValues values = new ContentValues();
        try {
            assert n != null;
        }
        catch(AssertionError e){
            return;
        }
        values.put(DatabaseHelper.DEVICE_id, n.getId());
        values.put(DatabaseHelper.DEVICE_NAME, n.getName());
        values.put(DatabaseHelper.DEVICE_INFO, (byte[]) null);
        values.put(DatabaseHelper.DEVICE_HOUSE, n.getHome_id());
        values.put(DatabaseHelper.DEVICE_TYPE, n.getType());
        values.put(DatabaseHelper.RV_POSITION, pos);
        db.update(DatabaseHelper.DEVICE_TABLE,  values,
                DatabaseHelper.DEVICE_id + " = ?",
                new String[]{String.valueOf(id)});
    }

    private void deleteDatabaseEntry(int id){
        db.delete(DatabaseHelper.DEVICE_TABLE, DatabaseHelper.DEVICE_id + "= ?", new String[]{String.valueOf(id)});
    }


    @Nullable
    private String[] getTopicsAsStringArray(){

        // distinct would be unnecessary since we're querying on primary keys
        try {
            Cursor cursor = db.query(
                    DatabaseHelper.HOUSES_TABLE,
                    new String[]{DatabaseHelper.HOUSE_ID},
                    null,
                    null,
                    null,
                    null,
                    null);

            String[] res = new String[cursor.getCount()];

            while (cursor.moveToNext()) {
                res[cursor.getPosition()] = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.HOUSE_ID));
            }
            cursor.close();
            return res;
        }catch (IllegalStateException e){
            return null;
        }

    }

    private void firebaseSetup(){

        // Check if we need notification channels ->
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    getApplicationContext()
                            .getResources()
                            .getString(R.string.default_notification_channel_id),
                    "dome_notification_channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Firebase messages topic is the same as the MQTT house topic for coherence.
        // New devices will subscribe to notification topics from firebase as well as mqtt.
        // We don't have to hang on with changing tokens etc.
        FirebaseMessaging.getInstance().subscribeToTopic(MQTT_TOPIC);

    }

    /**
     * Check if current instance of dome has to receive alert notifications
     * Notification subscription is done via MQTT, publishing our intention to
     * dome/firebase-requests topic.
     */
    private void subscribeToNotification(){

        if(EMON_NOTIFICATION_ACTIVE) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("notification-type", "energy-monitor");
            jsonObject.addProperty("house-id", MQTT_TOPIC);
            jsonObject.addProperty("threshold", MAX_POWER);
            deviceHandler.publish("dome/firebase-requests", jsonObject.toString(), 2);
            FirebaseMessaging.getInstance().subscribeToTopic("dome-" + MQTT_TOPIC + "-energy-monitor");
        }
        if(HEAT_NOTIFICATION_ACTIVE) {
            JsonObject jsonObject1 = new JsonObject();
            jsonObject1.addProperty("notification-type", "thermostat");
            jsonObject1.addProperty("house-id", MQTT_TOPIC);
            jsonObject1.addProperty("threshold", HEAT_NOTIFICATION_TIME);
            deviceHandler.publish("dome/firebase-requests", jsonObject1.toString(), 2);
            FirebaseMessaging.getInstance().subscribeToTopic("dome-" + MQTT_TOPIC + "-thermostat");
        }
    }

    /**
     * Create new Dome context when application is first started and db empty
     * @return String with new topic
     */

    @NonNull
    private String createNewDomeTopic(){
        return "dome-" + (int) (Math.random() * 10000);
    }



    /**
     * Provides simple tactile feedback when something relevant occurs
     */

    private void longClickFeedback(){
        Vibrator v = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VibrationEffect.createOneShot(50, 255));
    }

    private void errorClickFeedback(){
        Vibrator v = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VibrationEffect.createWaveform(new long[] {0, 50, 100, 50, 100}, -1));
    }

    private void refreshDevices(){
        longClickFeedback();
        Snackbar.make(getWindow().getDecorView(), "Updating...", Snackbar.LENGTH_LONG).show();
        devices.clear();
        rvDevices.clear();
        adapter.notifyDataSetChanged();
        deviceHandler.close();
        deviceHandler.setHouseTopic(MQTT_TOPIC);
        new Thread(() -> {
            deviceFactory();
            runOnUiThread(() -> {
                if(rvDevices.isEmpty()) emptyView.setVisibility(View.VISIBLE);
                else emptyView.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            });
        }).start();
        deviceHandler.connect();
    }

    // nfc from main activity, very similar to nfcreadactivity.java

    private void nfcSetup(){
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this,
                        this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
    }

    private void handleNfcTag(@NonNull Intent intent){
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        try {
            assert tag != null;
        } catch (AssertionError e){
            return;
        }
        Ndef ndef = Ndef.get(tag);
        NdefRecord[] records;

        try {
            NdefMessage msg = ndef.getCachedNdefMessage();
            records = msg.getRecords();
        } catch (Exception e){
            runOnUiThread(() -> Snackbar.make(
                    getWindow().getDecorView(),
                    getString(R.string.error_reading_tag_sb),
                    Snackbar.LENGTH_SHORT).show());
            return;
        }

        String decoded = new String(records[0].getPayload(), StandardCharsets.UTF_8);
        String TAG = "NFC";
        Log.d(TAG, decoded);

        final String text = decoded.substring(3);
        final JsonObject readData = gson.fromJson(text, JsonObject.class);

        try {
            if (!readData.get(Const.MQTT_TOPIC_C).getAsString().equals(MQTT_TOPIC)){
                PreferenceManager
                        .getDefaultSharedPreferences(this.getApplicationContext())
                        .edit()
                        .putString(Const.MQTT_TOPIC_C, readData.get(Const.MQTT_TOPIC_C).getAsString())
                        .putString(Const.MQTT_USER_C, readData.get(Const.MQTT_USER_C).getAsString())
                        .putString(Const.MQTT_PASS_C, readData.get(Const.MQTT_PASS_C).getAsString())
                        .putString(Const.MQTT_BROKER_C, readData.get(Const.MQTT_BROKER_C).getAsString())
                        .putString(Const.MQTT_PORT_C, readData.get(Const.MQTT_PORT_C).getAsString())
                        .apply();

                addTopicToDB(readData.get(Const.MQTT_TOPIC_C).getAsString());
                refreshDevices();
            }

        }catch (Exception e){
            //same reason of nfcReadActivity
            runOnUiThread(() -> Snackbar.make(
                    getWindow().getDecorView(),
                    getString(R.string.error_reading_tag_sb),
                    Snackbar.LENGTH_SHORT).show());
        }
    }
}
