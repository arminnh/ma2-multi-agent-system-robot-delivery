package mas;

public class SimulatorSettings {
    public static final long TICK_LENGTH = 100L;
    public static final int SIM_SPEEDUP = 10;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;

    public static final int CITY_SIZE = 15;
    public static final int NUM_ROBOTS = 5;
    public static final int ROBOT_CAPACITY = 5;
    public static final double BATTERY_CAPACITY = 2*CITY_SIZE*CITY_SIZE;
    public static final int VEHICLE_LENGTH = 1;
    public static final double VEHICLE_SPEED_KMH = 10;
    public static final int CHARGING_STATION_CAPACITY = 1;

    public static final double PROB_NEW_PARCEL = .02;
    public static final double PROB_NEW_ROAD_WORKS = 0.04;
    public static final long TIME_ROAD_WORKS = 15000;
    public static final double PIZZA_AMOUNT_STD = 0.75;
    public static final double PIZZA_AMOUNT_MEAN = 4;
    public static final int ALTERNATIVE_PATHS_TO_EXPLORE = 2;

    public static final long INTENTION_RESERVATION_LIFETIME = 30000L; // 10 seconds
    public static final Long REFRESH_INTENTIONS = 20000L; // 4 seconds
    public static final Long REFRESH_EXPLORATIONS = 200000L;
    public static final long RESCUE_DELAY = 30000L;
    public static final int RESCUE_CAPACITY = 30;
}
