package net.vornao.ddns.dome.activities

import net.vornao.ddns.dome.handler.DatabaseHelper.Companion.getInstance
import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.os.Bundle
import net.vornao.ddns.dome.R
import android.widget.TextView
import android.widget.EditText
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.android.volley.VolleyError
import me.tankery.lib.circularseekbar.CircularSeekBar
import net.vornao.ddns.dome.devices.EnergyMonitor
import android.widget.ProgressBar
import net.vornao.ddns.dome.fragments.BottomSheetFragment
import com.google.android.material.snackbar.Snackbar
import android.animation.ValueAnimator
import android.view.inputmethod.EditorInfo
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import org.json.JSONException
import net.vornao.ddns.dome.handler.DatabaseHelper
import android.content.ContentValues
import android.widget.LinearLayout
import net.vornao.ddns.dome.views.ChartBarDrawer
import android.graphics.Typeface
import android.util.Log
import android.view.*
import androidx.preference.PreferenceManager
import com.android.volley.Request
import net.vornao.ddns.dome.shared.Const
import java.lang.AssertionError
import java.text.SimpleDateFormat
import java.util.*

class EnergyMonitorActivity : AppCompatActivity() {
    private lateinit var requestQueue: RequestQueue
    private lateinit var deviceName: EditText
    private lateinit var deviceDesc: EditText
    private lateinit var energy: TextView
    private lateinit var power: TextView
    private lateinit var impact: TextView
    private lateinit var powerSeekBar: CircularSeekBar
    private lateinit var energyMonitor: EnergyMonitor
    private lateinit var loadingCircle: ProgressBar
    private lateinit var weeklyLoadingCircle: ProgressBar
    private var firstStart = true
    private val energyConsumptions = FloatArray(7)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.emon_actionbar_menu, menu)
        return true
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                energy.visibility = View.INVISIBLE
                loadingCircle.visibility = View.VISIBLE
                energyFromData
                true
            }
            R.id.action_details -> {
                val b = BottomSheetFragment()
                val bundle = Bundle()
                bundle.putString(Const.DEVICE_ID, energyMonitor.id.toString())
                bundle.putString(Const.MQTT_TOPIC_C, energyMonitor.home_id)
                b.arguments = bundle
                b.show(supportFragmentManager, "bsfd_fragment")
                true
            }
            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emon_activity)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)

        MAX_POWER = sharedPreferences.getString("emon_max_power", "3300")!!.toInt() + 700
        deviceName = findViewById(R.id.emon_name)
        deviceDesc = findViewById(R.id.emon_desc)
        energy = findViewById(R.id.energy_tv)
        energy.visibility = View.INVISIBLE
        power = findViewById(R.id.power_tv)
        impact = findViewById(R.id.impactTextView)
        powerSeekBar = findViewById(R.id.power_seekbar)
        powerSeekBar.isEnabled = false
        loadingCircle = findViewById(R.id.progressbar)
        weeklyLoadingCircle = findViewById(R.id.summaryProgressbar)
        loadingCircle.isIndeterminate = true

        // setup queue for HTTP history requests
        requestQueue = Volley.newRequestQueue(this)
        val intent = intent
        val deviceID = intent.getIntExtra(Const.DEVICE_ID, 0)
        Log.d("EMON_ACTIVITY", deviceID.toString())
        energyMonitor = (MainActivity.devices[deviceID] as EnergyMonitor?)!!
        try {
        } catch (e: AssertionError) {
            Snackbar.make(
                    window.decorView,
                    R.string.generic_connection_error_message,
                    Snackbar.LENGTH_SHORT)
            .show()
        }

        power.text = String.format(Locale.US, "%.1fW", energyMonitor.power)
        powerSeekBar.max = MAX_POWER.toFloat()
        powerSeekBar.isLockEnabled = true // prevent user from touching seekbar
        powerSeekBar.isNegativeEnabled = false


        // fancy animation for value
        val anim: ValueAnimator =
                if (energyMonitor.power > MAX_POWER)
                    ValueAnimator.ofFloat(0f, MAX_POWER.toFloat())
                else
                    ValueAnimator.ofFloat(0f, energyMonitor.power)
        anim.duration = 1000
        anim.addUpdateListener { animation: ValueAnimator ->
            val animProgress = animation.animatedValue as Float
            powerSeekBar.progress = animProgress
        }
        anim.start()
        deviceName.setText(energyMonitor.name)

        if (energyMonitor.description != null)
            deviceDesc.setText(energyMonitor.description)
        else
            deviceDesc.setText(energyMonitor.id.toString())

        energyMonitor.emonCallback = {

            val p = energyMonitor.power
            power.text = String.format(Locale.US, "%.1fW", p)
            impact.text = Const.getEnergyImpactString(p, MAX_POWER.toFloat(), this)

            // we don't know if device is online or offline, wait for first packet to come.
            if (firstStart) {
                firstStart = false
                energyFromData
            }

            if (energyMonitor.power > MAX_POWER)
                anim.setFloatValues(powerSeekBar.progress, MAX_POWER.toFloat())
            else
                anim.setFloatValues(powerSeekBar.progress, energyMonitor.power)

            anim.end()
            anim.start()
        }

        deviceName.setOnEditorActionListener { _: TextView?, i: Int, _: KeyEvent? ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                Log.i("EDITORACTION", deviceName.text.toString())
                updateDeviceName(deviceName.text.toString())
                deviceName.clearFocus()
                return@setOnEditorActionListener false
            }
            true
        }


        deviceDesc.setOnEditorActionListener { _: TextView?, i: Int, _: KeyEvent? ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                Log.i("EDITORACTION", deviceDesc.text.toString())
                updateDeviceDescription(deviceDesc.text.toString())
                deviceDesc.clearFocus()
                return@setOnEditorActionListener false
            }
            true
        }
    } //TODO: Handle error

    /**
     * Request today energy from AWS server
     * When done, request weekly report from api
     */
    private val energyFromData: Unit
        get() {
            val c = Calendar.getInstance()
            val request = JsonObjectRequest(
                    Request.Method.GET, energyMonitor.dataUrl + "/current-energy", null,
                    { response: JSONObject ->
                        try {
                            Log.d("VOLLEY", response.toString())
                            val body = response.getJSONObject("body")
                            val currentEnergy = body.getDouble("energy").toFloat()
                            loadingCircle.visibility = View.INVISIBLE
                            energy.text = String.format(Locale.US, "%.2f kWh", currentEnergy)
                            // update today value!
                            energyConsumptions[Const.realWeek[c[Calendar.DAY_OF_WEEK]]!! - 1] = currentEnergy
                            energy.visibility = View.VISIBLE
                            energyHistory
                        } catch (e: JSONException) {
                            Log.d("VOLLEY", "JsonError")
                            e.printStackTrace()
                        }
                    }
            ) {
                // TODO: Handle error
                Log.d("VOLLEY", "ErrorRequest")
            }
            requestQueue.add(request)
        }

    /**
     * Database utils when user changes name
     * @param newName
     */
    private fun updateDeviceName(newName: String) {
        energyMonitor.name = newName
        val dbHelper = getInstance(applicationContext)
        val db = dbHelper!!.writableDatabase
        val values = ContentValues()
        values.put(DatabaseHelper.DEVICE_NAME, newName)
        val selection = DatabaseHelper.DEVICE_id + "= ?"
        val args = arrayOf(energyMonitor.id.toString())
        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        )
        db.close()
    }

    /**
     * same but changes description
     * @param newDescr
     */
    private fun updateDeviceDescription(newDescr: String) {
        energyMonitor.description = newDescr
        val dbHelper = getInstance(applicationContext)
        val db = dbHelper!!.writableDatabase
        val values = ContentValues()
        values.put(DatabaseHelper.DEVICE_INFO, newDescr)
        val selection = DatabaseHelper.DEVICE_id + "= ?"
        val args = arrayOf(energyMonitor.id.toString())
        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        )
        db.close()
    }// increment calendar// Start date


    //first, let's get first day of current week
    // dt is now the new date
    // needed for aws api
    // java calendar util is quite horrifying
    private val energyHistory: Unit
        get() {

            // needed for aws api
            // java calendar util is quite horrifying
            @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd")
            var dt: String? // Start date
            val cal = Calendar.getInstance(Locale.getDefault())
            cal[Calendar.HOUR_OF_DAY] = 0
            cal.clear(Calendar.MINUTE)
            cal.clear(Calendar.SECOND)
            cal.clear(Calendar.MILLISECOND)


            //first, let's get first day of current week
            cal[Calendar.DAY_OF_WEEK] = cal.firstDayOfWeek
            dt = sdf.format(cal.time) // dt is now the new date
            Log.d("CALENDAR", dt)

            for (i in 0..6) {
                val requestBody = JSONObject()
                try {
                    requestBody.put("date", dt)
                    requestBody.put("type", "summary")
                    Log.d("VOLLEY", requestBody.toString())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                val request = JsonObjectRequest(
                        Request.Method.POST, energyMonitor.dataUrl + "history", requestBody,
                        { response: JSONObject ->
                            try {
                                val body = response.getJSONObject("body").getJSONObject("Item")
                                weeklyLoadingCircle.visibility = View.INVISIBLE
                                energyConsumptions[i] = body.getDouble("kwh").toFloat()
                                Log.d("VOLLEY", body.getString("kwh"))
                                if (i == 6) plotChart()
                            } catch (e: JSONException) {
                                Log.d("VOLLEY", "JsonError")
                                if (i == 6) plotChart()
                                e.printStackTrace()
                            }
                        }
                ) {
                    Log.d("VOLLEY", "ErrorRequest")
                    if (i == 6) plotChart()
                }

                requestQueue.add(request)

                // increment calendar
                cal.add(Calendar.DATE, 1)
                dt = sdf.format(cal.time)
            }
        }

    private fun createHistoryBarElem(`val`: Float, dayOfWeek: Int) {
        val ll = findViewById<LinearLayout>(R.id.summaryLayout)
        val dowtv = TextView(this)
        val cbd = ChartBarDrawer(this, `val`)
        val dayLayout = LinearLayout(this)
        val dayEnergyTv = TextView(this)

        dayEnergyTv.text = String.format(getString(R.string.chartbar_kwh), `val`)
        dayLayout.orientation = LinearLayout.HORIZONTAL
        dowtv.text = Const.daysOfWeek[dayOfWeek]
        dowtv.setPadding(energy.paddingLeft, energy.paddingLeft / 3, 0, energy.paddingLeft / 4)
        cbd.setPadding(energy.paddingLeft, energy.paddingTop, 0, energy.paddingLeft)
        dayEnergyTv.setBackgroundColor(resources.getColor(android.R.color.transparent, theme))


        // if we're plotting today make it bold
        if (dayOfWeek + 1 == Const.realWeek[Calendar.getInstance()[Calendar.DAY_OF_WEEK]]) {
            dowtv.setTypeface(null, Typeface.BOLD)
            dayEnergyTv.setTypeface(null, Typeface.BOLD)
        }

        // once measures are defined ->
        dowtv.post {
            val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // compute position for rect
            val rectMargin = ((this.window.decorView.width - energy.paddingLeft) * `val` / 15).toInt()
            if (rectMargin > dowtv.measuredWidth + 40) {
                params.setMargins(rectMargin - dayEnergyTv.width / 2 - dowtv.measuredWidth,
                        0,
                        0,
                        0)
                dayEnergyTv.layoutParams = params
                dayLayout.addView(dayEnergyTv)
            } else {
                params.setMargins(40, 0, 0, 0)
            }
        }
        dayLayout.addView(dowtv)
        ll.addView(dayLayout)
        ll.addView(cbd)
    }

    // for each value in array, plot bar with custom view.
    // if old views are there, discard them
    private fun plotChart() {
        (findViewById<View>(R.id.summaryLayout) as LinearLayout).removeAllViews()
        weeklyLoadingCircle.visibility = View.GONE
        for (i in 0..6) {
            createHistoryBarElem(energyConsumptions[i], i)
        }
    }

    companion object {
        private var MAX_POWER = 4000
    }
}