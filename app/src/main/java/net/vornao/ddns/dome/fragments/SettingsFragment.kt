package net.vornao.ddns.dome.fragments

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import net.vornao.ddns.dome.R
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import android.widget.LinearLayout
import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.EditTextPreference.OnBindEditTextListener
import android.widget.EditText
import android.text.InputType
import android.content.Intent
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import net.vornao.ddns.dome.activities.AboutActivity

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val editTextPreference = findPreference<EditTextPreference>("mqtt_password")

        editTextPreference?.setOnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val MQTT_SERVER = sharedPreferences.getString("broker", "Select MQTT server name")
        val MQTT_PORT = sharedPreferences.getString("broker_port", "Default: 1883")
        val MQTT_USERNAME = sharedPreferences.getString("mqtt_username", "Select MQTT username")
        val MQTT_TOPIC = sharedPreferences.getString("mqtt-topic", null)

        val serverPreference = findPreference<EditTextPreference>("broker")

        if (serverPreference != null) {
            serverPreference.summary = MQTT_SERVER
        }

        val userPreference = findPreference<EditTextPreference>("mqtt_username")

        if (userPreference != null) {
            userPreference.summary = MQTT_USERNAME
        }
        val portPreference = findPreference<EditTextPreference>("broker_port")

        if (portPreference != null) {
            portPreference.summary = MQTT_PORT
        }
        val topicPreference = findPreference<EditTextPreference>("mqtt-topic")

        if (topicPreference != null) {
            topicPreference.summary = MQTT_TOPIC
        }

        val powerPreference = findPreference<EditTextPreference>("emon_max_power")!!

        powerPreference.setOnBindEditTextListener { editText: EditText -> editText.inputType = InputType.TYPE_CLASS_NUMBER }

        val feedbackPreference = findPreference<Preference>("feedback")!!

        feedbackPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val i = Intent(Intent.ACTION_SENDTO)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_EMAIL, arrayOf("l.miglior@studenti.unipi.it"))
            i.putExtra(Intent.EXTRA_SUBJECT, "[Dome] Application feedback")
            startActivity(i)
            true
        }

        val aboutPreference = findPreference<Preference>("about")!!
        aboutPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val startAbout = Intent(context, AboutActivity::class.java)
            startActivity(startAbout)
            false
        }
    }
}