package mas;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;


public class SimulatorSettings {
    // SIMULATOR
    // Tick length = 1s, do not change this, probabilities for new task and new road works are done per tick.
    public static final long TICK_LENGTH = 1000L;
    public static final long SIMULATION_LENGTH = 10 * 60 * 60 * 1000;
    public static final int SIM_SPEEDUP = 1;
    public static final boolean VERBOSE = true;
    public static final boolean SHOW_GUI = true;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int EXPERIMENT_REPEATS = 100;
    public static final int EXPERIMENT_THREADS = 6;

    // CITY AND ROBOTS
    public static final int CITY_SIZE = 12;
    public static final int NUM_ROBOTS = 4;
    public static final int ROBOT_LENGTH = 1;
    public static final int NODE_DISTANCE = 2 * ROBOT_LENGTH;
    public static final double ROBOT_SPEED = 1;
    public static final Unit<Length> DISTANCE_UNIT = SI.METER;
    public static final Unit<Velocity> SPEED_UNIT = SI.METRES_PER_SECOND;
    public static final int ROBOT_CAPACITY = 5;
    public static final int CHARGING_STATION_ROBOT_CAPACITY = 1;
    // Make battery capacity relative to city size so that every node can be visited once before the battery is drained
    public static final double BATTERY_CAPACITY = CITY_SIZE * CITY_SIZE * NODE_DISTANCE;
    // Make the batteries be recharged with 1% every tick.
    public static final double BATTERY_RECHARGE_CAPACITY = 0.01 * BATTERY_CAPACITY;
    // Paths to explore when using exploration ants
    public static final int ALTERNATIVE_PATHS_TO_EXPLORE = 3;

    // TIMINGS (IN MILLISECONDS)
    // Let road works stay for a time relative to the size of the city
    public static final long TIME_ROAD_WORKS = (long) ((CITY_SIZE * CITY_SIZE * NODE_DISTANCE) * 1000);
    // If a robot's battery dies because it didn't recharge fast enough, rescue it in an amount of time dependent
    // on the battery charge capacity. Larger battery takes longer to load. => make rescue delay 5 time longer than that
    // as a punishment for draining the battery.
    public static final long BATTERY_RESCUE_DELAY = (long) (5 * (BATTERY_CAPACITY / BATTERY_RECHARGE_CAPACITY) * TICK_LENGTH);
    // Lifetime of a reservation. Make it long enough so that the robot can make one revolution around the city block
    public static final long INTENTION_RESERVATION_LIFETIME = (long) (4 * CITY_SIZE * NODE_DISTANCE / ROBOT_SPEED) * 1000;
    // Time to refresh explorations
    public static final long EXPLORATION_REFRESH_TIME = (long) (0.4 * INTENTION_RESERVATION_LIFETIME);
    // Time to refresh intentions
    public static final long INTENTION_REFRESH_TIME = (long) (0.5 * INTENTION_RESERVATION_LIFETIME);


    // PROBABILITIES
    public static final double PROB_NEW_DELIVERY_TASK = 0.0015 * CITY_SIZE;
    public static final double PROB_NEW_ROAD_WORKS = 0.0025 * CITY_SIZE;
    public static final double PIZZA_AMOUNT_STD = 0.75;
    public static final double PIZZA_AMOUNT_MEAN = 4;
}