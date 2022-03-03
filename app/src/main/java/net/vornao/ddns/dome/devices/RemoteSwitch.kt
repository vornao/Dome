package net.vornao.ddns.dome.devices

import net.vornao.ddns.dome.handler.DeviceHandler

class RemoteSwitch(id: Int, type: String, handler: DeviceHandler, home_id: String, name: String) : Device(id, type, handler, home_id, name) {
    var mode = 0
    fun setOn() {
        val msg = "{\"mode\":1}"
        super.handler.publish(super.pubTopic, msg, 2)
        mode = 1
    }

    fun setOff() {
        val msg = "{\"mode\":0}"
        super.handler.publish(super.pubTopic, msg, 2)
        mode = 0
    }

    override fun toString(): String {
        return "RemoteSwitch{" +
                "mode=" + mode +
                '}'
    }
}