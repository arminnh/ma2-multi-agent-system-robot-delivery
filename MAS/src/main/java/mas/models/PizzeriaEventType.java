package mas.models;


import mas.agents.RobotAgent;
import mas.tasks.PizzaParcel;

public enum PizzeriaEventType {
    /**
     * Indicates the start of a customer waiting for {@link PizzaParcel}s.
     */
    NEW_TASK,

    /**
     * Indicates that enough {@link PizzaParcel}s have been delivered for the deliveryTask and that the deliveryTask is thus done.
     */
    END_TASK,

    /**
     * Indicates that a new {@link} has opened
     */
    STARTED_ROADWORKS,

    /**
     * Indicates that a {@link} has finished
     */
    FINISHED_ROADWORKS,

    /**
     * Indicates that a {@link RobotAgent} has entered a {@link mas.buildings.ChargingStation}
     */
    ROBOT_AT_CHARGING_STATION,

    /**
     * Indicates that a {@link RobotAgent} is leaving a {@link mas.buildings.ChargingStation}
     */
    ROBOT_LEAVING_CHARGING_STATION,

    /**
     * Indicates that a {@link RobotAgent} is dropping a {@link mas.tasks.PizzaParcel}
     */
    DROP_PARCEL

}
