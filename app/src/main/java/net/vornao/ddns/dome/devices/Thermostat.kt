package net.vornao.ddns.dome.devices
import net.vornao.ddns.dome.handler.DeviceHandler
import java.util.*

class Thermostat(id: Int, type: String, handler: DeviceHandler, home_id: String, name: String) : Device(id, type, handler, home_id, name) {
    var mode = 0
    var threshold = 0f
    var temp = 0f
    var hum = 0f

    fun setThermostat(mode: Int, temp: Float) {
        val msg = String.format(Locale.ENGLISH, "{\"mode\":%d, \"threshold\":%.1f}", mode, temp)
        this.mode = mode
        threshold = temp
        handler.publish(pubTopic, msg, 2)
    }

    override fun toString(): String {
        return "Thermostat{" +
                "mode=" + mode +
                ", threshold=" + threshold +
                ", temp=" + temp +
                ", hum=" + hum +
                '}'
    }
}