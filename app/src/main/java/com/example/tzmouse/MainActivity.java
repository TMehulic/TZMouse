package com.example.tzmouse;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private ViewHolder holder=new ViewHolder();

    MqttAndroidClient mqttAndroidClient;

    final String serverUri = "tcp://broker.hivemq.com:1883";
    String clientId = "ExampleAndroidClient";
    final String subscriptionTopic = "exampleAndroidTopic";
    final String publishTopic = "tzmouse/test";
    String publishMessage = "Molim te da radi";


    static class ViewHolder{
        TextView x;
        TextView y;
        Button btn;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        holder.x=findViewById(R.id.x);
        holder.y=findViewById(R.id.y);
        holder.btn=findViewById(R.id.button);


        holder.btn.setOnClickListener(listener);




        clientId = clientId + System.currentTimeMillis();

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
//                    addToHistory("Reconnected to : " + serverURI);
//                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();

                } else {
//                    addToHistory("Connected to: " + serverURI);
                    System.out.println("Connected to: " + serverURI);;
                }

                mSensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                mSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

            }



            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("CONN LOST");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                addToHistory("Incoming message: " + new String(message.getPayload()));
                System.out.println("STIGLA PORUKA: "+new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    System.out.println("Failed to connect to: " + serverUri);
                }
            });

        } catch (MqttException ex){
            ex.printStackTrace();
        }


    }

    private  View.OnClickListener listener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            publishMessage();
        }
    };

    protected void onResume(){
        super.onResume();
//        mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL);
//        mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause(){
        super.onPause();
//        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x1=event.values[0];
        float y1=event.values[1];
        String x=null;
        String y=null;
        if(Math.abs(x1)>0.02){
            x=String.valueOf(x1);
        }else{
            x=String.valueOf(0);
        }
        if(Math.abs(y1)>0.02){
            y=String.valueOf(y1);
        }else{
            y=String.valueOf(0);
        }
        publishMessage=x+","+y;
        publishMessage();

        //TODO: OVDJE IDE PUBLISH MESSAGE DA SE PROSLIJEDE VRIJEDNOSTI OVE
//        System.out.println("X--->"+x);
//        System.out.println("Y--->"+y);
//        if(Math.abs(x)>0.2){
//            holder.x.setText(String.valueOf(x));
//        }
//        if(Math.abs(y)>0.2){
//            holder.y.setText(String.valueOf(y));
//        }
    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
//                    addToHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    addToHistory("Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage(){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            System.out.println("Message published");
            if(!mqttAndroidClient.isConnected()){
                System.out.println(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
