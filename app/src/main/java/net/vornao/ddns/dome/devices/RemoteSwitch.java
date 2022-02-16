package net.vornao.ddns.dome.devices;

import androidx.annotation.NonNull;

import net.vornao.ddns.dome.handler.DeviceHandler;


public class RemoteSwitch extends Device{

    private int mode;


    public RemoteSwitch(int id, String type, DeviceHandler handler, String home_id, String name) {
        super(id, type, handler, home_id, name);
    }

    public void setOn(){
        String msg = "{\"mode\":1}";
        super.handler.publish(super.pubTopic, msg, 2);
        mode = 1;
    }

    public void setOff(){
        String msg = "{\"mode\":0}";
        super.handler.publish(super.pubTopic, msg, 2);
        mode  = 0;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    @NonNull
    @Override
    public String toString() {
        return "RemoteSwitch{" +
                "mode=" + mode +
                '}';
    }
}
