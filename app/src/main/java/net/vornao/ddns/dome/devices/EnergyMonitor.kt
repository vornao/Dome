package net.vornao.ddns.dome.devices
import net.vornao.ddns.dome.handler.DeviceHandler

class EnergyMonitor(id: Int, type: String, handler: DeviceHandler, home_id: String, name: String) : Device(id, type, handler, home_id, name) {
    var power = 0f
     set(value) {
         field = value
         emonCallback?.invoke()
     }


    var energy = 0f
    var emonCallback: (()->Unit)? = null
    var dataUrl: String? = null

    override fun toString(): String {
        return "EnergyMonitor{" +
                "power=" + power +
                ", energy=" + energy +
                ", dataUrl='" + dataUrl + '\'' +
                '}'
    }
}

