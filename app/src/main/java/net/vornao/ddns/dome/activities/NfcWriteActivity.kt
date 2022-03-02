package net.vornao.ddns.dome.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.vornao.ddns.dome.R
import android.content.Intent
import com.google.android.material.snackbar.Snackbar
import android.app.AlertDialog
import android.app.PendingIntent
import android.nfc.tech.Ndef
import com.google.gson.JsonObject
import android.content.DialogInterface
import android.nfc.*
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.preference.PreferenceManager
import net.vornao.ddns.dome.shared.Const
import java.io.IOException
import java.util.*

// todo: transmit full json configuration to device
class NfcWriteActivity : AppCompatActivity() {
    private val jsonData = JsonObject()
    lateinit var nfcAdapter: NfcAdapter
    lateinit var pendingIntent: PendingIntent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nfc_write_activity)
        supportActionBar?.title = "Write Dome Tag"

        // i'm sure this is not null because a random one is always created
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        jsonData.addProperty(Const.MQTT_BROKER_C, sp.getString(Const.MQTT_BROKER_C, ""))
        jsonData.addProperty(Const.MQTT_PORT_C, sp.getString(Const.MQTT_PORT_C, ""))
        jsonData.addProperty(Const.MQTT_USER_C, sp.getString(Const.MQTT_USER_C, ""))
        jsonData.addProperty(Const.MQTT_PASS_C, sp.getString(Const.MQTT_PASS_C, ""))
        jsonData.addProperty(Const.MQTT_TOPIC_C, sp.getString(Const.MQTT_TOPIC_C, ""))
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (!nfcAdapter.isEnabled) {
            val sb = Snackbar.make(window.decorView, R.string.nfc_disabled_sb, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.enable_nfc_sbbutton) { view: View? ->
                        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                        startActivity(intent)
                    }
            sb.show()
        }

        // Creating empty intent with single top flag
        // we can now receive NFC intent from system when tag is detected
        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(
                        this,
                        this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
    }

    override fun onResume() {
        // pass detected tag intent to foreground activity
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        // stop listening for nfc tag
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
    }

    override fun onBackPressed() {
        val back = Intent(this@NfcWriteActivity, MainActivity::class.java)
        back.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(back)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val ndef = Ndef.get(tag)
        val record = NdefRecord.createTextRecord("en", jsonData.toString())
        val mMsg = NdefMessage(record)
        val TAG = "NFC"
        try {
            ndef.connect()
            ndef.writeNdefMessage(mMsg)
            runOnUiThread {
                AlertDialog.Builder(this@NfcWriteActivity)
                        .setTitle(R.string.nfc_write_success)
                        .setMessage(R.string.nfc_write_success_msg)
                        .setNeutralButton(R.string.great_button) { dialogInterface: DialogInterface?, i: Int -> finish() }
                        .show()
            }
        } catch (e: FormatException) {
            Log.d(TAG, "ERROR FORMAT")
            runOnUiThread { showErrorDialog() }
        } catch (e: TagLostException) {
            Log.d(TAG, "ERROR LOST")
            runOnUiThread { showErrorDialog() }
        } catch (e: IOException) {
            runOnUiThread { showErrorDialog() }
        } finally {
            try {
                ndef.close()
            } catch (e: IOException) {
                runOnUiThread { showErrorDialog() }
            }
        }
    }

    private fun showErrorDialog() {
        runOnUiThread {
            AlertDialog.Builder(this@NfcWriteActivity)
                    .setTitle(R.string.configuration_failed)
                    .setMessage(R.string.error_reading_tag_sb)
                    .setNeutralButton(R.string.try_again, null)
                    .show()
        }
    }
}