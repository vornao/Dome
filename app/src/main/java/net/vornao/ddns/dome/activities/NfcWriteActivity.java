package net.vornao.ddns.dome.activities;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;

import net.vornao.ddns.dome.R;
import net.vornao.ddns.dome.shared.Const;

import java.io.IOException;
import java.util.Objects;



// todo: transmit full json configuration to device

public class NfcWriteActivity extends AppCompatActivity {

    private final JsonObject jsonData = new JsonObject();
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_write_activity);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Write Dome tag");

        // i'm sure this is not null because a random one is always created
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        jsonData.addProperty(Const.MQTT_BROKER_C, sp.getString(Const.MQTT_BROKER_C, ""));
        jsonData.addProperty(Const.MQTT_PORT_C, sp.getString(Const.MQTT_PORT_C, ""));
        jsonData.addProperty(Const.MQTT_USER_C, sp.getString(Const.MQTT_USER_C, ""));
        jsonData.addProperty(Const.MQTT_PASS_C, sp.getString(Const.MQTT_PASS_C, ""));
        jsonData.addProperty(Const.MQTT_TOPIC_C, sp.getString(Const.MQTT_TOPIC_C, ""));

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null){
            Snackbar.make(getWindow().getDecorView(), R.string.no_nfc_detected, Snackbar.LENGTH_SHORT).show();
        }

        if(!nfcAdapter.isEnabled()){
            Snackbar sb =
                    Snackbar.make(getWindow().getDecorView(), R.string.nfc_disabled_sb, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.enable_nfc_sbbutton, view -> {
                        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                        startActivity(intent);
                    });
            sb.show();
        }

        // Creating empty intent with single top flag
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

    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return true;
    }

    public void onBackPressed(){
        Intent back = new Intent(NfcWriteActivity.this, MainActivity.class);
        back.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(back);
    }


    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Ndef ndef = Ndef.get(tag);

        NdefRecord record = NdefRecord.createTextRecord("en", jsonData.toString());
        NdefMessage mMsg = new NdefMessage(record);

        String TAG = "NFC";
        try {
            ndef.connect();
            ndef.writeNdefMessage(mMsg);

            runOnUiThread(() -> new AlertDialog.Builder(NfcWriteActivity.this)
                    .setTitle(R.string.nfc_write_success)
                    .setMessage(R.string.nfc_write_success_msg)
                    .setNeutralButton(R.string.great_button, (dialogInterface, i) -> {
                        finish();
                    })
                    .show());


        } catch (FormatException e) {
            Log.d(TAG, "ERROR FORMAT");
            runOnUiThread(this::showErrorDialog);
        } catch (TagLostException e) {
            Log.d(TAG, "ERROR LOST");
            runOnUiThread(this::showErrorDialog);
        } catch (IOException e){
            runOnUiThread(this::showErrorDialog);
        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                runOnUiThread(this::showErrorDialog);
            }
        }
    }

    private void showErrorDialog(){
        runOnUiThread(() -> new AlertDialog.Builder(NfcWriteActivity.this)
                .setTitle(R.string.configuration_failed)
                .setMessage(R.string.error_reading_tag_sb)
                .setNeutralButton(R.string.try_again, null)
                .show());
    }
}
