package mas;

public class SimulatorSettings {
    public static final long TICK_LENGTH = 1000L;
    public static final long RANDOM_SEED = 123L;
    public static final int SIM_SPEEDUP = 1;

    public static final int NUM_ROBOTS = 1;
    public static final int ROBOT_CAPACITY = 5;
    public static final int BATTERY_CAPACITY = 100;
    public static final int VEHICLE_LENGTH = 1;
    public static final double VEHICLE_SPEED_KMH = 1;

    public static final double PROB_NEW_PARCEL = .02;
    public static final double PROB_PIZZERIA_OPEN = .002;
    public static final double PROB_PIZZERIA_CLOSE = .002;
    public static final double PROB_ROAD_WORKS_START = .005;
    public static final double PROB_ROAD_WORKS_END = .005;
    public static final double PIZZA_AMOUNT_STD = 0.75;
    public static final double PIZZA_AMOUNT_MEAN = 4;
    public static final int ALTERNATIVE_PATHS_TO_EXPLORE = 3;

    public static final long INTENTION_RESERVATION_LIFETIME = 10000L; // 10 seconds
}
