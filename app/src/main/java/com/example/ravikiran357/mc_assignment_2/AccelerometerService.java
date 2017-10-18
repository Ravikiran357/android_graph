package com.example.ravikiran357.mc_assignment_2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class AccelerometerService extends Service implements SensorEventListener {

    float x_value;
    float y_value;
    float z_value;
    public static int timeDelay = 100;
    public long lastSaved;
    final static String INTENT_ACCELEROMETER_DATA = "ACCELEROMETER_DATA";

    @Override
    public void onCreate(){
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        lastSaved = System.currentTimeMillis();
        Log.i("accelerometerService", "Created");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if(System.currentTimeMillis() - lastSaved > timeDelay) { // Or start a new thread to
                lastSaved = System.currentTimeMillis();
                getAccelerometer(sensorEvent);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        return START_STICKY;
    }

    private void getAccelerometer(SensorEvent event)
    {
        float[] values = event.values;
        x_value = values[0];
        y_value = values[1];
        z_value = values[2];

        Intent intent = new Intent();
        intent.setAction(INTENT_ACCELEROMETER_DATA);
        intent.putExtra("X", x_value);
        intent.putExtra("Y", y_value);
        intent.putExtra("Z", z_value);
        sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}