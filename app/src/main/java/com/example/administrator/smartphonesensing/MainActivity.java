package com.example.administrator.smartphonesensing;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.support.v4.content.ContextCompat;
import android.Manifest;

import android.widget.Toast;

import static com.example.administrator.smartphonesensing.LogWriter.isExternalStorageWritable;

/**
 * Smart Phone Sensing Data Acquisition Code
 */
public class MainActivity extends Activity {

    /* The number of access points we are taking into consideration */
    private static int numSSIDs = 3;
    /* The number of RSS levels (e.g. 0..255) we are taking into consideration */
    private static int numRSSLvl = 100;
    /* The number of cells we are taking into consideration */
    private static int numRooms = 20;
    /* The number of samples per room we will be taking */
    private static int numScans = 40;
    /* The number of samples we take to detect movement */
    private static final int numACCSamples = 80;
    /* The number of particles we use for the particle system */
    private static final int numParticles = 1000;
    /* The number of particles selected for birthing */
    private static final int numTopParticles = 200;
    /* Stride length in cm */
    private static final int numStride = 48;


    ProbMassFuncs pmf;
    FloorMap floorMap3D;
    Sensors sensors;
    Compass compass;
    Movement movement;
    WifiScanner wifiScanner;
    ParticleFilter particleFilter;
    StepCounter stepCounter;

    private static final int REQUEST_CODE_WRITE_PERMISSION = 0;
    private static final int REQUEST_CODE_WIFI_PERMISSION = 0;

    private WifiManager wifiManager;

    TextView currentX,
            currentY,
            currentZ,
            titleAcc,
            textKNN,
            textAcc,
            textBayes,
            titleCfgApNum,
            titleCfgRssLvlNum,
            titleCfgRoomsNum,
            titleCfgScansNum,
            titleCfgStrideNum,
            titleTrainRoomNum,
            textTraining,
            textCompass,
            titleCfgCompassNum,
            textCurrentRoom;

    Button buttonRssi,
            buttonLocation,
            buttonWalk,
            buttonStand,
            buttonWalkOrStand,
            buttonBayesIterate,
            buttonBayesNew,
            buttonBayesCompile,
            buttonTest,
            buttonCfgApSubst,
            buttonCfgApAdd,
            buttonCfgRssLvlSubst,
            buttonCfgRssLvlAdd,
            buttonCfgRoomsSubst,
            buttonCfgRoomsAdd,
            buttonCfgScansSubst,
            buttonCfgScansAdd,
            buttonTrainRoomSubs,
            buttonTrainRoomAdd,
            buttonCfgCompassSubst,
            buttonCfgCompassAdd,
            buttonCfgStrideSubst,
            buttonCfgStrideAdd,
            buttonStartWalk;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for writing permission to external memory of the device
        if (isExternalStorageWritable())
            checkWritingPermission();
        checkWifiPermission();

        // Init the textViews
        initTextViews();

        // Init the buttons
        initButtons();

        // Init PMF
        pmf = new ProbMassFuncs(numRooms, numRSSLvl);
        if (pmf.loadPMF())
            Toast.makeText(MainActivity.this, "Loaded PMF", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, "No valid PMF found, created new", Toast.LENGTH_SHORT).show();

        // Init map
        floorMap3D = new FloorMap(this, this); //, numRoomsLit);

        // Init the particle filter
        particleFilter = new ParticleFilter(numParticles, numRooms, numTopParticles, floorMap3D, pmf, textCurrentRoom);

        // Init sensors
        compass = new Compass(textCompass, titleCfgCompassNum);
        movement = new Movement(currentX, currentY, currentZ, textAcc, numACCSamples);
        stepCounter = new StepCounter(particleFilter, compass, floorMap3D, numStride);
        sensors = new Sensors(this, compass, movement, stepCounter);
        sensors.start();



        // Init the wifi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanner = new WifiScanner(this, wifiManager, particleFilter, numSSIDs, numRSSLvl, numScans, numRooms, pmf,
                floorMap3D, textTraining, textKNN, textBayes);
        wifiScanner.init();
    }


    protected void onResume() {
        super.onResume();
        sensors.start();
    }


    protected void onPause() {
        super.onPause();
        sensors.stop();
    }

    public void initTextViews() {
        // Create the text views.
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);
        titleAcc = (TextView) findViewById(R.id.titleAcc);
        textKNN = (TextView) findViewById(R.id.textKNN);
        textAcc = (TextView) findViewById(R.id.textAcc);
        textBayes = (TextView) findViewById(R.id.textBAYES);
        titleCfgApNum = (TextView) findViewById(R.id.titleCfgApNum);
        titleCfgRssLvlNum = (TextView) findViewById(R.id.titleCfgRssLvlNum);
        titleCfgRoomsNum = (TextView) findViewById(R.id.titleCfgRoomsNum);
        titleCfgScansNum = (TextView) findViewById(R.id.titleCfgScansNum);
        titleTrainRoomNum = (TextView) findViewById(R.id.titleTrainRoomNum);
        textTraining = (TextView) findViewById(R.id.textTraining);
        textCompass = (TextView) findViewById(R.id.textCompass);
        textCurrentRoom = (TextView) findViewById(R.id.textCurrentRoom);
        titleCfgCompassNum = (TextView) findViewById(R.id.titleCfgCompassNum);
        titleCfgStrideNum = (TextView) findViewById(R.id.titleCfgStrideNum);

        // Set initial text for text views
        titleCfgApNum.setText(" " + numSSIDs + " ");
        titleCfgRssLvlNum.setText(" " + numRSSLvl + " ");
        titleCfgRoomsNum.setText(" " + numRooms + " ");
        titleCfgScansNum.setText(" " + numScans + " ");
        titleCfgStrideNum.setText(" " + numStride + " ");
        titleTrainRoomNum.setText(" 1 ");       // Safe. trainRoom is init to 0 in WifiScanner
    }

    public void initButtons() {

        // Create the buttons
        buttonRssi = (Button) findViewById(R.id.buttonRSSI);
        buttonLocation = (Button) findViewById(R.id.buttonLocation);
        buttonWalk = (Button) findViewById(R.id.buttonWalk);
        buttonStand = (Button) findViewById(R.id.buttonStand);
        buttonWalkOrStand = (Button) findViewById(R.id.buttonWalkOrStand);
        buttonBayesNew = (Button) findViewById(R.id.buttonBayesNew);
        buttonBayesIterate = (Button) findViewById(R.id.buttonBayesIterate);
        buttonBayesCompile = (Button) findViewById(R.id.buttonBayesCompile);
        buttonTest = (Button) findViewById(R.id.buttonTest);
        buttonCfgApSubst = (Button) findViewById(R.id.buttonCfgApSubst);
        buttonCfgApAdd = (Button) findViewById(R.id.buttonCfgApAdd);
        buttonCfgRssLvlSubst = (Button) findViewById(R.id.buttonCfgRssLvlSubst);
        buttonCfgRssLvlAdd = (Button) findViewById(R.id.buttonCfgRssLvlAdd);
        buttonCfgRoomsSubst = (Button) findViewById(R.id.buttonCfgRoomsSubst);
        buttonCfgRoomsAdd = (Button) findViewById(R.id.buttonCfgRoomsAdd);
        buttonCfgScansSubst = (Button) findViewById(R.id.buttonCfgScansSubst);
        buttonCfgScansAdd = (Button) findViewById(R.id.buttonCfgScansAdd);
        buttonTrainRoomSubs = (Button) findViewById(R.id.buttonTrainRoomSubs);
        buttonTrainRoomAdd = (Button) findViewById(R.id.buttonTrainRoomAdd);
        buttonCfgCompassSubst = (Button) findViewById(R.id.buttonCfgCompassSubst);
        buttonCfgCompassAdd = (Button) findViewById(R.id.buttonCfgCompassAdd);
        buttonCfgStrideSubst = (Button) findViewById(R.id.buttonCfgStrideSubst);
        buttonCfgStrideAdd = (Button) findViewById(R.id.buttonCfgStrideAdd);
        buttonStartWalk = (Button) findViewById(R.id.buttonStartWalk);

        // Create click listeners for our buttons
        buttonRssi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiScanner.startScan(WifiScanner.WifiScanAction.TRAINING);
            }
        });

        buttonLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // KNN
                textKNN.setText("Finding your location...");
                wifiScanner.startScan(WifiScanner.WifiScanAction.LOCATION_KNN);
            }
        });

        buttonBayesNew.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textBayes.setText("Starting over. Finding your location...");
                wifiScanner.startScan(WifiScanner.WifiScanAction.LOCATION_BAYES_NEW);
            }
        });

        buttonBayesIterate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textBayes.setText("Updating your location...");
                wifiScanner.startScan(WifiScanner.WifiScanAction.LOCATION_BAYES_ITER);
            }
        });

        buttonBayesCompile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // Calculate gaussian curves for all
                textTraining.setText("Calculating Gaussian distributions...");
                pmf.calcGauss();
                textTraining.setText("Gaussian distributions stored.");
            }
        });

        buttonWalk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                movement.setAccelAction(Movement.AccelScanAction.TRAIN_WALK);
                textAcc.setText("Start walking...");
            }
        });

        buttonStand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                movement.setAccelAction(Movement.AccelScanAction.TRAIN_STAND);
                textAcc.setText("Stand still...");
            }
        });

        buttonWalkOrStand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                movement.setAccelAction(Movement.AccelScanAction.DETECT_WALK);
                textAcc.setText("Tracking your movement...");
            }
        });

        buttonCfgApSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numSSIDs > 1){
                    numSSIDs -= 1;
                    titleCfgApNum.setText(" " + numSSIDs + " ");
                }
            }
        });

        buttonCfgApAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numSSIDs += 1;
                titleCfgApNum.setText(" " + numSSIDs + " ");
            }
        });

        buttonCfgRssLvlSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numRSSLvl > 10){
                    numRSSLvl -= 10;
                    titleCfgRssLvlNum.setText(" " + numRSSLvl + " ");
                }
            }
        });

        buttonCfgRssLvlAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numRSSLvl += 10;
                titleCfgRssLvlNum.setText(" " + numRSSLvl + " ");
            }
        });

        buttonCfgRoomsSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numRooms > 2){
                    numRooms -= 1;
                    titleCfgRoomsNum.setText(" " + numRooms + " ");
                }
            }
        });

        buttonCfgRoomsAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numRooms += 1;
                titleCfgRoomsNum.setText(" " + numRooms + " ");
            }
        });

        buttonCfgScansSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numScans > 10){
                    numScans -= 10;
                    titleCfgScansNum.setText(" " + numScans + " ");
                }
            }
        });

        buttonCfgScansAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numScans += 10;
                titleCfgScansNum.setText(" " + numScans + " ");
            }
        });

        buttonTrainRoomSubs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int room = wifiScanner.decTrainRoom();
                titleTrainRoomNum.setText(" " + (room + 1) + " ");
            }
        });

        buttonTrainRoomAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int room = wifiScanner.incTrainRoom();
                titleTrainRoomNum.setText(" " + (room + 1) + " ");
            }
        });

        buttonCfgStrideSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int stride = stepCounter.decStride();
                titleCfgStrideNum.setText(" " + (stride) + " ");
            }
        });

        buttonCfgStrideAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int stride = stepCounter.incStride();
                titleCfgStrideNum.setText(" " + (stride) + " ");
            }
        });

        buttonTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //floorMap3D.updateRooms(pmf, numRooms);
                stepCounter.incSteps(1);
            }
        });

        buttonCfgCompassAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                compass.incCalibration();
            }
        });

        buttonCfgCompassSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                compass.decCalibration();
            }
        });
        buttonStartWalk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!stepCounter.hasWalkStarted()) {
                    stepCounter.startWalk();
                    buttonStartWalk.setText("STOP");
                } else {
                    stepCounter.stopWalk();
                    buttonStartWalk.setText("WALK");
                }
            }
        });
    }

    private void checkWritingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // permission wasn't granted
//                if(LOG_INFO) Log.i(LOG_TAG,"No Write Permission!!");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_PERMISSION);
            }
        }
    }

    private void checkWifiPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // permission wasn't granted
//                if(LOG_INFO) Log.i(LOG_TAG,"No WiFi Permission!!");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_WIFI_PERMISSION);
            }
        }
    }
}

