package mas.models;


import mas.pizza.PizzaParcel;

public enum DeliveryTaskEventType {
    /**
     * Indicates the start of a customer waiting for {@link PizzaParcel}s.
     */
    NEW_TASK,

    /**
     * Indicates that enough {@link PizzaParcel}s have been delivered for the task and that the task is thus done.
     */
    END_TASK,

    /**
     * Indicates that a new {@link mas.buildings.Pizzeria} has opened
     */
    NEW_PIZZERIA,

    /**
     * Indicates that a {@link mas.buildings.Pizzeria} has closed
     */
    CLOSE_PIZZERIA,

    /**
     * Indicates that a new {@link} has opened
     */
    NEW_ROADWORK,

    /**
     * Indicates that a {@link} has finished
     */
    FINISH_ROADWORK,


    /**
     * Indicates that a {@link mas.robot.Robot} has entered a {@link mas.buildings.ChargingStation}
     */
    ROBOT_ENTERING_CHARGINGSTATION,

    /**
     * Indicates that a {@link mas.robot.Robot} is leaving a {@link mas.buildings.ChargingStation}
     */
    ROBOT_LEAVING_CHARGINGSTATION

}
