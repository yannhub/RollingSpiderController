package com.parrot.rollingspiderpiloting;

/**
 * Created by yann on 29/07/15.
 */
public class PilotingCommand {

    public Boolean takeoff;
    public Boolean landing;
    public int gaz;
    public int yaw;
    public int pitch;
    public int roll;

    public int delay;

    public PilotingCommand(Boolean takeoff, Boolean landing, int gaz, int yaw, int pitch, int roll, int delay) {
        this.takeoff = takeoff;
        this.landing = landing;
        this.gaz = gaz;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.delay = delay;
    }
}
