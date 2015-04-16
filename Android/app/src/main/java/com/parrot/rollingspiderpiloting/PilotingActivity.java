package com.parrot.rollingspiderpiloting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.sql.Date;

public class PilotingActivity extends Activity implements DeviceControllerListener
{
    private static String TAG = PilotingActivity.class.getSimpleName();
    public static String EXTRA_DEVICE_SERVICE = "pilotingActivity.extra.device.service";

    public DeviceController deviceController;
    public ARDiscoveryDeviceService service;

    private Button emergencyBt;
    private Button takeoffBt;
    private Button landingBt;

    private JoystickView joystickLeft;
    private JoystickView joystickRight;

    private TextView debugLabel;

    private TextView batteryLabel;

    private AlertDialog alertDialog;

    public static float easeIn (float t,float b , float c, float d) {
        return c*(t/=d)*t*t + b;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piloting);

        debugLabel = (TextView) findViewById(R.id.debugLabel);


        emergencyBt = (Button) findViewById(R.id.emergencyBt);
        emergencyBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if (deviceController != null)
                {
                    deviceController.sendEmergency();
                }
            }
        });

        takeoffBt = (Button) findViewById(R.id.takeoffBt);
        takeoffBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if (deviceController != null)
                {
                    deviceController.sendTakeoff();
                }
            }
        });
        landingBt = (Button) findViewById(R.id.landingBt);
        landingBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if (deviceController != null)
                {
                    deviceController.sendLanding();
                }
            }
        });


        joystickLeft = (JoystickView) findViewById(R.id.joystickLeft);
        //Event listener that always returns the variation of the angle in degrees, motion power in percentage and direction of movement
        joystickLeft.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction) {

                int yaw = (int) (Math.sin(Math.toRadians(angle))*power);
                int sign = 1;
                if(yaw < 0){
                    yaw = Math.abs(yaw);
                    sign = -1;
                }
                yaw = (int) easeIn(yaw,0,100*sign,100);

                int gaz = (int) (Math.cos(Math.toRadians(angle))*power);
                sign = 1;
                if(gaz < 0){
                    gaz = Math.abs(gaz);
                    sign = -1;
                }
                gaz = (int) easeIn(gaz,0,100*sign,100);
                if (deviceController != null)
                {
                    debugLabel.setText(String.format("yaw: %d, gaz: %d",yaw, gaz));
                    deviceController.setYaw((byte) yaw);
                    deviceController.setGaz((byte) gaz);
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);

        joystickRight = (JoystickView) findViewById(R.id.joystickRight);
        //Event listener that always returns the variation of the angle in degrees, motion power in percentage and direction of movement
        joystickRight.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction) {

                power = (int) easeIn(power,0,100,100);

                int roll = (int) (Math.sin(Math.toRadians(angle))*power);
                int pitch = (int) (Math.cos(Math.toRadians(angle))*power);
                if (deviceController != null)
                {
                    //debugLabel.setText(String.format("roll: %d",roll));
                    deviceController.setRoll((byte)roll);
                    deviceController.setPitch((byte)pitch);
                    if (pitch != 0 || roll != 0) {
                        deviceController.setFlag((byte)1);
                    } else {
                        deviceController.setFlag((byte)0);
                    }
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);



        batteryLabel = (TextView) findViewById(R.id.batteryLabel);

        Intent intent = getIntent();
        service = intent.getParcelableExtra(EXTRA_DEVICE_SERVICE);

        deviceController = new DeviceController(this, service);
        deviceController.setListener(this);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        if (deviceController != null)
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PilotingActivity.this);

            // set title
            alertDialogBuilder.setTitle("Connecting ...");

            // create alert dialog
            alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    boolean failed = false;

                    failed = deviceController.start();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            alertDialog.dismiss();
                        }
                    });

                    if (failed)
                    {
                        finish();
                    }
                    else
                    {
                        //only with RollingSpider in version 1.97 : date and time must be sent to permit a reconnection
                        Date currentDate = new Date(System.currentTimeMillis());
                        deviceController.sendDate(currentDate);
                        deviceController.sendTime(currentDate);

                        deviceController.sendMaxRotationSpeed(360);
                        deviceController.sendMaxTiltSpeed(16);
                        deviceController.sendMaxVerticalSpeed(1.5f);
                        deviceController.sendMaxAltitude(3);
                    }
                }
            }).start();

        }
    }

    private void stopDeviceController()
    {
        if (deviceController != null)
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PilotingActivity.this);

            // set title
            alertDialogBuilder.setTitle("Disconnecting ...");

            // create alert dialog
            alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    deviceController.stop();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            alertDialog.dismiss();
                            finish();
                        }
                    });

                }
            }).start();
        }
    }

    @Override
    protected void onStop()
    {
        stopDeviceController();

        super.onStop();
    }

    @Override
    public void onBackPressed()
    {
        stopDeviceController();
    }

    @Override
    public void onDisconnect()
    {
        stopDeviceController();
    }

    @Override
    public void onUpdateBattery(final byte percent)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                batteryLabel.setText(String.format("%d%%", percent));
            }
        });

    }
}
