package mas;

public class SimulatorSettings {
    public static final long TICK_LENGTH = 100L;
    public static final int SIM_SPEEDUP = 1;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;

<<<<<<< HEAD
    public static final int CITY_SIZE = 5;
    public static final int NUM_ROBOTS = 1;
=======
    public static final int CITY_SIZE = 10;
    public static final int NUM_ROBOTS = 10;
>>>>>>> 9b6573ff9bd63ee91101c51ee8e70395e5447988
    public static final int ROBOT_CAPACITY = 5;
    public static final double BATTERY_CAPACITY = 2000*CITY_SIZE*CITY_SIZE;
    public static final double BATTERY_RESCUE_CAPACITY = BATTERY_CAPACITY*0.3;
    public static final long BATTERY_RESCUE_DELAY = 30000L;
    public static final int VEHICLE_LENGTH = 1;
    public static final double VEHICLE_SPEED_KMH = 10;
    public static final int CHARGING_STATION_CAPACITY = 3;

    public static final double PROB_NEW_PARCEL = .05;
    public static final double PROB_NEW_ROAD_WORKS = 0.04;
    public static final long TIME_ROAD_WORKS = 15000;
    public static final double PIZZA_AMOUNT_STD = 0.75;
    public static final double PIZZA_AMOUNT_MEAN = 4;
    public static final int ALTERNATIVE_PATHS_TO_EXPLORE = 2;

    public static final long INTENTION_RESERVATION_LIFETIME = 30000L; // 10 seconds
    public static final Long REFRESH_INTENTIONS = 20000L; // 4 seconds
    public static final Long REFRESH_EXPLORATIONS = 200000L;
}
