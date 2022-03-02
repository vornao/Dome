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
import android.view.View
import net.vornao.ddns.dome.activities.AboutActivity
import net.vornao.ddns.dome.shared.Const
import java.lang.AssertionError

class BottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.device_details_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TextView deviceIP = view.findViewById(R.id.ddevice_ip);
        val deviceName = view.findViewById<TextView>(R.id.ddevice_name)
        val devicePubTopic = view.findViewById<TextView>(R.id.ddevice_pub_topic)
        val deviceSubTopic = view.findViewById<TextView>(R.id.ddevice_sub_topic)

        // deviceIP.setText(String.format(getString(R.string.ip_address_info), "192.168.173.34"));
        try {
            assert(arguments != null)
        } catch (e: AssertionError) {
            return
        }
        deviceName.text = String.format(getString(R.string.device_id_info), requireArguments().getString(Const.DEVICE_ID))
        deviceSubTopic.text = String.format(getString(R.string.device_sub_info), requireArguments().getString(Const.MQTT_TOPIC_C), requireArguments().getString(Const.DEVICE_ID))
        devicePubTopic.text = String.format(getString(R.string.device_pub_info), requireArguments().getString(Const.MQTT_TOPIC_C))
    }
}