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
import android.util.Log
import android.view.View
import androidx.preference.PreferenceManager
import net.vornao.ddns.dome.activities.AboutActivity
import net.vornao.ddns.dome.shared.Const
import java.lang.NullPointerException
import java.util.HashMap

class HouseSelectionFragment : BottomSheetDialogFragment() {
    private val indexMapping = HashMap<String, Int>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.change_house_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val houses = requireArguments().getStringArray("available-topics")
        if (houses == null) {
            Snackbar.make(requireView(), R.string.generic_connection_error_message, Snackbar.LENGTH_SHORT).show()
            return
        }
        val title = view.findViewById<TextView>(R.id.titleChange)
        val ll = view.findViewById<LinearLayout>(R.id.layoutHouseSelection)
        for (i in houses.indices) {
            val textView = TextView(context)
            textView.text = String.format(getString(R.string.house_selection_text), i, houses[i])
            indexMapping[textView.text.toString()] = i
            textView.textSize = 18f
            textView.background = resources.getDrawable(R.drawable.ripple_teal, requireContext().theme)
            textView.setPaddingRelative(title.paddingLeft, title.paddingTop / 2, 0, title.paddingLeft / 2)
            textView.setOnClickListener { view1: View ->
                val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
                try {
                    sp.edit().putString(Const.MQTT_TOPIC_C, houses[indexMapping[(view1 as TextView).text.toString()]!!]).apply()
                } catch (e: NullPointerException) {
                    Log.d(this.javaClass.name, e.toString())
                } finally {
                    dismiss()
                }
            }
            ll.addView(textView)
        }
    }
}