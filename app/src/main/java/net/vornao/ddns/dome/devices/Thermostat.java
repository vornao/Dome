package net.vornao.ddns.dome.devices;

import androidx.annotation.NonNull;

import net.vornao.ddns.dome.handler.DeviceHandler;

import java.util.Locale;

public class Thermostat extends Device{

    private int mode;
    private float threshold;
    private float temp;
    private float hum;

    public Thermostat(int id, String type, DeviceHandler handler, String home_id, String name) {
        super(id, type, handler, home_id, name);
    }

    public void setThermostat(int mode, float temp){
        String msg = String.format(Locale.ENGLISH, "{\"mode\":%d, \"threshold\":%.1f}", mode, temp);
        this.mode = mode;
        this.threshold = temp;
        handler.publish(pubTopic, msg, 2);
    }

    public void setTemp(float temp){
        this.temp = temp;
    }


    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public float getTemp() {
        return temp;
    }

    public float getHum() {
        return hum;
    }

    @NonNull
    @Override
    public String toString() {
        return "Thermostat{" +
                "mode=" + mode +
                ", threshold=" + threshold +
                ", temp=" + temp +
                ", hum=" + hum +
                '}';
    }

    public void setHum(float hum) {
        this.hum = hum;
    }

}
