package mas.statistics;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

public class StatisticsDTO extends com.github.rinde.rinsim.pdptw.common.StatisticsDTO {

    public long tasksWaitingTime;
    public long totalTasksWaitingTime;
    public int robotsIdle;
    public long totalRobotsTimeDriving;
    public long totalRobotsTimeIdle;
    public long totalRobotsTimeCharging;
    public int tasks;
    public int totalTasks;
    public int totalTasksFinished;
    public int pizzas;
    public int totalPizzas;
    public double avgPizzasPerRobot;
    public int pizzerias;
    public int roadWorks;

    /**
     * Create a new statistics object.
     *
     * @param dist     {@link #totalDistance}.
     * @param tt       {@link #totalTravelTime}.
     * @param pick     {@link #totalPickups}.
     * @param del      {@link #totalDeliveries}.
     * @param parc     {@link #totalParcels}.
     * @param accP     {@link #acceptedParcels}.
     * @param pickTar  {@link #pickupTardiness}.
     * @param delTar   {@link #deliveryTardiness}.
     * @param compT    {@link #computationTime}.
     * @param simT     {@link #simulationTime}.
     * @param finish   {@link #simFinish}.
     * @param atDepot  {@link #vehiclesAtDepot}.
     * @param overT    {@link #overTime}.
     * @param total    {@link #totalVehicles}.
     * @param moved    {@link #movedVehicles}.
     * @param time     {@link #timeUnit}.
     * @param distUnit {@link #distanceUnit}.
     * @param speed    {@link #speedUnit}.
     */
    public StatisticsDTO(
            double dist, double tt, int pick, int del, int parc, int accP, long pickTar, long delTar, long compT,
            long simT, boolean finish, int atDepot, long overT, int total, int moved, Unit<Duration> time,
            Unit<Length> distUnit, Unit<Velocity> speed, long twt, long ttwt, int ri, long trtd, long trti, long trtc,
            int tasks, int ttasks, int ttf, int pizzas, int totalPizzas, double appr,  int p, int rw
    ) {
        super(dist, tt, pick, del, parc, accP, pickTar, delTar, compT, simT, finish, atDepot, overT, total, moved, time,
                distUnit, speed);

        this.tasksWaitingTime = twt;
        this.totalTasksWaitingTime = ttwt;
        this.robotsIdle = ri;
        this.totalRobotsTimeDriving = trtd;
        this.totalRobotsTimeIdle = trti;
        this.totalRobotsTimeCharging = trtc;
        this.tasks = tasks;
        this.totalTasks = ttasks;
        this.totalTasksFinished = ttf;
        this.pizzas = pizzas;
        this.totalPizzas = totalPizzas;
        this.avgPizzasPerRobot = appr;
        this.pizzerias = p;
        this.roadWorks = rw;
    }
}
