package net.vornao.ddns.dome.activities;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.handler.DatabaseHelper;
import net.vornao.ddns.dome.shared.Const;

import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class NfcReadActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    final Gson gson = new Gson();

    /** Menu buttons handler */
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nfc_actionbar_menu, menu);
        return true;

    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.writeNfc){
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            Intent intent = new Intent(this, NfcWriteActivity.class);
            intent.putExtra("mqtt-topic", sharedPreferences.getString("mqtt-topic", null));
            this.startActivity(intent);
            finish();
            return true;
        }
        return false;

    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_activity);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.join_dome_network_title);


        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null){
            Snackbar.make(
                    getWindow().getDecorView(),
                    getString(R.string.no_nfc_capabilities),
                    Snackbar.LENGTH_SHORT).show();
            finish();
        }

        // bring user to nfc settings if it's not enabled
        if(!nfcAdapter.isEnabled()){
            Snackbar sb =
                    Snackbar.make(getWindow().getDecorView(), R.string.nfc_disabled_sb, Snackbar.LENGTH_LONG)
                            .setAction(R.string.enable_nfc_sbbutton, view -> {
                                Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                                startActivity(intent);
                            });
            sb.show();
        }

        // we can now receive NFC intent from system when tag is detected
        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(
                        this,
                        this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);

    }


    protected void onResume() {
        // pass detected tag intent to foreground activity
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent,null,null);
    }


    protected void onPause() {
        super.onPause();

        // stop listening for nfc tag
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }


    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
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
            runOnUiThread(() -> new AlertDialog.Builder(NfcReadActivity.this)
                    .setTitle(getString(R.string.join_dome_network_title))
                    .setMessage(String.format(getString(R.string.join_alert_msg), readData.get(Const.MQTT_TOPIC_C).getAsString()))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                        PreferenceManager
                                .getDefaultSharedPreferences(this.getApplicationContext())
                                .edit()
                                .putString(Const.MQTT_TOPIC_C, readData.get(Const.MQTT_TOPIC_C).getAsString())
                                .putString(Const.MQTT_USER_C, readData.get(Const.MQTT_USER_C).getAsString())
                                .putString(Const.MQTT_PASS_C, readData.get(Const.MQTT_PASS_C).getAsString())
                                .putString(Const.MQTT_BROKER_C, readData.get(Const.MQTT_BROKER_C).getAsString())
                                .putString(Const.MQTT_PORT_C, readData.get(Const.MQTT_PORT_C).getAsString())
                                .apply();

                        updateDatabase(readData.get(Const.MQTT_TOPIC_C).getAsString());

                        //restart application
                        finish();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show());
        }catch (Exception e){
            // run this on ui thread, otherwise app will crash randomly
            runOnUiThread(() -> Snackbar.make(
                    getWindow().getDecorView(),
                    getString(R.string.error_reading_tag_sb),
                    Snackbar.LENGTH_SHORT).show());
        }


    }

    private void updateDatabase(String topic){
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.HOUSE_ID, topic);
        db.insert(DatabaseHelper.HOUSES_TABLE, null, values);
    }


}
