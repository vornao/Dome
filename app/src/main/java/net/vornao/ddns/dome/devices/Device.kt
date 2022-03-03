package net.vornao.ddns.dome.devices

import net.vornao.ddns.dome.handler.DeviceHandler

open class Device(var id: Int, var type: String, protected val handler: DeviceHandler, var home_id: String, var name: String) : Comparable<Device?> {
    protected val pubTopic: String
    protected val subTopic: String
    fun setOnline() {
        isOffline = false
    }

    fun setOffline() {
        isOffline = true
    }

    var isOffline = true
        protected set
    var viewPosition = 0
    var description: String? = null

    override fun compareTo(other: Device?): Int {
        return viewPosition - other?.viewPosition!!
    }

    init {
        subTopic = "dome/$home_id"
        pubTopic = "dome/$home_id/$id"
    }
}