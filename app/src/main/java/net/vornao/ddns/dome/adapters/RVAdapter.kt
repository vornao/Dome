package net.vornao.ddns.dome.adapters

import net.vornao.ddns.dome.handler.DatabaseHelper.Companion.getInstance
import androidx.recyclerview.widget.RecyclerView
import net.vornao.ddns.dome.callbacks.UpdatesCallback
import androidx.cardview.widget.CardView
import android.widget.TextView
import net.vornao.ddns.dome.R
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import android.widget.EditText
import android.view.ViewGroup
import android.view.LayoutInflater
import net.vornao.ddns.dome.adapters.RVAdapter.ThermostatViewHolder
import net.vornao.ddns.dome.adapters.RVAdapter.EnergyMonitorViewHolder
import net.vornao.ddns.dome.adapters.RVAdapter.RemoteSwitchViewHolder
import net.vornao.ddns.dome.devices.Thermostat
import android.content.Intent
import net.vornao.ddns.dome.activities.ThermostatActivity
import net.vornao.ddns.dome.devices.EnergyMonitor
import net.vornao.ddns.dome.activities.EnergyMonitorActivity
import net.vornao.ddns.dome.devices.RemoteSwitch
import android.view.View.OnFocusChangeListener
import android.widget.TextView.OnEditorActionListener
import android.view.inputmethod.EditorInfo
import net.vornao.ddns.dome.fragments.BottomSheetFragment
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.vornao.ddns.dome.handler.DatabaseHelper
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.View
import net.vornao.ddns.dome.devices.Device
import net.vornao.ddns.dome.shared.Const
import java.util.ArrayList

class RVAdapter(private val deviceList: ArrayList<Device>, private val MAX_POWER: Int, private val rv: RecyclerView, private val updatesCallback: UpdatesCallback, private val parentContext: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class EnergyMonitorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cv: CardView = itemView.findViewById(R.id.emon_cv)
        val name: TextView = itemView.findViewById(R.id.emon_name)
        val power: TextView = itemView.findViewById(R.id.current_power)
        val energy: TextView = itemView.findViewById(R.id.current_energy)
        val offline: TextView = itemView.findViewById(R.id.offline_emon)

    }

    class ThermostatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cv: CardView = itemView.findViewById(R.id.thermostat_cardview)
        val name: TextView = itemView.findViewById(R.id.device_name)
        val temperature: TextView = itemView.findViewById(R.id.device_temp)
        val humidity: TextView = itemView.findViewById(R.id.device_hum)
        val offline: TextView = itemView.findViewById(R.id.offline_therm)
        val linearLayout: LinearLayout = itemView.findViewById(R.id.thermostat_card_ll)

    }

    class RemoteSwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cv: CardView = itemView.findViewById(R.id.remote_switch_cardview)
        val toggle: SwitchCompat = itemView.findViewById(R.id.rs_toggle)
        val name: EditText = itemView.findViewById(R.id.switch_name)
        val desc: EditText = itemView.findViewById(R.id.switch_desc)
        val offline: TextView = itemView.findViewById(R.id.offline_switch)

    }

    override fun getItemViewType(position: Int): Int {
        return Const.deviceType[deviceList[position].type]!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val v0 = LayoutInflater.from(parent.context).inflate(R.layout.thermostat_card, parent, false)
                ThermostatViewHolder(v0)
            }
            1 -> {
                val v1 = LayoutInflater.from(parent.context).inflate(R.layout.emon_card, parent, false)
                EnergyMonitorViewHolder(v1)
            }
            else -> {
                val v3 = LayoutInflater.from(parent.context).inflate(R.layout.remote_switch_card, parent, false)
                RemoteSwitchViewHolder(v3)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        when (holder.itemViewType) {
            0 -> {
                val thermostat = deviceList.get(pos) as Thermostat
                val tvh = holder as ThermostatViewHolder
                tvh.name.text = thermostat.name
                tvh.name.setTextColor(parentContext.getColor(R.color.light_grey))
                if (!thermostat.isOffline) {
                    tvh.name.setTextColor(parentContext.getColor(R.color.black))
                    tvh.offline.visibility = View.GONE
                    tvh.temperature.visibility = View.VISIBLE
                    tvh.humidity.visibility = View.VISIBLE
                    tvh.humidity.text = String.format(parentContext.getString(R.string.humidity_cardview), thermostat.hum)
                    tvh.temperature.text = String.format(parentContext.getString(R.string.temperature_cardview), thermostat.temp)
                }
                holder.cv.setOnClickListener { v: View ->
                    val intent = Intent(v.context, ThermostatActivity::class.java)
                    intent.putExtra(Const.DEVICE_ID, thermostat.id)
                    v.context.startActivity(intent)
                }
            }
            1 -> {
                val evh = holder as EnergyMonitorViewHolder
                val energyMonitor = deviceList[pos] as EnergyMonitor
                evh.name.text = energyMonitor.name
                evh.name.setTextColor(parentContext.getColor(R.color.light_grey))
                if (!energyMonitor.isOffline) {
                    evh.name.setTextColor(parentContext.getColor(R.color.black))
                    evh.offline.visibility = View.GONE
                    evh.energy.visibility = View.VISIBLE
                    evh.power.visibility = View.VISIBLE
                    evh.power.text = String.format(parentContext.getString(R.string.power_cardview), energyMonitor.power)
                    evh.energy.text = String.format(
                            "%s %s",
                            Const.getEnergyImpactString(
                                    energyMonitor.power,
                                    MAX_POWER.toFloat(),
                                    parentContext
                            ),
                            parentContext
                                    .resources
                                    .getString(R.string.lowercase_energy_impact))
                }
                holder.cv.setOnClickListener { v: View ->
                    val intent = Intent(v.context, EnergyMonitorActivity::class.java)
                    intent.putExtra(Const.DEVICE_ID, deviceList[pos].id)
                    v.context.startActivity(intent)
                }
            }
            else -> {
                val rvh = holder as RemoteSwitchViewHolder
                val remoteSwitch = deviceList[pos] as RemoteSwitch
                rvh.name.setText(remoteSwitch.name)

                if (remoteSwitch.description == null)
                    rvh.desc.setText(remoteSwitch.id.toString())
                else
                    rvh.desc.setText(remoteSwitch.description)

                rvh.name.setTextColor(parentContext.getColor(R.color.light_grey))
                rvh.name.onFocusChangeListener =
                    OnFocusChangeListener {
                            _: View?, b: Boolean -> if (b) updatesCallback.disableUpdates()
                    }

                rvh.name.setOnEditorActionListener { _: TextView?, i: Int, _: KeyEvent? ->
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        Log.i("EDITORACTION", rvh.name.text.toString())
                        updateDeviceName(rvh.name.text.toString(), deviceList[pos])
                        rvh.name.clearFocus()
                        updatesCallback.enableUpdates()
                        return@setOnEditorActionListener false
                    }
                    true
                }

                rvh.desc.onFocusChangeListener =
                    OnFocusChangeListener {
                            view: View?, b: Boolean -> if (b) updatesCallback.disableUpdates()
                    }

                rvh.desc.setOnEditorActionListener { textView: TextView?, i: Int, keyEvent: KeyEvent? ->
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        Log.i("EDITORACTION", rvh.desc.text.toString())
                        updateDeviceDescription(rvh.desc.text.toString(), deviceList[pos])
                        rvh.desc.clearFocus()
                        updatesCallback.enableUpdates()
                        return@setOnEditorActionListener false
                    }
                    true
                }

                if (!remoteSwitch.isOffline) {
                    rvh.name.setTextColor(parentContext.getColor(R.color.black))
                    rvh.desc.setTextColor(parentContext.getColor(R.color.light_grey))
                    rvh.desc.visibility = View.VISIBLE
                    rvh.toggle.visibility = View.VISIBLE
                    rvh.offline.visibility = View.GONE
                }

                if (remoteSwitch.mode == 0) {
                    rvh.toggle.setText(R.string.switch_off)
                    rvh.toggle.isChecked = false
                    rvh.toggle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_outline_lightbulb_24, 0, 0, 0)
                } else {
                    rvh.toggle.isChecked = true
                    rvh.toggle.setText(R.string.switch_on)
                    rvh.toggle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lightbulb, 0, 0, 0)
                }

                rvh.toggle.setOnClickListener { view: View? ->
                    if (remoteSwitch.mode < 1) {
                        remoteSwitch.setOn()
                        rvh.toggle.isChecked = true
                        rvh.toggle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lightbulb, 0, 0, 0)
                    } else {
                        remoteSwitch.setOff()
                        rvh.toggle.isChecked = false
                        rvh.toggle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_outline_lightbulb_24, 0, 0, 0)
                    }
                }

                rvh.cv.setOnClickListener { view: View? ->
                    val b = BottomSheetFragment()
                    val bundle = Bundle()
                    bundle.putString(Const.DEVICE_ID, remoteSwitch.id.toString())
                    bundle.putString(Const.MQTT_TOPIC_C, remoteSwitch.home_id)
                    b.arguments = bundle
                    b.show((parentContext as AppCompatActivity).supportFragmentManager, "bsfd_fragment")
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    private fun updateDeviceName(newName: String, d: Device) {
        d.name = newName
        val dbHelper = getInstance(rv.context)
        val db = dbHelper!!.writableDatabase
        val values = ContentValues()
        values.put(DatabaseHelper.DEVICE_NAME, newName)
        val selection = DatabaseHelper.DEVICE_id + "= ?"
        val args = arrayOf(d.id.toString())
        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        )
        db.close()
    }

    private fun updateDeviceDescription(newDesc: String, d: Device) {
        d.description = newDesc
        val dbHelper = getInstance(rv.context)
        val db = dbHelper!!.writableDatabase
        val values = ContentValues()
        values.put(DatabaseHelper.DEVICE_INFO, newDesc)
        val selection = DatabaseHelper.DEVICE_id + "= ?"
        val args = arrayOf(d.id.toString())
        db.update(
                DatabaseHelper.DEVICE_TABLE,
                values,
                selection,
                args
        )
        db.close()
    }
}