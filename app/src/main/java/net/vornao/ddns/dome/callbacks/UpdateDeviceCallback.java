package net.vornao.ddns.dome.callbacks;

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface UpdateDeviceCallback {
    void onSuccess();
    void onFailure();
    void onConnSuccess();
    void onSuccess(int pos);
    public void onNewDevice(int id);
}
