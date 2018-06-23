package mas.statistics;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

public class StatisticsDTO extends com.github.rinde.rinsim.pdptw.common.StatisticsDTO {

    public final int droppedParcels;
    public final long tasksWaitingTime;
    public final long totalTasksWaitingTime;
    public final long totalIdleTime;
    public final long totalChargingTime;
    public final int tasks;
    public final int totalTasks;
    public final int totalTasksFinished;
    public final int pizzas;
    public final int totalPizzas;
    public final double avgPizzasPerRobot;
    public final int roadWorks;
    public final int totalRoadWorks;

    /**
     * Create a new statistics object.
     */
    public StatisticsDTO(
            double totalDistance,
            double totalTravelTime,
            long totalIdleTime,
            long totalChargingTime,
            int totalPickups,
            int totalDeliveries,
            int totalParcels,
            int acceptedParcels,
            int droppedParcels,
            long pickupTardiness,
            long deliveryTardiness,
            long computationTime,
            long simulationTime,
            boolean simFinish,
            int vehiclesAtDepot,
            long overTime,
            int totalVehicles,
            int movedVehicles,
            Unit<Duration> timeUnit,
            Unit<Length> distanceUnit,
            Unit<Velocity> speedUnit,
            long tasksWaitingTime,
            long totalTasksWaitingTime,
            int tasks,
            int totalTasks,
            int totalTasksFinished,
            int pizzas,
            int totalPizzas,
            double avgPizzasPerRobot,
            int roadWorks,
            int totalRoadWorks
    ) {
        super(
                totalDistance,
                totalTravelTime,
                totalPickups,
                totalDeliveries,
                totalParcels,
                acceptedParcels,
                pickupTardiness,
                deliveryTardiness,
                computationTime,
                simulationTime,
                simFinish,
                vehiclesAtDepot,
                overTime,
                totalVehicles,
                movedVehicles,
                timeUnit,
                distanceUnit,
                speedUnit
        );
        this.droppedParcels = droppedParcels;
        this.tasksWaitingTime = tasksWaitingTime;
        this.totalTasksWaitingTime = totalTasksWaitingTime;
        this.totalIdleTime = totalIdleTime;
        this.totalChargingTime = totalChargingTime;
        this.tasks = tasks;
        this.totalTasks = totalTasks;
        this.totalTasksFinished = totalTasksFinished;
        this.pizzas = pizzas;
        this.totalPizzas = totalPizzas;
        this.avgPizzasPerRobot = avgPizzasPerRobot;
        this.roadWorks = roadWorks;
        this.totalRoadWorks = totalRoadWorks;
    }

    @Override
    public String toString() {
        return "{" +
                "\"totalTasks\": " + this.totalTasks +
                ", \"totalTasksFinished\": " + this.totalTasksFinished +
                ", \"totalRoadWorks\": " + this.totalRoadWorks +
                ", \"totalTasksWaitingTime\": " + this.totalTasksWaitingTime +
                ", \"totalPizzas\": " + this.totalPizzas +
                ", \"totalDistance\": " + this.totalDistance +
                ", \"totalTravelTime\": " + this.totalTravelTime +
                ", \"totalIdleTime\": " + this.totalIdleTime +
                ", \"totalChargingTime\": " + this.totalChargingTime +
                ", \"totalPickups\": " + this.totalPickups +
                ", \"totalDeliveries\": " + this.totalDeliveries +
                ", \"totalParcels\": " + this.acceptedParcels +
                ", \"droppedParcels\": " + this.droppedParcels +
                ", \"totalVehicles\": " + this.totalVehicles +
                ", \"timeUnit\": \"" + this.timeUnit + "\"" +
                ", \"distanceUnit\": \"" + this.distanceUnit + "\"" +
                ", \"speedUnit\": \"" + this.speedUnit + "\"" +
                ", \"simulationTime\": " + this.simulationTime +
                ", \"simulationTimeInMin\": " + this.simulationTime / 1000.0 / 60.0 +
                "}";


    }


}
