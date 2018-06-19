package com.bussa.splashaward2018;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public final static int FROM_BUS_POLLING = 1;
    public final static int FROM_DIRECTION = 2;

    private static final int STATUS_CHOICE = 111;
    private static final int STATUS_INIT = 1112;
    private static final int STATUS_REMINDER =2224 ;
    private static final int STATUS_ADDED_REMINDER = 3234;
    private static final int STATUS_TIMING =1024 ;
    private static final int STATUS_DESTINATION_CONFIRM = 123434;
    private static final int STATUS_DESTINATION = 883;
    private static final int STATUS_DISPLAY_ROUTE = 12341;

    int STATUS_SAY_HI = 10;

    private static final int SENSOR_SENSITIVITY = 4;
    public static final int MQTT_CONNECTED =3 ;

    private SplashService splashService;

    private boolean bound = false;
    private MessageHandler messageHandler = new MessageHandler();
    private SpeechRecognizer speechRecognizer;

    BusAssistance bussa = new BusAssistance(103.75223395114867, 1.37392273953012);
    private int currentStatus;
    private String destinationPlace;


    private List<String> getBestRoute(String routeInfo) throws JSONException {

        JSONObject jsonObject= new JSONObject(routeInfo);

        if (jsonObject.getString("status").equals("OK")) {

            ArrayList<String> routeToTake = new ArrayList<String>();

            JSONArray routes = jsonObject.getJSONArray("routes");

            // get first route first
            JSONObject bestRoute = routes.getJSONObject(0);

            JSONArray steps = bestRoute.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");

            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.getJSONObject(i);
                routeToTake.add(step.getString("html_instructions"));
            }
            return routeToTake;
        }


        return null;
    }
    NumberFormat formatter = new DecimalFormat("#0.00");
    boolean busPoolDoneAlready = false;

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            Log.i("splash", "i got the message");

            if (message.arg1 == MQTT_CONNECTED) {

            }
            if (message.arg1 == FROM_BUS_POLLING) {


                Bundle data = message.getData();
                List<BusAssistance.BusInfo>  busList = bussa.getAndStoreBusListUsingJSON(data.getString("busInfo"));

                StringBuilder sb = new StringBuilder();
                for (BusAssistance.BusInfo bi : busList) {
                    sb.append((bi.busNo + " " + formatter.format(bi.getMinutes()) + "mins - " + formatter.format(bi.getDistance()) + " metres,   "));
                }

                TextView busInfo = findViewById(R.id.busInfo);
                busInfo.setText(sb.toString());

                if (currentStatus == STATUS_TIMING) {
                    try {
                        splashService.sendMessageToClient("timing*" + bussa.giveMeBusList());
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                if (!busPoolDoneAlready) {
                    setCurrentStatus(STATUS_INIT);
                    busPoolDoneAlready = true;
                }
            } else if (message.arg1 == FROM_DIRECTION) {
                Bundle data = message.getData();

                String routeInfo = data.getString("route");

                List<String> bestRoute = null;
                try {
                    bestRoute = getBestRoute(routeInfo);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                StringBuilder sb = new StringBuilder();
                for (String route : bestRoute ) {
                    sb.append((route  + ", "));

                }

                TextView routetextview = findViewById(R.id.routeInfo);

                routetextview.setText(sb.toString());
                if (currentStatus == STATUS_DISPLAY_ROUTE) {
                    try {
                        splashService.sendMessageToClient("displayroute*" + sb.toString());
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            SplashService.SplashServiceBinder splashBinder = (SplashService.SplashServiceBinder)binder;
            splashService = splashBinder.getSplashService();

            bound = true;
            Log.i("splash", "bound");
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //Code that runs when the service is disconnected
            splashService = null;
            bound = false;
            Log.i("splash", "unbound");

        }
    };

    public float sensorValue = 0;
    class SplashSensorAlert implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                Log.i("sensor proximity", String.valueOf(event.values[0]));
                // 8 means far
                // 0 means near

            } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {

                TextView sensorValue = findViewById(R.id.sensorValue);

                MainActivity.this.checkSensorValue(event.values[0] );
                if (MainActivity.this.sensorValue == 0) {
                    MainActivity.this.sensorValue = event.values[0];
                } else {
                    MainActivity.this.sensorValue = (float) (MainActivity.this.sensorValue * 0.7 + event.values[0] * 0.3);
                }
                sensorValue.setText("Sensor Value : " + String.valueOf(MainActivity.this.sensorValue));

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    boolean checkForSensor = false;
    private void checkSensorValue(float value) {

        if (!checkForSensor) {
            return ;
        }
        if (sensorValue== 0) {
            return;
        } else if (value < sensorValue/2) {
            try {
                setCurrentStatus(STATUS_SAY_HI);
                splashService.sendMessageToClient("hi");
                saySomething("Hi. Welcome.");
                checkForSensor = false;
            } catch (MqttException e) {
                Log.i("splash", "cannot send mq message");
                e.printStackTrace();
            }

        }
    }

    private void setCurrentStatus(int status) {
        currentStatus = status;

        TextView textViewStatus = findViewById(R.id.currentStatus);
        if (status == STATUS_SAY_HI) {
            textViewStatus.setText("Say Hi");
        } else if (status == STATUS_CHOICE) {
            textViewStatus.setText("Making a choice");
        } else if (status == STATUS_INIT) {
            checkForSensor = true;
            textViewStatus.setText("System Ready");
            try {
                splashService.sendMessageToClient("init");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else if(status == STATUS_TIMING) {
            textViewStatus.setText("Timing display");
            checkForSensor = true;

        } else if(status == STATUS_DESTINATION) {
            textViewStatus.setText("Waiting for destination");
        } else if(status == STATUS_DESTINATION_CONFIRM) {
            textViewStatus.setText("Waiting for destination - confirmation");
        } else if(status == STATUS_DISPLAY_ROUTE) {
            textViewStatus.setText("Displaying route");
        }

    }
    public void setStatuMainThread(final int status) {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                setCurrentStatus(status);
            }
        });

    }
    public void listenAgain() {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable() {


            @Override
            public void run() {
                startListeningToUser();
            }
        });

    }
    public void activateHiAlready() {

        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable() {


            @Override
            public void run() {
                try {
                    setCurrentStatus(STATUS_CHOICE);
                    splashService.sendMessageToClient("screen");
                    startListeningToUser();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    void checkSpeech(String speech) {
        Log.i("splash", "check speech");
        if (currentStatus == STATUS_CHOICE) {
            if (speech.equalsIgnoreCase("reminder")) {
                try {
                    splashService.sendMessageToClient("reminder*" + bussa.giveMeBusList());
                    setCurrentStatus(STATUS_REMINDER);
                    saySomething("Service Number Please");

                } catch (MqttException e) {
                    e.printStackTrace();
                }

            } else if (speech.equalsIgnoreCase("route") || speech.equalsIgnoreCase("route planning")) {

                setCurrentStatus(STATUS_DESTINATION);
                try {
                    splashService.sendMessageToClient("destination*");
                    saySomething("Your destination please");

                } catch (MqttException e) {
                    e.printStackTrace();
                }

            } else if (speech.equalsIgnoreCase("timing") || speech.equalsIgnoreCase("bus timing")){
                try {
                    splashService.sendMessageToClient("timing*" + bussa.giveMeBusList());
                    setCurrentStatus(STATUS_TIMING);
                } catch (MqttException e) {
                    e.printStackTrace();
                }

            }


        } else if (currentStatus == STATUS_REMINDER) {
            if (bussa.insideBusList(speech)) {
                bussa.addBusToReminder(speech);
                try {
                    splashService.sendMessageToClient("add*" + bussa.giveMeBusList());
                    saySomething(speech + " " + "has been added. Thank you.");
                    setCurrentStatus(STATUS_ADDED_REMINDER);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            } else {
                saySomething("Please say again.");
            }
        } else if (currentStatus == STATUS_DESTINATION) {
            try {
                setCurrentStatus(STATUS_DESTINATION_CONFIRM);
                splashService.sendMessageToClient("destination*" + speech);
                TextView destinationEditText = findViewById(R.id.routeEditText);
                destinationPlace = speech;
                destinationEditText.setText(speech);
                saySomething("Please say ok to confirm");
            } catch (MqttException e) {
                e.printStackTrace();
            }

        } else if (currentStatus == STATUS_DESTINATION_CONFIRM) {
            if (!(speech.equalsIgnoreCase("okay") || speech.equalsIgnoreCase("ok"))) {
                try {

                    setCurrentStatus(STATUS_DESTINATION);
                    saySomething("Your destination please");
                    splashService.sendMessageToClient("destination*");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            } else {

                try {
                    splashService.sendMessageToClient("destination*" + destinationPlace + "*wait");
                    saySomething("Your destination is being calculated now");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                setCurrentStatus(STATUS_DISPLAY_ROUTE);
                splashService.getDirectionTo(destinationPlace);
            }
        }

    }

    private SensorManager mSensorManager;
    private Sensor mProximity;

    private SplashSensorAlert  mSplashSensorAlert = new SplashSensorAlert();

    private TextToSpeech tts;

    void saySomething(String text) {
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");

        tts.speak(text, TextToSpeech.QUEUE_ADD, params,"uniqueID");
    }

    void setTextToSpeechEngine() {
        // setup text to speech engine
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.i("splash", "Speech init");
                Log.i("splash", String.valueOf(status));
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {

            }

            @Override
            public void onDone(String s) {
                Log.i("splash", "done speaking");
                Log.i("splash", s);
                if (currentStatus == STATUS_SAY_HI) {
                    activateHiAlready();
                } else if (currentStatus == STATUS_CHOICE) { // come here means it didn't listen correctly
                    listenAgain();
                } else if (currentStatus == STATUS_REMINDER) {
                    listenAgain();
                } else if (currentStatus == STATUS_ADDED_REMINDER) {

                    setStatuMainThread(STATUS_INIT);
                } else if (currentStatus == STATUS_DESTINATION) {
                    listenAgain();
                } else if (currentStatus == STATUS_DESTINATION_CONFIRM){
                    listenAgain();
                }
            }

            @Override
            public void onError(String s) {

            }
        });
        tts.setLanguage(Locale.US);

    }

    void setupSlashService() {
        Intent intent = new Intent(this, SplashService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        setTextToSpeechEngine();

        Button btnspeakout = findViewById(R.id.button);
        btnspeakout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saySomething("Yahoo");
            }
        });


        Button saysomething = findViewById(R.id.button2);

        saysomething.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               startListeningToUser();
            }
        });

        Button getoroute = findViewById(R.id.getroute);

        getoroute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText routeEditText = findViewById(R.id.routeEditText);
                Log.i("splash","Initiate direction api");
                splashService.getDirectionTo(routeEditText.getText().toString());

            }
        });

        Button resetbutton = findViewById(R.id.resetbutton);

        resetbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentStatus(STATUS_INIT);
            }
        });

        setupSlashService();
        setupSpeechRecognition();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
    }

    private void startListeningToUser() {
        final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        // TODO: Start ASR
        speechRecognizer.startListening(recognizerIntent);
    }


    void setupSpeechRecognition() {
        speechRecognizer  = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.i("splash", "ready speech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.i("splash", "beginnnig speech");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                Log.i("splash", "end of speech");
            }

            @Override
            public void onError(int i) {
                Log.i("splash", "error starting");
                Log.i("splash", String.valueOf(i));
                saySomething("Please say again.");
            }

            @Override
            public void onResults(Bundle bundle) {
                List<String> texts = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                TextView textView = findViewById(R.id.resultView);
                if (texts == null || texts.isEmpty()) {
                    textView.setText("No text");
                    checkSpeech("");
                } else {

                    textView.setText(texts.get(0));
                    checkSpeech(texts.get(0));
                }


                Log.i("splash", texts.toString());


            }

            @Override
            public void onPartialResults(Bundle bundle) {
                Log.i("splash", "artial result");
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSplashSensorAlert, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSplashSensorAlert);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}
