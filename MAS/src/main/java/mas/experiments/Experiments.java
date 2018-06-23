package mas.experiments;

import com.google.devtools.common.options.OptionsParser;
import mas.ExecutionOptions;

import java.util.Collections;

public class Experiments {

    private static ExperimentParameters getExperimentParameters(ExecutionOptions options) {
        ExperimentParameters params = new ExperimentParameters();

        params.alternativePathsToExplore = options.alternativePaths;
        params.chargingStationCapacity = options.chargingStationCapacity;
        params.citySize = options.citySize;
        params.repeat = options.repeat;
        params.numRobots = options.numRobots;
        params.probNewDeliveryTask = options.probNewDeliveryTask;
        params.probNewRoadWorks = options.probNewRoadWorks;
        params.showGUI = options.showGUI;
        params.simSpeedUp = options.simSpeedUp;

        return params;
    }

    private static void printUsage(OptionsParser parser) {
        System.out.println("Robot Pizza Delivery MAS experiments Usage: java -jar experiments.jar OPTIONS");
        System.out.println("Experiment iterations last for 2 simulation hours.");
        System.out.println(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }

    public static void main(String[] args) {
        OptionsParser parser = OptionsParser.newOptionsParser(ExecutionOptions.class);
        parser.parseAndExitUponError(args);

        ExecutionOptions options = parser.getOptions(ExecutionOptions.class);
        assert options != null;
        if (options.help) {
            printUsage(parser);
            return;
        }
        options.showGUI = false;
        options.probNewRoadWorks = 0;

        ExperimentParameters params = getExperimentParameters(options);
        Experiment experiment = new Experiment(1, params);
        experiment.run();
    }
}


