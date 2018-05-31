package mas;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;


public class SimulatorSettings {
    public static final int SIM_SPEEDUP = 1;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;


    // Tick length = 1s
    public static final long TICK_LENGTH = 1000L;


    public static final int CITY_SIZE = 5;
    public static final int NUM_ROBOTS = 1;
    public static final int ROBOT_CAPACITY = 5;
    public static final int CHARGING_STATION_CAPACITY = 3;
    public static final double BATTERY_CAPACITY = 2*CITY_SIZE*CITY_SIZE;
    // When a robot's battery dies because it didn't go to a charging station quickly enough and it gets recharged
    // e.g. by some worker that went to the robot, then reset the battery capacity to 30%.
    public static final double BATTERY_RESCUE_CAPACITY = BATTERY_CAPACITY*0.3;


    // Other timings in milliseconds
    // Want road works to stay for the time that a robot can do 15 moves to a next node
    public static final long TIME_ROAD_WORKS = (long) ((15 * 2 / SimulatorSettings.VEHICLE_SPEED) * 1000);
    // If a robot's battery dies because it didn't recharge fast enough, rescue it in an amount of time dependent
    // on the size of the city.
    public static final long BATTERY_RESCUE_DELAY = 30 * 1000;
    // Lifetime of a reservation
    public static final long INTENTION_RESERVATION_LIFETIME = 30000L;
    public static final long REFRESH_INTENTIONS = 200000000000000000L;
    public static final long REFRESH_EXPLORATIONS = 20000000000000000L;

    public static final int VEHICLE_LENGTH = 1;
    public static final double VEHICLE_SPEED = 0.1;
    public static final Unit<Length> DISTANCE_UNIT = SI.METER;
    public static final Unit<Velocity> SPEED_UNIT = SI.METRES_PER_SECOND;

    public static final double PROB_NEW_PARCEL = .05;
    public static final double PROB_NEW_ROAD_WORKS = 0.01;
    public static final double PIZZA_AMOUNT_STD = 0.75;
    public static final double PIZZA_AMOUNT_MEAN = 4;
    public static final int ALTERNATIVE_PATHS_TO_EXPLORE = 2;
}
