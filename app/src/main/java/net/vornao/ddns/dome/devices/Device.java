package net.vornao.ddns.dome.devices;

import androidx.annotation.NonNull;

import net.vornao.ddns.dome.handler.DeviceHandler;

public class Device implements Comparable<Device>{

    protected int id;
    protected String type;
    protected String name;
    protected String home_id;
    protected final DeviceHandler handler;
    @NonNull
    protected final String pubTopic;
    @NonNull
    protected final String subTopic;

    public boolean isOffline() {
        return isOffline;
    }

    public void setOnline(){
        isOffline = false;
    }

    public void setOffline(){
        isOffline = true;
    }

    protected boolean isOffline = true;

    public int getViewPosition() {
        return viewPosition;
    }

    public void setViewPosition(int viewPosition) {
        this.viewPosition = viewPosition;
    }

    protected int viewPosition = 0;

    protected String description;

    public Device(int id, String type, DeviceHandler handler, String home_id, String name) {
        this.id = id;
        this.type = type;
        this.handler = handler;
        this.home_id = home_id;
        this.name = name;
        this.subTopic = "dome/" + home_id;
        this.pubTopic = "dome/" + home_id + "/" + id;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHome_id() {
        return home_id;
    }

    public void setHome_id(String home_id) {
        this.home_id = home_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int compareTo(@NonNull Device device) {
        return this.getViewPosition() -device.getViewPosition();
    }
}
