package mas.experiments;

import mas.ExecutionOptions;


public class Experiments {

    public static void main(String[] args) {
        waitingTimeCustomersAndRoadTimeRobotsTest();
        waitingTimeIncreaseRobotAndOccupancyChargingStationTest();
        checkWaitingTimeOnIncreaseRoadworksTest();
    }

    private static void waitingTimeCustomersAndRoadTimeRobotsTest() {
        // What is the relation between the amount of requests that RoboPizza receives and the waiting time for customers?
        // Are robots on the road more often when there are more requests in the system?
        ExperimentParameters exp1 = ExperimentParameters.copyDefault();
        new Experiment(1, exp1).run();

        // What is the relation between the amount of requests that RoboPizza receives and the waiting time for customers?
        ExperimentParameters exp2 = ExperimentParameters.copyDefault();
        exp2.probNewDeliveryTask *= 1.5;
        new Experiment(2, exp2).run();

        ExperimentParameters exp3 = ExperimentParameters.copyDefault();
        exp3.probNewDeliveryTask *= 2;
        new Experiment(3, exp3).run();

        ExperimentParameters exp4 = ExperimentParameters.copyDefault();
        exp4.probNewDeliveryTask *= 4;
        new Experiment(4, exp4).run();
    }

    private static void waitingTimeIncreaseRobotAndOccupancyChargingStationTest() {
        //Does increasing the amount of robots decrease the customer waiting time when there are many requests?
        // How does the amount of robots impact the average workload (occupancy rate) of the charging station?
        ExperimentParameters exp1 = ExperimentParameters.copyDefault();
        exp1.probNewDeliveryTask *= 2;
        exp1.numRobots = 6;
        new Experiment(5, exp1).run();

        // What is the relation between the amount of requests that RoboPizza receives and the waiting time for customers?
        ExperimentParameters exp2 = ExperimentParameters.copyDefault();
        exp2.probNewDeliveryTask *= 2;
        exp2.numRobots = 8;
        new Experiment(6, exp2).run();

        ExperimentParameters exp3 = ExperimentParameters.copyDefault();
        exp3.probNewDeliveryTask *= 2;
        exp3.numRobots = 10;
        new Experiment(7, exp3).run();

        ExperimentParameters exp4 = ExperimentParameters.copyDefault();
        exp4.probNewDeliveryTask *= 2;
        exp4.numRobots = 20;
        new Experiment(8, exp4).run();
    }

    private static void checkWaitingTimeOnIncreaseRoadworksTest(){
        // How do waiting times change as the amount of road works changes (dynamism)?
        ExperimentParameters exp1 = ExperimentParameters.copyDefault();
        new Experiment(9, exp1).run();

        // What is the relation between the amount of requests that RoboPizza receives and the waiting time for customers?
        ExperimentParameters exp2 = ExperimentParameters.copyDefault();
        exp2.probNewRoadWorks *= 1.5;
        new Experiment(10, exp2).run();

        ExperimentParameters exp3 = ExperimentParameters.copyDefault();
        exp3.probNewRoadWorks *= 2;
        new Experiment(11, exp3).run();

        ExperimentParameters exp4 = ExperimentParameters.copyDefault();
        exp4.probNewRoadWorks *= 2;
        new Experiment(12, exp4).run();
    }
}


