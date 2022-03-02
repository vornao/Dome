package net.vornao.ddns.dome.activities

import net.vornao.ddns.dome.handler.DatabaseHelper.Companion.getInstance
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.vornao.ddns.dome.R
import me.tankery.lib.circularseekbar.CircularSeekBar
import net.vornao.ddns.dome.fragments.BottomSheetFragment
import com.google.android.material.snackbar.Snackbar
import android.animation.ValueAnimator
import android.view.inputmethod.EditorInfo

import net.vornao.ddns.dome.handler.DatabaseHelper
import android.content.ContentValues

import android.util.Log
import android.view.*
import android.widget.*
import net.vornao.ddns.dome.devices.Thermostat
import me.tankery.lib.circularseekbar.CircularSeekBar.OnCircularSeekBarChangeListener
import net.vornao.ddns.dome.shared.Const
import java.lang.AssertionError
import java.util.*

class ThermostatActivity : AppCompatActivity() {
    private lateinit var thermostat: Thermostat
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.thermostat_actionbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // action with ID action_refresh was selected
        if (item.itemId == R.id.action_details) {
            val b = BottomSheetFragment()
            val bundle = Bundle()
            bundle.putString(Const.DEVICE_ID, thermostat.id.toString())
            bundle.putString(Const.MQTT_TOPIC_C, thermostat.home_id)
            b.arguments = bundle
            b.show(supportFragmentManager, "bsfd_fragment")
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.thermostat_activity)
        val modeSelector = findViewById<Button>(R.id.mode_selector)
        modeSelector.elevation = 5.toFloat()
        val deviceDescription = findViewById<EditText>(R.id.thermostat_descr)
        val deviceName = findViewById<EditText>(R.id.thermostat_name)
        val humValue = findViewById<TextView>(R.id.hum_value_tw)
        val tempValue = findViewById<TextView>(R.id.temp_val_tw)
        val tempSet = findViewById<TextView>(R.id.temp_set)
        val circularSeekBar = findViewById<CircularSeekBar>(R.id.cs)
        val intent = intent
        val deviceID = intent.getIntExtra(Const.DEVICE_ID, 0)

        Log.d("THERMOSTAT", deviceID.toString())

        thermostat = MainActivity.devices[deviceID] as Thermostat

        try {
        } catch (e: AssertionError) {
            Snackbar.make(window.decorView, R.string.generic_connection_error_message, Snackbar.LENGTH_SHORT).show()
        }

        deviceName.setText(thermostat.name)
        if (thermostat.description == null)
            deviceDescription.setText(R.string.update_dev_descr)
        else
            deviceDescription.setText(thermostat.description)

        deviceName.setImeActionLabel("Save", KeyEvent.KEYCODE_ENTER)
        deviceName.imeOptions = EditorInfo.IME_ACTION_DONE
        deviceDescription.setImeActionLabel("edit", KeyEvent.KEYCODE_ENTER)
        deviceDescription.imeOptions = EditorInfo.IME_ACTION_DONE

        deviceName.setOnEditorActionListener { _: TextView?, i: Int, _: KeyEvent? ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                Log.i("EDITORACTION", deviceName.text.toString())
                updateDeviceName(deviceName.text.toString())
                deviceName.isCursorVisible = false
                deviceName.clearFocus()
                return@setOnEditorActionListener false // hide keyboard and cursor
            }
            true
        }

        deviceDescription.setOnEditorActionListener { _: TextView?, i: Int, _: KeyEvent? ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                Log.i("EDITORACTION", deviceDescription.text.toString())
                updateDeviceDescription(deviceDescription.text.toString())
                deviceDescription.isCursorVisible = false
                deviceDescription.clearFocus()
                return@setOnEditorActionListener false //hide keyboard and cursor
            }
            true
        }

        // set all textviews
        tempValue.text = String.format(Locale.US, "%.2f%s", thermostat.temp, "°C")
        humValue.text = String.format(Locale.US, "%.2f%s", thermostat.hum, "%")
        tempSet.text = String.format(Locale.US, "%.1f%s", thermostat.threshold, "°C")
        modeSelector.text = Const.thermostatModes[thermostat.mode]
        circularSeekBar.max = 35.toFloat()

        val anim = ValueAnimator.ofFloat(0f, thermostat.threshold)
        anim.duration = 1000
        anim.addUpdateListener { animation: ValueAnimator ->
            val animProgress = animation.animatedValue as Float
            circularSeekBar.progress = animProgress
        }
        anim.start()
        circularSeekBar.setOnSeekBarChangeListener(object : OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBar, progress: Float, fromUser: Boolean) {
                tempSet.text = String.format(Locale.ENGLISH, "%.1f°C", progress)
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar) {
                thermostat.setThermostat(2, circularSeekBar.progress)
                modeSelector.text = Const.thermostatModes[2]
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar) {}
        })
        modeSelector.setOnClickListener {
            when (modeSelector.text.toString()) {
                "Thermostat on" -> thermostat.setThermostat(2, circularSeekBar.progress)
                "Thermostat off" -> thermostat.setThermostat(1, circularSeekBar.progress)
                "Thermostat auto" -> thermostat.setThermostat(0, circularSeekBar.progress)
            }
            modeSelector.text = Const.thermostatModes[thermostat.mode]
        }
    }

    private fun updateDeviceName(newName: String) {
        thermostat.name = newName
        val dbHelper = getInstance(applicationContext)
        val db = dbHelper!!.writableDatabase
        val values = ContentValues()
        values.put(DatabaseHelper.DEVICE_NAME, newName)
        val selection = DatabaseHelper.DEVICE_id + "= ?"
        val args = arrayOf(thermostat.id.toString())
        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        )
        db.close()
    }

    private fun updateDeviceDescription(newDescr: String) {
        thermostat.description = newDescr
        val dbHelper = getInstance(applicationContext)
        val db = dbHelper!!.writableDatabase
        val values = ContentValues()
        values.put(DatabaseHelper.DEVICE_INFO, newDescr)
        val selection = DatabaseHelper.DEVICE_id + "= ?"
        val args = arrayOf(thermostat.id.toString())
        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        )
        db.close()
    }
}