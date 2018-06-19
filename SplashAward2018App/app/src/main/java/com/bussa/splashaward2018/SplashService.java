package com.bussa.splashaward2018;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SplashService extends Service {

    public MqttAndroidClient mqttAndroidClient;
    final String serverUri = "tcp://172.104.58.41:21";

    final String clientId = "ExampleAndroidClient";
    final String subscriptionTopic = "hello";

    String api_key = "data mall key here";


    private Messenger parentMessenger;

    private MqttCallback mqttCallback = new MqttCallback();

    boolean pollAgain = false;

    Timer timer = new Timer();
    private boolean firstTimePool = true;

    public void getDirectionTo(String direction) {

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=1.373922,103.752233&destination={0}&key=_KEY_HERE&mode=transit&alternatives=true";

        String finalUrl = MessageFormat.format(url, direction);

        final OkHttpClient client = new OkHttpClient();

        Request.Builder builderRequest = new Request.Builder().url(finalUrl);
        final Request request = builderRequest.build();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                Response response = null;
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String bodyMessage = "";
                try {
                    bodyMessage =response.body().string();
                } catch (IOException e) {
                    Log.i("splash", "Error while fecthing venue");
                    e.printStackTrace();
                }

                if (bodyMessage != "") {
                    try {

                        Message message = new Message();
                        message.arg1 = MainActivity.FROM_DIRECTION;
                        Bundle data = new Bundle();
                        data.putString("route", bodyMessage);
                        message.setData(data);
                        parentMessenger.send(message);
                        
                    } catch (RemoteException e) {
                        Log.i("splash", "Error while SEND MESSAGE");
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();


    }


    private void pollForBus() {
        String url = "http://datamall2.mytransport.sg/ltaodataservice/BusArrivalv2?BusStopCode=44151";

        OkHttpClient client = new OkHttpClient();
        Request.Builder builderRequest = new Request.Builder().url(url);
        builderRequest.header("AccountKey", api_key);
        builderRequest.header("accept", "application/json");
        Request request = builderRequest.build();

        try {
            Response response = client.newCall(request).execute();
            String bodyMessage =response.body().string();

            Message messageFromBusPolling = new Message();
            Bundle data = new Bundle();
            data.putString("busInfo", bodyMessage );
            messageFromBusPolling.arg1 = MainActivity.FROM_BUS_POLLING;
            messageFromBusPolling.setData(data);
            parentMessenger.send(messageFromBusPolling);

        } catch (Exception ex) {
            Log.i("splash", "error");
            ex.printStackTrace();
        }
    }

    private void loopApiPolling() {
        Log.i("splash","start api polling");

        TimerTask delayedTimer = new TimerTask() {
            @Override
            public void run() {
                pollForBus();
                if (pollAgain) {
                    loopApiPolling();
                }
            }
        };

        if (firstTimePool) {
            timer.schedule(delayedTimer, 1 * 1000);
            firstTimePool = false;
        } else {
            timer.schedule(delayedTimer, 15 * 1000);
        }

    }





    public SplashService() {
        Log.i("splash", "service splash init");

    }


    @Override
    public void onCreate() {
        Log.i("splash", "oncreate service");

        mqttAndroidClient = new MqttAndroidClient(this, serverUri, clientId);
        mqttAndroidClient.setCallback(mqttCallback);
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName("splash");
        mqttConnectOptions.setPassword("splash".toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("mqtt", "onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                }
            });
        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    public void sendMessageToClient(String message) throws MqttException {
        MqttMessage m = new MqttMessage(message.getBytes());
        mqttAndroidClient.publish("display", m);
    }


    class MqttCallback implements MqttCallbackExtended {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
            disconnectedBufferOptions.setBufferEnabled(true);
            disconnectedBufferOptions.setBufferSize(100);
            disconnectedBufferOptions.setPersistBuffer(false);
            disconnectedBufferOptions.setDeleteOldestMessages(false);
            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
            MqttMessage m = new MqttMessage("hello".getBytes());

            try {
                mqttAndroidClient.publish("splash", m);
            } catch (MqttException e) {
                e.printStackTrace();
            }

            Message msg = new Message();
            msg.arg1 = MainActivity.MQTT_CONNECTED;
            try {
                parentMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            Log.w("Mqtt", mqttMessage.toString());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    }

    public class SplashServiceBinder extends Binder {
        SplashService getSplashService() {
            return SplashService.this;
        }
    }

    private final IBinder binder = new SplashServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        parentMessenger = (Messenger)intent.getExtras().get("MESSENGER");
        Log.i("splash", "i got the messenger");
        pollAgain = true;
        loopApiPolling(); // kick start api polling after it's been started


        // TODO: Return the communication channel to the service.
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        pollAgain = false;
        return true;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        pollAgain = false;
        mqttAndroidClient.unregisterResources();
        mqttAndroidClient.close();

        Log.i("splash", "Destroy service splash");

    }

}


