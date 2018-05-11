package mas.statistics;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

public class StatisticsDTO extends com.github.rinde.rinsim.pdptw.common.StatisticsDTO {

    public long tasksWaitingTime;
    public long totalTasksWaitingTime;
    public int vehiclesIdle;
    public long totalIdleTime;
    public long totalChargingTime;
    public int tasks;
    public int totalTasks;
    public int totalTasksFinished;
    public int pizzas;
    public int totalPizzas;
    public double avgPizzasPerRobot;
    public int pizzerias;
    public int roadWorks;
    public int totalRoadWorks;

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
            int vehiclesIdle,
            int tasks,
            int totalTasks,
            int totalTasksFinished,
            int pizzas,
            int totalPizzas,
            double avgPizzasPerRobot,
            int pizzerias,
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
        this.tasksWaitingTime = tasksWaitingTime;
        this.totalTasksWaitingTime = totalTasksWaitingTime;
        this.vehiclesIdle = vehiclesIdle;
        this.totalIdleTime = totalIdleTime;
        this.totalChargingTime = totalChargingTime;
        this.tasks = tasks;
        this.totalTasks = totalTasks;
        this.totalTasksFinished = totalTasksFinished;
        this.pizzas = pizzas;
        this.totalPizzas = totalPizzas;
        this.avgPizzasPerRobot = avgPizzasPerRobot;
        this.pizzerias = pizzerias;
        this.roadWorks = roadWorks;
        this.totalRoadWorks = totalRoadWorks;
    }
}
