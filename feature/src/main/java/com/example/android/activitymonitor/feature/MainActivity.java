package com.example.android.activitymonitor.feature;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    //array lists to hold all the changes in x y z
    private static List<Float> xAccelVal;
    private static List<Float> yAccelVal;
    private static List<Float> zAccelVal;

    private static List<Float> xGyroVal;
    private static List<Float> yGyroVal;
    private static List<Float> zGyroVal;

    private static List<Float> xRawAccelVal;
    private static List<Float> yRawAccelVal;
    private static List<Float> zRawAccelVal;

    //textview to display our action
    private String resultStr;
    private String curAction = null;
    private TextView predictDisplay;
    private TextView distanceTravel;
    private TextView speed;
    private TextView averageSpeed;
    private TextView totalDistance;

    private EditText usid;
    private EditText fname;
    private EditText lname;

    LocationManager locM;
    LocationListener locL;

    List<Location> locations;
    List<Double> avgSpeed;
    List<Double> totalDist;

    private String currentSpeed;
    private String currentDistance;

    //id, first name, and last name is hardCoded atm but would be replaced with asking the user for user name
    //and checking a database of already existing users to make sure its not taken
    String userId;
    String userFName;
    String userLName;

    Context thisContext;

    //our classifier which uses the model built by our neural network
    private Classifier classifier;

    //function called when app is opened
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Start the main activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thisContext = this;
        locations = new ArrayList<>();
        avgSpeed = new ArrayList<>();
        totalDist = new ArrayList<>();

        //array lists to hold all the changes in x y z
        xAccelVal = new ArrayList<>();
        yAccelVal = new ArrayList<>();
        zAccelVal = new ArrayList<>();

        xGyroVal = new ArrayList<>();
        yGyroVal = new ArrayList<>();
        zGyroVal = new ArrayList<>();

        xRawAccelVal = new ArrayList<>();
        yRawAccelVal = new ArrayList<>();
        zRawAccelVal = new ArrayList<>();
        //TextView to display action being performed
        usid = findViewById(R.id.userid);
        fname = findViewById(R.id.firstname);
        lname = findViewById(R.id.lastname);
        predictDisplay = findViewById(R.id.someActivity);
        distanceTravel = findViewById(R.id.distanceT);
        speed = findViewById(R.id.speed);
        averageSpeed = findViewById(R.id.avgSpeed);
        totalDistance = findViewById(R.id.totalDistance);

        //initialize our tensorflow classifier
        classifier = new Classifier(getApplicationContext());

        locM = (LocationManager) thisContext.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(thisContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&ActivityCompat.checkSelfPermission(thisContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            verifyLocationPermissions(this);
        }

        locL = new LocationListener() {
            private Location prevLocation;

            @Override
            public void onLocationChanged(Location location) {
                getSpeed(location);
                getDistance(location);
                locations.add(location);
                this.prevLocation = location;

                userId = usid.getText().toString();
                userFName = fname.getText().toString();
                userLName = lname.getText().toString();

                SimpleDateFormat curtimesdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
                String curtime = curtimesdf.format(new Date());
                SimpleDateFormat curdatesdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                String curdate = curdatesdf.format(new Date());

                if(prevLocation != null && curAction != null) {
                    if(!curAction.equals("null") && !userId.equals("userid") && !userId.equals("")) {
                        String writeToFile = userId + "\t" + userLName + "\t" + userFName
                                + "\t" + curAction + "\t" + currentSpeed + "\t" + currentDistance
                                + "\t" + location.getLongitude() + "\t" + location.getLatitude()
                                + "\t" + curtime + "\t" + curdate + "\n";
                        writeToFile(writeToFile);
                    }
                }
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }
            @Override
            public void onProviderEnabled(String s) {

            }
            @Override
            public void onProviderDisabled(String s) {

            }
            private void getSpeed(Location location) {
                double curSpeed = 0.00;
                if (prevLocation != null) {
                    double elapsedTime = (location.getTime() - prevLocation.getTime()) / 1_000; // Convert milliseconds to seconds
                    curSpeed = prevLocation.distanceTo(location) / elapsedTime;

                    if(curSpeed > 12.422222){
                        curSpeed = 0.000;
                    }

                    avgSpeed.add(curSpeed);
                    double aSp = 0.00;
                    for (Double sp : avgSpeed) {
                        aSp = aSp + sp;
                    }
                    aSp = aSp / avgSpeed.size();
                    String meanSpeed = String.format(Locale.US,"%.3f", aSp);
                    meanSpeed = "Average Speed: " + meanSpeed + " m/s";
                    averageSpeed.setText(meanSpeed);
                }

                currentSpeed = String.format(Locale.US,"%.3f",curSpeed);

                String spd = "Speed: " + currentSpeed + " m/s";
                speed.setText(spd);
            }
            private void getDistance(Location location){
                double d = 0.00;
                if(prevLocation != null) {
                    final int R = 6371;
                    double dLat = deg2rad(location.getLatitude() - prevLocation.getLatitude());
                    double dLon = deg2rad(location.getLongitude() - prevLocation.getLongitude());
                    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(deg2rad(prevLocation.getLatitude())) * Math.cos(deg2rad(location.getLatitude())) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
                    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                    d = R * c;
                    d = d * 1000;

                    totalDist.add(d);
                    double sumDist = 0.00;
                    for (Double ds : totalDist) {
                        sumDist = sumDist + ds;
                    }
                    String sumDistance = String.format(Locale.US,"%.3f", sumDist);
                    sumDistance = "Total Distance: " + sumDistance + " m";
                    totalDistance.setText(sumDistance);
                }

                currentDistance = String.format(Locale.US,"%.3f",d);
                String dist = "Distance: " + currentDistance + " m";
                distanceTravel.setText(dist);
            }
            private double deg2rad(double deg) {
                return deg * (Math.PI / 180);
            }
        };

        if (locM.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locL);
        }

    }

    private static final int REQUEST_LOCATION_SERVICE = 1;
    private static String[] PERMISSIONS_LOCATION_SERVICE = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyLocationPermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
        int permission2 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_LOCATION_SERVICE,
                    REQUEST_LOCATION_SERVICE
            );
        }
    }

    private void writeToFile(String data) {
        Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        File file;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "userData.txt");
            outputStream = new FileOutputStream(file, true);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //function used to display the action and probability
    public void displayAction(float highestProb){
        //round the probability up to 3 decimal places
        BigDecimal bd = new BigDecimal(Float.toString(highestProb));
        bd = bd.setScale(3, BigDecimal.ROUND_HALF_UP);

        //add the action and its probability to a string in order to display it
        curAction = resultStr;
        resultStr = resultStr + ", Prob: " + bd;
        predictDisplay.setText(resultStr);
        resultStr = "";
    }

    //function used for every time the sensors read a change they store the data
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        actClassifier();
        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            xAccelVal.add(sensorEvent.values[0]);
            yAccelVal.add(sensorEvent.values[1]);
            zAccelVal.add(sensorEvent.values[2]);
        }
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            xGyroVal.add(sensorEvent.values[0]);
            yGyroVal.add(sensorEvent.values[1]);
            zGyroVal.add(sensorEvent.values[2]);
        }
        else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            xRawAccelVal.add(sensorEvent.values[0]);
            yRawAccelVal.add(sensorEvent.values[1]);
            zRawAccelVal.add(sensorEvent.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    protected void onPause() {
        //if program is paused stop listening to the sensors
        getSensorManager().unregisterListener(this);
        super.onPause();
    }

    protected void onResume() {
        //if program is resumed start listening to the sensors again
        super.onResume();
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
    }

    private SensorManager getSensorManager() {
        //get our sensor manager
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    //function used to send our data to the classifier and get a result and probability
    private void actClassifier() {
        //every 128 values we send our data to our classifier in TensorFlow to see what action is being performed and display it
        if (xAccelVal.size() == 128 && yAccelVal.size() == 128 && zAccelVal.size() == 128
                && xGyroVal.size() == 128 && yGyroVal.size() == 128 && zGyroVal.size() == 128
                && xRawAccelVal.size() == 128 && yRawAccelVal.size() == 128 && zRawAccelVal.size() == 128) {
            //make a list to hold all our data collected
            List<Float> values = new ArrayList<>();
            //variable to hold the largest of our probabilities
            float result;
            //array to hold the probability of each action
            float[] predict;
            values.addAll(xAccelVal);
            values.addAll(yAccelVal);
            values.addAll(zAccelVal);

            values.addAll(xGyroVal);
            values.addAll(yGyroVal);
            values.addAll(zGyroVal);

            values.addAll(xRawAccelVal);
            values.addAll(yRawAccelVal);
            values.addAll(zRawAccelVal);

            //use our classifier to get the prediction
            predict = classifier.predict(fListToArray(values));

            //get which probability is the highest
            result = largestProb(predict);

            //display the results on our screen
            displayAction(result);

            //reset the lists
            xAccelVal.clear();
            yAccelVal.clear();
            zAccelVal.clear();

            xGyroVal.clear();
            yGyroVal.clear();
            zGyroVal.clear();

            xRawAccelVal.clear();
            yRawAccelVal.clear();
            zRawAccelVal.clear();
        }
    }

    //function used to convert our Float list to a float array
    private float[] fListToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++){
            array[i] = list.get(i);
        }
        return array;
    }

    //edit this function for walking and jogging
    //function used to find the largest probability and which acton it corresponds to
    private float largestProb(float[] resultsProb){
        float rtnResult = 0;
        for(int i = 0; i < resultsProb.length; i++){
            if(resultsProb[i] > rtnResult){
                rtnResult = resultsProb[i];
                //old 0 down 1 jogg 2 sit 3 stand 4 up 5 walk
                switch(i) {
                    case 0: resultStr = "Walking";
                            break;
                    case 1: resultStr = "Jogging";
                            break;
                }
            }
        }
        return rtnResult;
    }

}
