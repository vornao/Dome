package net.vornao.ddns.dome.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.vornao.ddns.dome.R
import com.android.volley.toolbox.Volley
import com.android.volley.toolbox.StringRequest
import com.android.volley.VolleyError
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.android.volley.Request
import net.vornao.ddns.dome.shared.Const

class DeviceSetupActivity : AppCompatActivity() {
    private var currentState = 0
    private var step0: TextView? = null
    private var step1: TextView? = null
    private var step2: TextView? = null
    private var step3: TextView? = null
    private var ssid: EditText? = null
    private var pass: EditText? = null
    private var buttonNext: Button? = null
    private var buttonConf: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_device_layout)
        supportActionBar?.title = "Device setup"
        step0 = findViewById(R.id.step0)
        step1 = findViewById(R.id.step1)
        step2 = findViewById(R.id.step2)
        step3 = findViewById(R.id.step3)
        ssid = findViewById(R.id.wifiSSID)
        pass = findViewById(R.id.wifipass)
        buttonNext = findViewById(R.id.buttonNext)
        buttonConf = findViewById(R.id.buttonConfigure)
        buttonNext?.setOnClickListener {
            currentState++
            changeView(currentState)
        }
        buttonConf?.setOnClickListener { sendConfiguration() }
    }

    //fancy text change when next button pressed
    private fun changeView(state: Int) {
        when (state) {
            1 -> {
                step0!!.visibility = View.GONE
                step1!!.visibility = View.VISIBLE
            }
            2 -> {
                step1!!.visibility = View.GONE
                step2!!.visibility = View.VISIBLE
                ssid!!.visibility = View.VISIBLE
                pass!!.visibility = View.VISIBLE
            }
            3 -> {
                step2!!.visibility = View.GONE
                step3!!.visibility = View.VISIBLE
                ssid!!.visibility = View.GONE
                pass!!.visibility = View.GONE
                buttonConf!!.visibility = View.VISIBLE
                buttonNext!!.visibility = View.GONE
            }
            else -> {
            }
        }
    }

    private fun sendConfiguration() {
        val pm = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val topic = pm.getString(Const.MQTT_TOPIC_C, "")
        val name = ssid!!.text.toString()
        val pwd = pass!!.text.toString()
        val queue = Volley.newRequestQueue(applicationContext)
        val urlConfig = String.format( // cleartraffic allowed only for this domain on network preferences
                "http://192.168.1.1?ssid=%s&password=%s&home-uuid=%s",  // fixed uri to arduino board
                name,
                pwd,
                topic)
        val request = StringRequest(Request.Method.GET, urlConfig,
                {
                    Log.d("DEVCONF", "SUCCESS")
                    val builder = AlertDialog.Builder(this@DeviceSetupActivity)
                    builder.setTitle(R.string.success_msg)
                            .setMessage(R.string.success_info)
                            .setNeutralButton(R.string.great_button, null)
                            .show()
                }
        ) { error: VolleyError ->
            Log.d("DEVCONF", "msg: " + error.message)
            val builder = AlertDialog.Builder(this@DeviceSetupActivity)
            builder.setTitle(R.string.configuration_failed)
                    .setMessage(R.string.generic_connection_error_message)
                    .setNeutralButton(R.string.try_again, null)
                    .show()
        }
        queue.add(request)
    }
}