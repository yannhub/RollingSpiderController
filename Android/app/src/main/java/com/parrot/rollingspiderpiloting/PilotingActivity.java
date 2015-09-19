package com.parrot.rollingspiderpiloting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.sql.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PilotingActivity extends Activity implements DeviceControllerListener
{
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static String TAG = PilotingActivity.class.getSimpleName();
    public static String EXTRA_DEVICE_SERVICE = "pilotingActivity.extra.device.service";

    public DeviceController deviceController;
    public ARDiscoveryDeviceService service;

    private Button emergencyBt;
    private Button takeoffBt;
    private Button landingBt;

    private Button autoPilotBt;

    private JoystickView joystickLeft;
    private JoystickView joystickRight;

    private TextView debugLabel;
    private TextView yawLabel;
    private TextView gazLabel;
    private TextView rollLabel;
    private TextView pitchLabel;

    private TextView batteryLabel;

    private AlertDialog alertDialog;

    public static float easeIn (float t,float b , float c, float d) {
        return c*(t/=d)*t*t + b;
    }

    public static float easeOut (float t,float b , float c, float d) {
        return c*((t=t/d-1)*t*t + 1) + b;
    }

    public static float  easeInOut(float t,float b , float c, float d) {
        if ((t/=d/2) < 1) return -c/2 * ((float)Math.sqrt(1 - t*t) - 1) + b;
        return c/2 * ((float)Math.sqrt(1 - (t-=2)*t) + 1) + b;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piloting);

        debugLabel = (TextView) findViewById(R.id.debugLabel);
        yawLabel = (TextView) findViewById(R.id.yawLabel);
        gazLabel = (TextView) findViewById(R.id.gazLabel);
        rollLabel = (TextView) findViewById(R.id.rollLabel);
        pitchLabel = (TextView) findViewById(R.id.pitchLabel);


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

        autoPilotBt = (Button) findViewById(R.id.autoPilotBt);
        autoPilotBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {

                deviceController.sendMaxRotationSpeed(360);
                deviceController.sendMaxTiltSpeed(12);
                deviceController.sendMaxVerticalSpeed(1.5f);


                PilotingCommandTask pilotingCommandTask = new PilotingCommandTask(deviceController, scheduler);

                PilotingCommand takeoffCommand = new PilotingCommand(true, false, 0, 0, 0, 0, 0);
                pilotingCommandTask.pilotingCommands.add(takeoffCommand);

                PilotingCommand waitCommand = new PilotingCommand(true, false, 0, 0, 0, 0, 6000);
                pilotingCommandTask.pilotingCommands.add(waitCommand);

                int delay =  20;
                int power = 12;
                int nbSteps = 400;

                int nbStepsByPhase = (int)nbSteps/4;
                int nbStepPhase1 = nbStepsByPhase;
                int nbStepPhase2 = nbStepsByPhase*2;
                int nbStepPhase3 = nbStepsByPhase*3;

                for (int step=0; step<=nbSteps; step++) {
                    int gaz = 0;
                    int roll = 0;

                    if(step > nbStepPhase3) {
                        gaz = -power;
                        roll = (int) (power*1.5);
                    } else if (step > nbStepPhase2) {
                        gaz = -power;
                        roll = -power;
                    } else if (step > nbStepPhase1) {
                        gaz = power;
                        roll = (int) (-power*1.5);
                    } else {
                        gaz = power;
                        roll = power;
                    }
                    PilotingCommand pilotingCommand = new PilotingCommand(false, false, gaz, 0, 0, roll, delay);
                    pilotingCommandTask.pilotingCommands.add(pilotingCommand);
                }

                PilotingCommand landingCommand = new PilotingCommand(false, true, 0, 0, 0, 0, 0);
                pilotingCommandTask.pilotingCommands.add(landingCommand);

                scheduler.schedule(pilotingCommandTask, takeoffCommand.delay, TimeUnit.MILLISECONDS);
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
                    yawLabel.setText(String.format("yaw: %d", yaw));
                    gazLabel.setText(String.format("gaz: %d", gaz));
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
                    rollLabel.setText(String.format("roll: %d", roll));
                    pitchLabel.setText(String.format("pitch: %d", pitch));
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
