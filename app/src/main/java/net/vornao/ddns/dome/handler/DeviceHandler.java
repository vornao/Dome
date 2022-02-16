package net.vornao.ddns.dome.handler;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.vornao.ddns.dome.callbacks.UpdateDeviceCallback;
import net.vornao.ddns.dome.shared.Const;
import net.vornao.ddns.dome.devices.Device;
import net.vornao.ddns.dome.devices.EnergyMonitor;
import net.vornao.ddns.dome.devices.RemoteSwitch;
import net.vornao.ddns.dome.devices.Thermostat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.HashMap;

public class DeviceHandler {

    @NonNull
    private final MqttAndroidClient client;
    @NonNull
    private final MqttConnectOptions mqttConnectOptions;
    @NonNull
    private final MqttCallback callback;
    @NonNull
    private final DeviceHandler deviceHandler;

    private String house_id;


    private String houseTopic;
    private final UpdateDeviceCallback updateDeviceCallback;

    private final Gson gson = new Gson();

    @NonNull
    public static HashMap<Integer, Device> devices = new HashMap<>();



    public DeviceHandler(Context context, String server, String username, @NonNull String pass,
                         @NonNull HashMap<Integer, Device> devices, String house_id, UpdateDeviceCallback udc) {

        client = new MqttAndroidClient(context, server, MqttClient.generateClientId());
        updateDeviceCallback = udc;

        this.house_id = house_id;
        DeviceHandler.devices = devices;

        houseTopic = "dome/" + house_id;
        Log.i("MQTT", houseTopic);
        this.deviceHandler = this;

        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(pass.toCharArray());

        callback = new MqttCallback() {

            @Override
            public void connectionLost(Throwable cause) {
                Log.i("MQTT", "CONNLOST");
            }

            @Override
            public void messageArrived(@NonNull String topic, @NonNull MqttMessage message) {

                JsonObject recv = gson.fromJson(new String(message.getPayload()), JsonObject.class);
                String type =  recv.get(Const.DEVICE_TYPE).getAsString();
                int tempId  =  recv.get(Const.DEVICE_ID).getAsInt();

                if (topic.equals(houseTopic)) {

                    if (!devices.containsKey(tempId)) {
                        switch (type) {
                            case Const.THERMOSTAT: addThermostat(tempId, recv); break;
                            case Const.REMOTE_SWITCH: addRemoteSwitch(tempId, recv); break;
                            case Const.ENERGY_MONITOR: addEnergyMonitor(tempId, recv); break;
                        }
                    }else {
                        switch (type) {
                            case Const.THERMOSTAT: updateThermostat(tempId, recv); break;
                            case Const.REMOTE_SWITCH: updateRemoteSwitch(tempId, recv); break;
                            case Const.ENERGY_MONITOR: updateEnergyMonitor(tempId, recv); break;
                        }
                    }
                    updateDeviceCallback.onSuccess(tempId);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        };
    }

    public void connect() {
        try {
            IMqttToken token = client.connect(mqttConnectOptions);
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("MQTT", "SUCCESS");
                    Log.d("MQTT", houseTopic);
                    subscribe(houseTopic);
                    client.setCallback(callback);
                    updateDeviceCallback.onConnSuccess();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i("MQTT", "FAIL");
                    updateDeviceCallback.onFailure();
                }
            });

        } catch (MqttException e) {
            Log.i("MQTT", "ERROR CONNECTING TO MQTT BROKER");
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            IMqttToken token = client.disconnect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {}
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            //client.disconnect();
            client.close();
        } catch (Exception e) {
            Log.d("MQTT", Arrays.toString(e.getStackTrace()));
        }
    }
       

    public void publish(String topic, @NonNull String payload, int qos){
        try {

            client.publish(topic, new MqttMessage(payload.getBytes()));

        }catch (@NonNull MqttException | NullPointerException e){
            Log.i("MQTT", e.getLocalizedMessage());
        }
    }

    public void subscribe(String topic){
        try{
            client.subscribe(topic, 0);
        }
        catch (@NonNull MqttException | NullPointerException e){
            Log.i("MQTT", "FAILED TO SUBSCRIBE");
        }
    }

    private void addThermostat(int id, @NonNull JsonObject recv){
        devices.put(id, new Thermostat(id, Const.THERMOSTAT, deviceHandler, house_id, "Thermostat"));
        updateDeviceCallback.onNewDevice(id);
        updateThermostat(id, recv);
    }

    private void addRemoteSwitch(int id, @NonNull JsonObject recv){
        devices.put(id, new RemoteSwitch(id, Const.REMOTE_SWITCH, deviceHandler, house_id, "Remote switch"));
        updateRemoteSwitch(id, recv);
        updateDeviceCallback.onNewDevice(id);
    }

    private void addEnergyMonitor(int id, @NonNull JsonObject recv){
        devices.put(id, new EnergyMonitor(id, Const.ENERGY_MONITOR, deviceHandler, house_id, "Power meter"));
        updateDeviceCallback.onNewDevice((id));
        updateEnergyMonitor(id, recv);
    }

    private void updateThermostat(int id, @NonNull JsonObject recv){
        ((Thermostat) devices.get(id)).setHum(recv.get(Const.THERM_HUM_KEY).getAsFloat());
        ((Thermostat) devices.get(id)).setTemp(recv.get(Const.THERM_TEMP_KEY).getAsFloat());
        ((Thermostat) devices.get(id)).setThreshold(recv.get(Const.THERM_THRESHOLD_KEY).getAsFloat());
        ((Thermostat) devices.get(id)).setMode(recv.get(Const.STATUS_KEY).getAsInt());
        devices.get(id).setOnline();

    }

    private void updateRemoteSwitch(int id, @NonNull JsonObject recv){
        ((RemoteSwitch) devices.get(id)).setMode(recv.get(Const.STATUS_KEY).getAsInt());
        devices.get(id).setOnline();
    }

    private void updateEnergyMonitor(int id, @NonNull JsonObject recv){
        ((EnergyMonitor)devices.get(id)).setDataUrl(recv.get(Const.EMON_DATA_PATH_KEY).getAsString());
        ((EnergyMonitor)devices.get(id)).setPower(recv.get(Const.EMON_POWER).getAsFloat());
        devices.get(id).setOnline();
    }

    public String getHouseTopic() {
        return houseTopic;
    }

    public void setHouseTopic(String houseTopic) {
        this.house_id = houseTopic;
        this.houseTopic = "dome/" + house_id;
    }
}
