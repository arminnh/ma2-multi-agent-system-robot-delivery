package mas.experiments;

import mas.SimulatorSettings;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

public class ExperimentParameters {
    public long tickLength = SimulatorSettings.TICK_LENGTH;
    public long simulationLength = SimulatorSettings.SIMULATION_LENGTH;
    public int citySize = SimulatorSettings.CITY_SIZE;
    public int numRobots = SimulatorSettings.NUM_ROBOTS;
    public int robotLength = SimulatorSettings.ROBOT_LENGTH;
    public int nodeDistance = SimulatorSettings.NODE_DISTANCE;
    public Unit<Length> distanceUnit = SimulatorSettings.DISTANCE_UNIT;
    public Unit<Velocity> speedUnit = SimulatorSettings.SPEED_UNIT;
    public int simSpeedUp = SimulatorSettings.SIM_SPEEDUP;
    public boolean verbose = SimulatorSettings.VERBOSE;
    public boolean showGUI = SimulatorSettings.SHOW_GUI;
    public int repeat = SimulatorSettings.EXPERIMENT_REPEATS;
    public int threads = SimulatorSettings.EXPERIMENT_THREADS;

    public double robotSpeed = SimulatorSettings.ROBOT_SPEED;
    public int robotCapacity = SimulatorSettings.ROBOT_CAPACITY;
    public int chargingStationCapacity = SimulatorSettings.CHARGING_STATION_ROBOT_CAPACITY;
    public double batteryCapacity = SimulatorSettings.BATTERY_CAPACITY;
    public double batteryRechargeCapacity = SimulatorSettings.BATTERY_RECHARGE_CAPACITY;
    public int alternativePathsToExplore = SimulatorSettings.ALTERNATIVE_PATHS_TO_EXPLORE;
    public long timeRoadWorks = SimulatorSettings.TIME_ROAD_WORKS;
    public long batteryRescueDelay = SimulatorSettings.BATTERY_RESCUE_DELAY;
    public long intentionReservationLifetime = SimulatorSettings.INTENTION_RESERVATION_LIFETIME;
    public long explorationRefreshTime = SimulatorSettings.EXPLORATION_REFRESH_TIME;
    public long intentionRefreshTime = SimulatorSettings.INTENTION_REFRESH_TIME;
    public double probNewDeliveryTask = SimulatorSettings.PROB_NEW_DELIVERY_TASK;
    public double probNewRoadWorks = SimulatorSettings.PROB_NEW_ROAD_WORKS;
    public double pizzaAmountMean = SimulatorSettings.PIZZA_AMOUNT_MEAN;
    public double pizzaAmountStd = SimulatorSettings.PIZZA_AMOUNT_STD;

    public String toJson() {
        return "{" +
                "\"simSpeedUp\": " + simSpeedUp + ", " +
                "\"repeat\": " + repeat + ", " +
                "\"citySize\": " + citySize + ", " +
                "\"numRobots\": " + numRobots + ", " +
                "\"robotSpeed\": " + robotSpeed + ", " +
                "\"robotCapacity\": " + robotCapacity + ", " +
                "\"chargingStationCapacity\": " + chargingStationCapacity + ", " +
                "\"batteryCapacity\": " + batteryCapacity + ", " +
                "\"alternativePathsToExplore\": " + alternativePathsToExplore + ", " +
                "\"timeRoadWorks\": " + timeRoadWorks + ", " +
                "\"batteryRescueDelay\": " + batteryRescueDelay + ", " +
                "\"intentionReservationLifetime\": " + intentionReservationLifetime + ", " +
                "\"explorationRefreshTime\": " + explorationRefreshTime + ", " +
                "\"probNewDeliveryTask\": " + probNewDeliveryTask + ", " +
                "\"probNewRoadWorks\": " + probNewRoadWorks + ", " +
                "\"pizzaAmountMean\": " + pizzaAmountMean + ", " +
                "\"pizzaAmountStd\": " + pizzaAmountStd +
                "}";
    }
}
