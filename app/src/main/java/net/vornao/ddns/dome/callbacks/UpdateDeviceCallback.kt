package net.vornao.ddns.dome.callbacks

interface UpdateDeviceCallback {
    fun onSuccess()
    fun onFailure()
    fun onConnSuccess()
    fun onSuccess(pos: Int)
    fun onNewDevice(id: Int)
}