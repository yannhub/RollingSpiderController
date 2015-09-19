package com.parrot.rollingspiderpiloting;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PilotingCommandTask implements Runnable {

    public DeviceController deviceController;

    public ArrayDeque<PilotingCommand> pilotingCommands;

    public ScheduledExecutorService scheduler;

    public PilotingCommandTask(DeviceController deviceController, ScheduledExecutorService scheduler) {
        this.deviceController = deviceController;
        this.scheduler = scheduler;
        this.pilotingCommands = new ArrayDeque<>();
    }

    @Override
    public void run() {
        System.out.println("Timer task started at:"+new Date());
        if(!pilotingCommands.isEmpty()) {

            PilotingCommand pilotingCommand = pilotingCommands.removeFirst();

            if (deviceController != null) {

                deviceController.setGaz((byte) pilotingCommand.gaz);
                deviceController.setYaw((byte) pilotingCommand.yaw);
                deviceController.setPitch((byte) pilotingCommand.pitch);
                deviceController.setRoll((byte) pilotingCommand.roll);

                if (pilotingCommand.pitch != 0 || pilotingCommand.roll != 0) {
                    deviceController.setFlag((byte) 1);
                } else {
                    deviceController.setFlag((byte) 0);
                }

                if (pilotingCommand.takeoff) {
                    deviceController.sendTakeoff();
                }

                if (pilotingCommand.landing) {
                    deviceController.sendLanding();
                }
            }


            PilotingCommand nextPilotingCommand = pilotingCommands.getFirst();

            scheduler.schedule(this, nextPilotingCommand.delay, TimeUnit.MILLISECONDS);
        }
        System.out.println("Timer task finished at:" + new Date());
    }

}
