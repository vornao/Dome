package net.vornao.ddns.dome.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.vornao.ddns.dome.callbacks.EnergyMonitorUpdatedCallback;
import net.vornao.ddns.dome.handler.DeviceHandler;

public class EnergyMonitor extends Device{

    private float power;
    private float energy;


    @Nullable
    private EnergyMonitorUpdatedCallback emonCallback = null;
    private String dataUrl;

    public EnergyMonitor(int id, String type, DeviceHandler handler, String home_id, String name) {
        super(id, type, handler, home_id, name);
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
        if(emonCallback != null) emonCallback.onUpdate();
    }

    public float getEnergy() {
        return energy;
    }

    public void setEnergy(float energy) {
        this.energy = energy;
    }

    public String getDataUrl() {
        return dataUrl;
    }

    public void setDataUrl(String dataUrl) {
        this.dataUrl = dataUrl;
    }

    @Nullable
    public EnergyMonitorUpdatedCallback getEmonCallback() {
        return emonCallback;
    }

    public void setEmonCallback(EnergyMonitorUpdatedCallback emonCallback) {
        this.emonCallback = emonCallback;
    }

    @NonNull
    @Override
    public String toString() {
        return "EnergyMonitor{" +
                "power=" + power +
                ", energy=" + energy +
                ", dataUrl='" + dataUrl + '\'' +
                '}';
    }
}
