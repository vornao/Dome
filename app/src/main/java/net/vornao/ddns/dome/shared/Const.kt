package net.vornao.ddns.dome.shared

import android.content.Context
import net.vornao.ddns.dome.R
import java.util.HashMap

object Const {
    const val DEVICE_ID = "device-id"
    const val DEVICE_TYPE = "device-type"
    const val REMOTE_SWITCH = "remote-switch"
    const val THERMOSTAT = "thermostat"
    const val ENERGY_MONITOR = "energy-monitor"
    const val STATUS_KEY = "current-status"
    const val SWITCH_CHANGE_KEY = "mode"
    const val THERM_TEMP_KEY = "temperature"
    const val THERM_HUM_KEY = "humidity"
    const val THERM_THRESHOLD_KEY = "current-threshold"
    const val EMON_DATA_PATH_KEY = "data-path"
    const val EMON_POWER = "current-power"

    //config const
    const val MQTT_TOPIC_C = "mqtt-topic"
    const val MQTT_BROKER_C = "broker"
    const val MQTT_USER_C = "mqtt_username"
    const val MQTT_PASS_C = "mqtt_password"
    const val MQTT_PORT_C = "broker_port"
    val deviceType: HashMap<String?, Int?> = object : HashMap<String?, Int?>() {
        init {
            put(THERMOSTAT, 0)
            put(ENERGY_MONITOR, 1)
            put(REMOTE_SWITCH, 2)
        }
    }
    val thermostatModes: HashMap<Int?, String?> = object : HashMap<Int?, String?>() {
        init {
            put(0, "Thermostat off")
            put(1, "Thermostat on")
            put(2, "Thermostat auto")
        }
    }
    val daysOfWeek: HashMap<Int?, String?> = object : HashMap<Int?, String?>() {
        init {
            put(0, "Monday")
            put(1, "Tuesday")
            put(2, "Wednesday")
            put(3, "Thursday")
            put(4, "Friday")
            put(5, "Saturday")
            put(6, "Sunday")
        }
    }

    fun getEnergyImpactString(p: Float, MAX_POWER: Float, context: Context): String {
        return if (p < MAX_POWER / 4) context.resources.getString(R.string.low_impact) else if (p > MAX_POWER / 4 && p < MAX_POWER / 2) context.resources.getString(R.string.medium_impact) else if (p > MAX_POWER / 2 && p < (MAX_POWER / 1.4).toFloat()) context.resources.getString(R.string.high_impact) else context.resources.getString(R.string.very_high_impact)
    }

    val realWeek: HashMap<Int?, Int?> = object : HashMap<Int?, Int?>() {
        init {
            put(1, 7)
            put(2, 1)
            put(3, 2)
            put(4, 3)
            put(5, 4)
            put(6, 5)
            put(7, 6)
        }
    }
}