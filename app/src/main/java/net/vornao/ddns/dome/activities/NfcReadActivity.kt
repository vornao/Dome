package net.vornao.ddns.dome.activities

import net.vornao.ddns.dome.handler.DatabaseHelper.Companion.getInstance
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.vornao.ddns.dome.R
import android.content.Intent
import com.google.android.material.snackbar.Snackbar
import android.app.AlertDialog
import net.vornao.ddns.dome.handler.DatabaseHelper
import android.content.ContentValues
import android.app.PendingIntent
import com.google.gson.Gson
import android.nfc.tech.Ndef
import com.google.gson.JsonObject
import android.content.DialogInterface
import android.nfc.*
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.preference.PreferenceManager
import net.vornao.ddns.dome.shared.Const
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.util.*

class NfcReadActivity : AppCompatActivity() {
    lateinit var nfcAdapter: NfcAdapter
    lateinit var pendingIntent: PendingIntent
    val gson = Gson()

    /** Menu buttons handler  */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.nfc_actionbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.writeNfc) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            val intent = Intent(this, NfcWriteActivity::class.java)
            intent.putExtra("mqtt-topic", sharedPreferences.getString("mqtt-topic", null))
            this.startActivity(intent)
            finish()
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nfc_activity)
        supportActionBar!!.setTitle(R.string.join_dome_network_title)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // bring user to nfc settings if it's not enabled
        if (!nfcAdapter.isEnabled) {
            val sb = Snackbar.make(window.decorView, R.string.nfc_disabled_sb, Snackbar.LENGTH_LONG)
                    .setAction(R.string.enable_nfc_sbbutton) { _: View? ->
                        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                        startActivity(intent)
                    }
            sb.show()
        }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val ndef = Ndef.get(tag)
        val records: Array<NdefRecord> = try {
            val msg = ndef.cachedNdefMessage
            msg.records
        } catch (e: Exception) {
            runOnUiThread {
                Snackbar.make(
                        window.decorView,
                        getString(R.string.error_reading_tag_sb),
                        Snackbar.LENGTH_SHORT).show()
            }
            return
        }
        val decoded = String(records[0].payload, StandardCharsets.UTF_8)
        val TAG = "NFC"
        Log.d(TAG, decoded)
        val text = decoded.substring(3)
        val readData = gson.fromJson(text, JsonObject::class.java)
        try {
            runOnUiThread {
                AlertDialog.Builder(this@NfcReadActivity)
                        .setTitle(getString(R.string.join_dome_network_title))
                        .setMessage(String.format(getString(R.string.join_alert_msg), readData[Const.MQTT_TOPIC_C].asString))
                        .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                            PreferenceManager
                                    .getDefaultSharedPreferences(this.applicationContext)
                                    .edit()
                                    .putString(Const.MQTT_TOPIC_C, readData[Const.MQTT_TOPIC_C].asString)
                                    .putString(Const.MQTT_USER_C, readData[Const.MQTT_USER_C].asString)
                                    .putString(Const.MQTT_PASS_C, readData[Const.MQTT_PASS_C].asString)
                                    .putString(Const.MQTT_BROKER_C, readData[Const.MQTT_BROKER_C].asString)
                                    .putString(Const.MQTT_PORT_C, readData[Const.MQTT_PORT_C].asString)
                                    .apply()
                            updateDatabase(readData[Const.MQTT_TOPIC_C].asString)

                            //restart application
                            finish()
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
            }
        } catch (e: Exception) {
            // run this on ui thread, otherwise app will crash randomly
            runOnUiThread {
                Snackbar.make(
                        window.decorView,
                        getString(R.string.error_reading_tag_sb),
                        Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDatabase(topic: String) {
        val dbHelper = getInstance(applicationContext)
        val db = dbHelper!!.writableDatabase
        val values = ContentValues()
        values.put(DatabaseHelper.HOUSE_ID, topic)
        db.insert(DatabaseHelper.HOUSES_TABLE, null, values)
    }
}