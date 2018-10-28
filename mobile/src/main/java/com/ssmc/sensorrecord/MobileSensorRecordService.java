package com.ssmc.sensorrecord;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.ssmc.sensordesc.SensorRecord;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 读取传感器数据并异步存储到本地
 */
public class MobileSensorRecordService extends Service {

    private static String TAG = "MobileSensorRecordService";

    private SensorManager mSensorManager;
    private SensorEventListener mSensorEventListener;
    public static SensorDataWriter mSensorDataWriter;
    private long startTime;

    @Override
    public void onCreate() {
        super.onCreate();
        //1.获取SensorManager实例
        mSensorManager = (SensorManager) this.getSystemService(Service.SENSOR_SERVICE);
        mSensorEventListener = new MySensorEventListener();
        mSensorDataWriter = new SensorDataWriter("mobile");
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new SensorRecordBinder();
    }

    /**
     * 监听器，监听传感器返回的数据
     * {@link SensorEventListener}
     */
    public class MySensorEventListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float secondToBegin = (System.currentTimeMillis() - startTime) / 1000.00f; //计算从任务开始到现在的用时
            SensorRecord sensorRecord = new SensorRecord(event, System.currentTimeMillis(), secondToBegin);
            try {
                mSensorDataWriter.writeSensorData(sensorRecord);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    public static void writeWearData(SensorRecord sensorRecord){
        try {
            mSensorDataWriter.writeSensorData(sensorRecord);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Binder实现类
     */
    public class SensorRecordBinder extends Binder {
        /**
         * 注册待监听的传感器接收器，将需要监听的传感器的常量{@link Sensor}放入集合传入进来，
         * 举例来说，如果想要监听加速度，可以把常量{@link Sensor#TYPE_ACCELEROMETER}作为
         * sensorTypes中的一个变量传入。如果无法监听某个传感器，将会把该传感器的常量保存在
         * 一个集合中并返回这个常量集合。
         *
         * @param sensorTypes {@link Sensor}中代表传感器的常量如{@link Sensor#TYPE_ACCELEROMETER}
         * @return 无法监听的传感器对应的常量
         */
        public List<Integer> start(List<Integer> sensorTypes) {
            //2.注册对应传感器的监听器
            List<Integer> cannotRegister = new ArrayList<>();
            for (int sensorType : sensorTypes) {
                if (mSensorManager.getDefaultSensor(sensorType) != null) {
                    mSensorManager.registerListener(mSensorEventListener,
                            mSensorManager.getDefaultSensor(sensorType),
                            SensorManager.SENSOR_DELAY_FASTEST);
                } else {
                    cannotRegister.add(sensorType);
                }
            }
            startTime = System.currentTimeMillis();
            return cannotRegister.isEmpty() ? null : cannotRegister;
        }

        public void stop() {
            startTime = 0;
            mSensorManager.unregisterListener(mSensorEventListener);
            try {
                mSensorDataWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
