/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008-2012. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package edu.wpi.first.wpilibj.communication;

import com.sun.cldc.jna.Structure;
import org._649mset.RobotEmulator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Structure for data exchanged between the robot and the driver station.
 */
public final class FRCCommonControlData extends Structure {

    public static final byte ESTOP_BIT = 0x40;
    public static final byte ENABLED_BIT = 0x20;
    public static final byte AUTONOMOUS_BIT = 0x10;
    public static final byte FMS_ATTATCHED = 0x08;
    public static final byte RESYNCH = 0x04;
    public static final byte TEST_MODE_BIT = 0x02;
    public static final byte CHECK_VERSIONS_BIT = 0x01;

    /**
     * The index of the packet
     */
    public /*UINT16*/ int packetIndex;
    /**
     * The control mode e.g. Autonomous, E-stop, isEnabled ...
     */
    public AtomicInteger/*UINT8*/ control;
    // { reset, notEStop, isEnabled, isAutonomousMode, fmsAttached, resync, cRIOChkSum, fpgaChkSum }

    /**
     * Determine if the robot should be isEnabled
     *
     * @return true if the robot is isEnabled
     */
    public boolean isEnabled() {
        synchronized (this) {
            final byte tempControl = (byte) control.get();
            final byte tempEnabled = ENABLED_BIT;
            boolean b = (tempControl& tempEnabled) == tempEnabled;
            if (!b) {
            }

            return b;
        }
    }

    /**
     * Determine if the robot is in test mode
     *
     * @return true if the robot is in test mode
     */
    public boolean isTestMode() {
        return (control.get() & TEST_MODE_BIT) == TEST_MODE_BIT;
    }

    /**
     * Determine if the robot should be in isAutonomousMode
     *
     * @return true if the robot is in isAutonomousMode
     */
    public boolean isAutonomousMode() {
        return (control.get() & AUTONOMOUS_BIT) == AUTONOMOUS_BIT;
    }

    /**
     * The state of the digital inputs on the ds
     */
    public /*UINT8*/ short dsDigitalIn;
    /**
     * The team number from the ds
     */
    public /*UINT16*/ int teamID;
    /**
     * Which alliance the robot is on
     */
    public char dsID_Alliance;
    /**
     * The position of the controls on the alliance station wall.
     */
    public char dsID_Position;
    /**
     * Position of the axes of the first js
     */
    public byte[] stick0Axes = new byte[6];
    /**
     * Button state of the first js
     */
    public short stick0Buttons;        // Left-most 4 bits are unused
    /**
     * Position of the axes of the second js
     */
    public byte[] stick1Axes = new byte[6];
    /**
     * Button state of the second js
     */
    public short stick1Buttons;        // Left-most 4 bits are unused
    /**
     * Position of the axes of the third js
     */
    public byte[] stick2Axes = new byte[6];
    /**
     * Button state of the third js
     */
    public short stick2Buttons;        // Left-most 4 bits are unused
    /**
     * Position of the axes of the fourth js
     */
    public byte[] stick3Axes = new byte[6];
    /**
     * Button state of the fourth js
     */
    public short stick3Buttons;        // Left-most 4 bits are unused
    //Analog inputs are 10 bit right-justified
    /**
     * Driver Station analog input
     */
    public short analog1;
    /**
     * Driver Station analog input
     */
    public short analog2;
    /**
     * Driver Station analog input
     */
    public short analog3;
    /**
     * Driver Station analog input
     */
    public short analog4;

    // Other fields are used by the lower-level comm system and not needed by robot code:

    /**
     * Create a new FRCControlData structure
     */
    public FRCCommonControlData() {
//        allocateMemory();
    }

    /**
     * Method to free the memory used by this structure
     */
    protected void free() {
        freeMemory();
    }

    /**
     * Read new data in the structure
     */
    public void read() {
//        packetIndex = backingNativeMemory.getShort(0) & 0xFFFF;
        switch (RobotEmulator.getInstance().getRobotMode()) {
            case TELEOP:
                control = new AtomicInteger(0);
                break;
            case AUTONOMOUS:
                control = new AtomicInteger(AUTONOMOUS_BIT);
                break;
            case TEST:
                control = new AtomicInteger(TEST_MODE_BIT);
                break;
        }
        if (RobotEmulator.getInstance().isRobotEnabled())
            control.set(control.get() | ENABLED_BIT);
//
        dsDigitalIn = RobotEmulator.getInstance().getDSDigitalIn();
        teamID = RobotEmulator.getInstance().getTeamId();
//
        dsID_Alliance = RobotEmulator.getInstance().getAlliance();
        dsID_Position = RobotEmulator.getInstance().getPosition();
//
        stick0Axes = RobotEmulator.getInstance().getAxes(0);
        stick0Buttons = RobotEmulator.getInstance().getButtons(0);

        stick1Axes = RobotEmulator.getInstance().getAxes(1);
        stick1Buttons = RobotEmulator.getInstance().getButtons(1);

        stick2Axes = RobotEmulator.getInstance().getAxes(2);
        stick2Buttons = RobotEmulator.getInstance().getButtons(2);

        stick3Axes = RobotEmulator.getInstance().getAxes(3);
        stick3Buttons = RobotEmulator.getInstance().getButtons(3);
//
        analog1 = RobotEmulator.getInstance().getDSAnalogIn(0);
        analog2 = RobotEmulator.getInstance().getDSAnalogIn(1);
        analog3 = RobotEmulator.getInstance().getDSAnalogIn(2);
        analog4 = RobotEmulator.getInstance().getDSAnalogIn(3);

        // Other fields are used by the lower-level comm system and not needed by robot code
    }

    /**
     * Write new data in the structure
     */
    public void write() {
        throw new IllegalStateException("FRCCommonControlData is not writable");
    }

    /**
     * Get the size of the structure
     *
     * @return size of the structure
     */
    public int size() {
        return 80;
    }

}
