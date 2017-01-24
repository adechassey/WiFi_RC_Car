package com.adc.pirobot;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;


/**
 * Created by Antoine on 16/01/2017.
 */

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";

    // Sensors
    private TextView xA, yA, zA;
    private TextView actionY, actionZ;
    private float thrust_Z = 0, thrust_Y = 0;
    private float thrustInMemory_Z = 1000, thrustInMemory_Y = 1000;
    private final float acquisitionDelta_Z = 100, acquisitionDelta_Y = 100;

    // MQTT
    private TextView mqtt_status;
    private MqttAndroidClient mqtt_client;
    private MqttConnectOptions options;
    private final String mqtt_broker = "tcp://<IP>:1883";
    private final String mqtt_topic = "home/car";
    private final String mqtt_topic_log = "home/log";

    // Déclaration de l'attribut en tant qu'attribut de l'activité
    // Le sensor manager (gestionnaire de capteurs)
    SensorManager sensorManager;

    // L'accéléromètre
    Sensor accelerometer;

    // Appelé à la création de l'activité. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        // Instancier le gestionnaire des capteurs, le SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Instancier l'accéléromètre
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // MQTT
        mqtt_status = (TextView) findViewById(R.id.mqtt_status);
        connect();

        // Init textviews
        xA = (TextView) findViewById(R.id.xAccelerometer);
        yA = (TextView) findViewById(R.id.yAccelerometer);
        zA = (TextView) findViewById(R.id.zAccelerometer);
        actionY = (TextView) findViewById(R.id.actionY);
        actionZ = (TextView) findViewById(R.id.actionZ);

    }

    /********************************************************************/
    /****************************** MQTT ********************************/
    /********************************************************************/

    private void connect() {
        options = new MqttConnectOptions();
        options.setWill(mqtt_topic_log, "{\"action\": \"forward\", \"thrust\": 0}".getBytes(), 1, false);

        String clientId = MqttClient.generateClientId();
        mqtt_client =
                new MqttAndroidClient(this.getApplicationContext(), mqtt_broker,
                        clientId);
        try {
            IMqttToken token = mqtt_client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    String payload = "{\"name\": \"robot\", \"status\": \"connected\"}";
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        mqtt_client.publish(mqtt_topic_log, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                    mqtt_status.setText("Connected successfully");
                    Log.d(TAG, "onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    mqtt_status.setText("Failed to connect");
                    Log.d(TAG, String.valueOf(exception));
                    Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publish(String payload) {
        if (mqtt_client.isConnected()) {
            byte[] encodedPayload = new byte[0];
            try {
                encodedPayload = payload.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                mqtt_client.publish(mqtt_topic, message);
            } catch (UnsupportedEncodingException | MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void disconnect() {
        if (mqtt_client.isConnected()) {
            try {
                IMqttToken disconToken = mqtt_client.disconnect();
                disconToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // we are now successfully disconnected
                        mqtt_status.setText("Disconnected successfully");
                        Log.d(TAG, "Disconnected successfully");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
                                          Throwable exception) {
                        // something went wrong, but probably we are disconnected anyway
                        mqtt_status.setText("Something went wrong");
                        Log.d(TAG, "Something went wrong");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /********************************************************************/
    /********************** SensorEventListener *************************/
    /********************************************************************/

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Rien à faire la plupart du temps
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Récupérer les valeurs du capteur
        float xAccelerometer, yAccelerometer, zAccelerometer;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            xAccelerometer = event.values[0];
            yAccelerometer = event.values[1];
            zAccelerometer = event.values[2];
            xA.setText(String.valueOf(xAccelerometer));
            yA.setText(String.valueOf(yAccelerometer));
            zA.setText(String.valueOf(zAccelerometer));

            if (zAccelerometer >= 6 & zAccelerometer <= 10) {
                thrust_Z = ((zAccelerometer - 6) / 4) * 1000;
                if (Math.abs(thrust_Z - thrustInMemory_Z) >= acquisitionDelta_Z) {
                    actionZ.setText("Going forward, thrust: " + thrust_Z + "%");
                    publish("{\"action\": \"forward\", \"thrust\": " + String.valueOf(thrust_Z) + "}\"");
                    thrustInMemory_Z = thrust_Z;
                }
            } else if (zAccelerometer <= 4 && zAccelerometer >= 0) {
                thrust_Z = Math.abs(((zAccelerometer - 4) / 4) * 1000);
                if (Math.abs(thrust_Z - thrustInMemory_Z) >= acquisitionDelta_Z) {
                    actionZ.setText("Going backwards, thrust: " + thrust_Z + "%");
                    publish("{\"action\": \"backward\", \"thrust\": " + String.valueOf(thrust_Z) + "}\"");
                    thrustInMemory_Z = thrust_Z;
                }
            } else {
                actionZ.setText("Not moving on Z");
                thrustInMemory_Z = thrust_Y;
            }

            if (yAccelerometer >= -4 && yAccelerometer < -1) {
                thrust_Y = Math.abs((yAccelerometer / (float) 4) * 1000);
                if (Math.abs(thrust_Y - thrustInMemory_Y) >= acquisitionDelta_Y) {
                    actionY.setText("Going left, thrust: " + thrust_Y + "%");
                    publish("{\"action\": \"left\", \"thrust\": " + String.valueOf(thrust_Y) + "}\"");
                    thrustInMemory_Y = thrust_Y;
                }
            } else if (yAccelerometer >= 1 && yAccelerometer <= 4) {
                thrust_Y = Math.abs((yAccelerometer / (float) 4) * 1000);
                if (Math.abs(thrust_Y - thrustInMemory_Y) >= acquisitionDelta_Y) {
                    actionY.setText("Going right, thrust: " + thrust_Y + "%");
                    publish("{\"action\": \"right\", \"thrust\": " + String.valueOf(thrust_Y) + "}\"");
                    thrustInMemory_Y = thrust_Y;
                }
            } else {
                actionY.setText("Not moving on Y");
                thrustInMemory_Y = thrust_Y;
            }
        }
    }

    /**
     * Called when the activity is about to become visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "The onStart() event");
    }

    /**
     * Called when the activity has become visible.
     */
    @Override
    protected void onResume() {
        /* Ce qu'en dit Google&#160;dans le cas de l'accéléromètre :
         * «&#160; Ce n'est pas nécessaire d'avoir les évènements des capteurs à un rythme trop rapide.
         * En utilisant un rythme moins rapide (SENSOR_DELAY_UI), nous obtenons un filtre
         * automatique de bas-niveau qui "extrait" la gravité  de l'accélération.
         * Un autre bénéfice étant que l'on utilise moins d'énergie et de CPU.&#160;»
         */
        sensorManager.registerListener((SensorEventListener) this, accelerometer, SensorManager.SENSOR_DELAY_UI);

        super.onResume();
        Log.d(TAG, "The onResume() event");
    }

    /**
     * Called when another activity is taking focus.
     */
    @Override
    protected void onPause() {
        // Unregister the sensor (désenregistrer le capteur)
        sensorManager.unregisterListener((SensorEventListener) this, accelerometer);

        super.onPause();
        Log.d(TAG, "The onPause() event");
    }

    /**
     * Called when the activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "The onStop() event");
    }

    /**
     * Called just before the activity is destroyed.
     */
    @Override
    public void onDestroy() {
        // Disconnect from MQTT Broker
        disconnect();

        super.onDestroy();
        Log.d(TAG, "The onDestroy() event");
    }
}
