package mas;

public class SimulatorSettings {
    public static final long TICK_LENGTH = 100L;
    public static final int SIM_SPEEDUP = 2;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;

    public static final int CITY_SIZE = 6;
    public static final int NUM_ROBOTS = 1;
    public static final int ROBOT_CAPACITY = 5;
    public static final int BATTERY_CAPACITY = 100;
    public static final int VEHICLE_LENGTH = 1;
    public static final double VEHICLE_SPEED_KMH = 10;

    public static final double PROB_NEW_PARCEL = .02;
    public static final double PROB_PIZZERIA_OPEN = .002;
    public static final double PROB_PIZZERIA_CLOSE = .002;
    public static final double PROB_NEW_ROAD_WORKS = 0.0;
    public static final long TIME_ROAD_WORKS = 15000;
    public static final double PIZZA_AMOUNT_STD = 0.75;
    public static final double PIZZA_AMOUNT_MEAN = 4;
    public static final int ALTERNATIVE_PATHS_TO_EXPLORE = 1;

    public static final long INTENTION_RESERVATION_LIFETIME = 30000L; // 10 seconds
    public static final Long REFRESH_INTENTIONS = 20000L; // 4 seconds
    public static final Long REFRESH_EXPLORATIONS = 200000L;
}
