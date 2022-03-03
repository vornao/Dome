package net.vornao.ddns.dome.handler

import android.content.Context
import android.util.Log

import net.vornao.ddns.dome.callbacks.UpdateDeviceCallback
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttCallback
import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import com.google.gson.JsonObject
import net.vornao.ddns.dome.devices.Device
import net.vornao.ddns.dome.devices.Thermostat
import net.vornao.ddns.dome.devices.RemoteSwitch
import net.vornao.ddns.dome.devices.EnergyMonitor
import net.vornao.ddns.dome.shared.Const
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import java.lang.Exception
import java.lang.NullPointerException
import java.util.*

class DeviceHandler(context: Context?, server: String?, username: String?, pass: String,
                    devices: HashMap<Int, Device>, private var house_id: String, udc: UpdateDeviceCallback) {
    private val client: MqttAndroidClient = MqttAndroidClient(context, server, MqttClient.generateClientId())
    private val mqttConnectOptions: MqttConnectOptions
    private val callback: MqttCallback
    private val deviceHandler: DeviceHandler
    var houseTopic: String

    private val updateDeviceCallback: UpdateDeviceCallback = udc
    private val gson = Gson()
    fun connect() {
        try {
            val token = client.connect(mqttConnectOptions)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.i("MQTT", "SUCCESS")
                    Log.d("MQTT", houseTopic)
                    subscribe(houseTopic)
                    client.setCallback(callback)
                    updateDeviceCallback.onConnSuccess()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.i("MQTT", "FAIL")
                    updateDeviceCallback.onFailure()
                }
            }
        } catch (e: MqttException) {
            Log.i("MQTT", "ERROR CONNECTING TO MQTT BROKER")
            e.printStackTrace()
        }
    }

    fun close() {
        try {
            //client.disconnect();
            client.close()
        } catch (e: Exception) {
            Log.d("MQTT", Arrays.toString(e.stackTrace))
        }
    }

    fun publish(topic: String?, payload: String, qos: Int) {
        try {
            client.publish(topic, MqttMessage(payload.toByteArray()))
        } catch (e: MqttException) {
            Log.i("MQTT", e.localizedMessage)
        } catch (e: NullPointerException) {
            Log.i("MQTT", e.localizedMessage)
        }
    }

    fun subscribe(topic: String?) {
        try {
            client.subscribe(topic, 0)
        } catch (e: MqttException) {
            Log.i("MQTT", "FAILED TO SUBSCRIBE")
        } catch (e: NullPointerException) {
            Log.i("MQTT", "FAILED TO SUBSCRIBE")
        }
    }

    private fun addThermostat(id: Int, recv: JsonObject) {
        devices[id] = Thermostat(id, Const.THERMOSTAT, deviceHandler, house_id, "Thermostat")
        updateDeviceCallback.onNewDevice(id)
        updateThermostat(id, recv)
    }

    private fun addRemoteSwitch(id: Int, recv: JsonObject) {
        devices[id] = RemoteSwitch(id, Const.REMOTE_SWITCH, deviceHandler, house_id, "Remote switch")
        updateRemoteSwitch(id, recv)
        updateDeviceCallback.onNewDevice(id)
    }

    private fun addEnergyMonitor(id: Int, recv: JsonObject) {
        devices[id] = EnergyMonitor(id, Const.ENERGY_MONITOR, deviceHandler, house_id, "Power meter")
        updateDeviceCallback.onNewDevice(id)
        updateEnergyMonitor(id, recv)
    }

    private fun updateThermostat(id: Int, recv: JsonObject) {
        (devices[id] as Thermostat?)!!.hum = recv[Const.THERM_HUM_KEY].asFloat
        (devices[id] as Thermostat?)!!.temp = recv[Const.THERM_TEMP_KEY].asFloat
        (devices[id] as Thermostat?)!!.threshold = recv[Const.THERM_THRESHOLD_KEY].asFloat
        (devices[id] as Thermostat?)!!.mode = recv[Const.STATUS_KEY].asInt
        devices[id]!!.setOnline()
    }

    private fun updateRemoteSwitch(id: Int, recv: JsonObject) {
        (devices[id] as RemoteSwitch?)!!.mode = recv[Const.STATUS_KEY].asInt
        devices[id]!!.setOnline()
    }

    private fun updateEnergyMonitor(id: Int, recv: JsonObject) {
        (devices[id] as EnergyMonitor?)!!.dataUrl = recv[Const.EMON_DATA_PATH_KEY].asString
        (devices[id] as EnergyMonitor?)!!.power = recv[Const.EMON_POWER].asFloat
        devices[id]!!.setOnline()
    }


    companion object {
        var devices = HashMap<Int, Device>()
    }

    init {
        Companion.devices = devices
        houseTopic = "dome/$house_id"
        Log.i("MQTT", houseTopic)
        deviceHandler = this
        mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.userName = username
        mqttConnectOptions.password = pass.toCharArray()
        callback = object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                Log.i("MQTT", "CONNLOST")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val recv = gson.fromJson(String(message.payload), JsonObject::class.java)
                val type = recv[Const.DEVICE_TYPE].asString
                val tempId = recv[Const.DEVICE_ID].asInt
                Log.d("MQTT", houseTopic)
                if (topic == houseTopic) {
                    if (!devices.containsKey(tempId)) {
                        when (type) {
                            Const.THERMOSTAT -> addThermostat(tempId, recv)
                            Const.REMOTE_SWITCH -> addRemoteSwitch(tempId, recv)
                            Const.ENERGY_MONITOR -> addEnergyMonitor(tempId, recv)
                        }
                    } else {
                        when (type) {
                            Const.THERMOSTAT -> updateThermostat(tempId, recv)
                            Const.REMOTE_SWITCH -> updateRemoteSwitch(tempId, recv)
                            Const.ENERGY_MONITOR -> updateEnergyMonitor(tempId, recv)
                        }
                    }
                    updateDeviceCallback.onSuccess(tempId)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        }
    }
}