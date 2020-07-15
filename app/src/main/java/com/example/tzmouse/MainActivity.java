package com.example.tzmouse;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

    private static class ViewHolder{
        Button leftClick;
        Button rightClick;
        Button confirmBtn;
        Button scrollUp;
        Button scrollDown;
        Button block;
        EditText topicText;
        TextView logotext;
        ImageView info;
    }

    private boolean blocked=false;


    //SPEED INFO
    private long previousTimeStamp=System.currentTimeMillis();

    int brojac=0;

//    boolean prviPut=true;
//    double prosla;
//    private double velocityX=0;
//    private double velocityY=0;





    private ViewHolder holder=new ViewHolder();

    MqttAndroidClient mqttAndroidClient;

    final String serverUri = "tcp://broker.hivemq.com:1883";
    String clientId = "ExampleAndroidClient";
    final String subscriptionTopic = "exampleAndroidTopic";
    String publishTopic = "tzmouse/PCName/Move"; //MOVEMENT
    String publishTopicLMBPress="tzmouse/PCName/LMBPress";//Lijevi click press   šalje 1 dok je pritisnuto, 0 kad je gg
    String publishTopicRMBPress="tzmouse/PCName/RMBPress";//Desni click press    šalje 1 dok je pritisnuto, 0 kad je gg
    String publishTopicLMBClick="tzmouse/PCName/LMBClick";//Lijevi click         šalje 1 po kliku samo
    String publishTopicRMBClick="tzmouse/PCName/RMBClick";//Desni click          šalje 1 po kliku samo
    String publishTopicScrollUp="tzmouse/PCName/ScrollUp";//Scroll up            šalje 1 dok je pritisnuto, 0 kad je gg
    String publishTopicScrollDown="tzmouse/PCName/ScrollDown";//Scroll down      šalje 1 dok je pritisnuto, 0 kad je gg
    String publishMessage = "Molim te da radi";
    String publishLMBClickMessage="1";
    String publishRMBClickMessage="1";
    String publishLMBPressMessage="";
    String publishRMBPressMessage="";
    String publishScrollUpMessage="";
    String publishScrollDownMessage="";

    AlertDialog.Builder dialogBuilder;




    @SuppressLint({"ClickableViewAccessibility", "SourceLockedOrientationActivity"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        dialogBuilder=new AlertDialog.Builder(this);

        holder.leftClick=findViewById(R.id.leftClick);
        holder.rightClick=findViewById(R.id.rightClick);
        holder.confirmBtn=findViewById(R.id.confirmBtn);
        holder.topicText=findViewById(R.id.topicText);
        holder.scrollUp=findViewById(R.id.scrollUp);
        holder.scrollDown=findViewById(R.id.scrollDown);
        holder.logotext=findViewById(R.id.logotext);
        holder.block=findViewById(R.id.block);
        holder.info=findViewById(R.id.imageView);

        holder.info.setColorFilter(Color.CYAN);

        holder.info.setOnClickListener(infoListener);


        setAnimations();

//        Matrix matrix = new Matrix();
//        images.img1.setScaleType(ImageView.ScaleType.MATRIX); //required
//        matrix.postRotate((float) 20, 5,    5);
//        images.img1.setImageMatrix(matrix);


        clientId = clientId + System.currentTimeMillis();

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    subscribeToTopic();
                } else {
                    System.out.println("Connected to: " + serverURI);;
                }
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
                    sucess();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    System.out.println("Failed to connect to: " + serverUri);
                }
            });

        } catch (MqttException ex){
            ex.printStackTrace();
        }



        holder.confirmBtn.setOnClickListener(confirmClickListener);
        holder.leftClick.setOnClickListener(lmbClickListener);
        holder.rightClick.setOnClickListener(rmbClickListener);
        holder.leftClick.setOnTouchListener(lmbPressListener);
        holder.rightClick.setOnTouchListener(rmbPressListener);
        holder.scrollUp.setOnTouchListener(scrollUpListener);
        holder.scrollDown.setOnTouchListener(scrollDownListener);
        holder.block.setOnTouchListener(blockListener);


    }

    private void setAnimations() {
        final ValueAnimator animator=ValueAnimator.ofFloat(0f,1f);
        final int cyan=Color.CYAN;
        animator.setDuration(3000);



        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float mul = (Float) animation.getAnimatedValue();
                int alpha = adjustAlpha(cyan, mul);
                holder.logotext.setTextColor(alpha);
            }
        });

        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }

    public int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private View.OnClickListener infoListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dialogBuilder.setMessage("'BLOCK' dugme služi za blokiranje slanja informacija o kretanju.\n" +
                    "Korisno pri pomjeranju uređaja s jednog mjesta na drugo, a pri tome ne želimo da se kursor pomjeri.");
            dialogBuilder.setCancelable(false);
            dialogBuilder.setPositiveButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            Dialog dialog = dialogBuilder.create();
            dialog.show();
        }
    };

    //Postavlja topic
    private View.OnClickListener confirmClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            publishTopic="tzmouse/"+holder.topicText.getText().toString()+"/Move";
            publishTopicLMBClick="tzmouse/"+holder.topicText.getText().toString()+"/LMBClick";
            publishTopicRMBClick="tzmouse/"+holder.topicText.getText().toString()+"/RMBClick";
            publishTopicLMBPress="tzmouse/"+holder.topicText.getText().toString()+"/LMBPress";
            publishTopicRMBPress="tzmouse/"+holder.topicText.getText().toString()+"/RMBPress";
            publishTopicScrollUp="tzmouse/"+holder.topicText.getText().toString()+"/ScrollUp";
            publishTopicScrollDown="tzmouse/"+holder.topicText.getText().toString()+"/ScrollDown";
            holder.topicText.setEnabled(false);
            holder.topicText.setEnabled(true);
        }
    };

    private View.OnClickListener lmbClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            publishLMBClick();
        }
    };

    private View.OnClickListener rmbClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            publishRMBClick();
        }
    };

    private View.OnTouchListener blockListener=new View.OnTouchListener(){

        private Handler handler;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                if(handler!=null) return true;
                blocked=true;
//                velocityX=0;
//                velocityY=0;
                v.setPressed(true);
                holder.block.setTextColor(Color.parseColor("#cc0000"));
                handler=new Handler();
                handler.postDelayed(mAction,1000);
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                if (handler == null) return true;
                v.setPressed(false);
                blocked=false;
                handler.removeCallbacks(mAction);
                blocked=false;
                holder.block.setTextColor(Color.parseColor("#ff103a"));
                handler = null;
            }
            return false;
        }

        Runnable mAction= new Runnable() {
            @Override
            public void run() {
                blocked=true;
                handler.postDelayed(this,1000);
            }
        };
    };

    private View.OnTouchListener lmbPressListener=new View.OnTouchListener() {

        private Handler handler;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if(event.getAction()==MotionEvent.ACTION_DOWN){
                if(handler!=null) return true;
                publishLMBPressMessage="1";
                v.setPressed(true);
                handler=new Handler();
                handler.postDelayed(mAction,1000);
//                publishLMBClick();
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                if (handler == null) return true;
                publishLMBPressMessage="0";
                v.setPressed(false);
                handler.removeCallbacks(mAction);
                handler = null;
                publishLMBPress();
            }
            return false;
        }

        Runnable mAction= new Runnable() {
            @Override
            public void run() {
                publishLMBPress();
                handler.postDelayed(this,1000);
            }
        };
    };

    private View.OnTouchListener rmbPressListener=new View.OnTouchListener() {
        private Handler handler;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if(event.getAction()==MotionEvent.ACTION_DOWN){
                if(handler!=null) return true;
                publishRMBPressMessage="1";
                v.setPressed(true);
                handler=new Handler();
                handler.postDelayed(mAction,1000);
//                publishRMBClick();
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                if (handler == null) return true;
                publishRMBPressMessage="0";
                v.setPressed(false);
                handler.removeCallbacks(mAction);
                handler = null;
                publishRMBPress();
            }
            return false;
        }

        Runnable mAction= new Runnable() {
            @Override
            public void run() {
                publishRMBPress();
                handler.postDelayed(this,1000);
            }
        };
    };

    private View.OnTouchListener scrollUpListener=new View.OnTouchListener() {

        private Handler handler;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if(event.getAction()==MotionEvent.ACTION_DOWN){
                if(handler!=null) return true;
                publishScrollUpMessage="1";
                v.setPressed(true);
                handler=new Handler();
                handler.postDelayed(mAction,1000);
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                if (handler == null) return true;
                publishScrollUpMessage="0";
                v.setPressed(false);
                handler.removeCallbacks(mAction);
                handler = null;
                publishScrollUp();
            }
            return false;
        }

        Runnable mAction= new Runnable() {
            @Override
            public void run() {
                publishScrollUp();
                handler.postDelayed(this,1000);
            }
        };
    };

    private View.OnTouchListener scrollDownListener=new View.OnTouchListener() {

        private Handler handler;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if(event.getAction()==MotionEvent.ACTION_DOWN){
                if(handler!=null) return true;
                publishScrollDownMessage="1";
                v.setPressed(true);
                handler=new Handler();
                handler.postDelayed(mAction,1000);
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                if (handler == null) return true;
                publishScrollDownMessage="0";
                v.setPressed(false);
                handler.removeCallbacks(mAction);
                handler = null;
                publishScrollDown();
            }
            return false;
        }

        Runnable mAction= new Runnable() {
            @Override
            public void run() {
                publishScrollDown();
                handler.postDelayed(this,1000);
            }
        };
    };



    private void sucess() {
        mSensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }



    protected void onResume(){
        super.onResume();
    }

    protected void onPause(){
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //OVDJE NE MOŽEMO SKONTATI MOVEMENTE...

        float x1=event.values[0];
        float y1=event.values[1];

        long timestamp=System.currentTimeMillis();
        double time = (timestamp-previousTimeStamp)/1000f;

        if((Math.abs(x1)>2 || Math.abs(y1)>2) && !blocked) {
            if(Math.abs(x1)<2){
                x1=0;
            }
            if(Math.abs(y1)<2){
                y1=0;
            }
            if(brojac==0){
                publishMessage=x1/2+","+y1/2;
                System.out.println(publishMessage);
                publishMessage();
                brojac++;
            }else if(brojac==3){
                brojac=0;
            }else{
                brojac++;
            }
        }

//            if(prviPut){
//                prosla=x1;
//                prviPut=false;
//            }else{
//                if(prosla<0 && x1>0){
//                    brojac++;
//                }else if(prosla<0 && x1<0){
//                    prosla=x1;
//                    brojac=0;
//                }else if(prosla>0 && x1<0){
//                    brojac++;
//                }else if(prosla>0 && x1>0){
//                    brojac=0;
//                    prosla=x1;
//                }
//            }
//
//            if(brojac==3){
//                System.out.println("SLEEP");
//                if(prosla<0){
//                    velocityX-=0.001;
//                }else{
//                    velocityX+=0.001;
//                }
////                velocityX=0;
//                mSensorManager.unregisterListener(this);
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                brojac=0;
//                prosla*=(-1);
//                previousTimeStamp=System.currentTimeMillis();
//                mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL);
//            }else{
//                previousTimeStamp=timestamp;
//
//                double distancex = (velocityX * time) + (0.5 * (x1 * (time * time)));
//                double distancey = (velocityY * time) + (0.5 * (y1 * (time * time)));
//
//                velocityX += x1 * time;
//                velocityY += y1 * time;
////            velocityX+=Math.pow(Math.E,(-1)*Math.abs(velocityX))*x1*20;
//
//                publishMessage = distancex + "," + distancey;
//                System.out.println("VELOCITY: "+velocityX);
//                System.out.println(publishMessage);
//
//                publishMessage();
//            }
//        }else{
//            velocityX=0;
//            brojac=0;
//            previousTimeStamp=System.currentTimeMillis();
//        }
    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                }
            });

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

    public void publishLMBPress(){
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishLMBPressMessage.getBytes());
            mqttAndroidClient.publish(publishTopicLMBPress, message);
            System.out.println("Message published");
            if(!mqttAndroidClient.isConnected()){
                System.out.println(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publishRMBPress(){
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishRMBPressMessage.getBytes());
            mqttAndroidClient.publish(publishTopicRMBPress, message);
            System.out.println("Message published");
            if(!mqttAndroidClient.isConnected()){
                System.out.println(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publishLMBClick(){
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishLMBClickMessage.getBytes());
            mqttAndroidClient.publish(publishTopicLMBClick, message);
            System.out.println("Message published");
            if(!mqttAndroidClient.isConnected()){
                System.out.println(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publishRMBClick(){
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishRMBClickMessage.getBytes());
            mqttAndroidClient.publish(publishTopicRMBClick, message);
            System.out.println("Message published");
            if(!mqttAndroidClient.isConnected()){
                System.out.println(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publishScrollUp(){
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishScrollUpMessage.getBytes());
            mqttAndroidClient.publish(publishTopicScrollUp, message);
            System.out.println("Message published");
            if(!mqttAndroidClient.isConnected()){
                System.out.println(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publishScrollDown(){
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishScrollDownMessage.getBytes());
            mqttAndroidClient.publish(publishTopicScrollDown, message);
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