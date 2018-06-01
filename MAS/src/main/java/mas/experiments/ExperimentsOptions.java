package mas.experiments;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

public class ExperimentsOptions  extends OptionsBase {
    // numRobots
    // citySize
    // chargingStationCapacity
    // probNewDeliveryTask
    // probNewRoadWorks
    // alternativePathsToExplore

    @Option(
            name = "numRobots",
            abbrev = 'n',
            help = "The amount of robots used",
            category="Startup",

            defaultValue = "3"
    )
    public int numRobots;

    @Option(
            name = "citySize",
            abbrev = 's',
            help = "The size of the city",
            category="Startup",

            defaultValue = "12"
    )
    public int citySize;

    @Option(
            name = "chargingStationCapacity",
            abbrev = 'c',
            help = "The capacity of the charging station. (How many robots can be there at one given point in time)",
            category="Startup",

            defaultValue = "3"
    )
    public int chargingStationCapacity;


    @Option(
            name = "probTask",
            abbrev = 't',
            help = "The probability for a new task each tick.",
            category="Startup",

            defaultValue = "0.03"
    )
    public double probNewDeliveryTask;

    @Option(
            name = "probRoadWork",
            abbrev = 'w',
            help = "The probability for a new roadwork each tick.",
            category="Startup",
            defaultValue = "0.03"
    )
    public double probNewRoadWorks;

    @Option(
            name = "alterativePaths",
            abbrev = 'p',
            help = "The amount of paths the robot check before making a decision.",
            category="Startup",

            defaultValue = "3"
    )
    public int alternativePaths;

    @Option(
            name = "repeat",
            abbrev = 'r',
            help = "How many times to repeat the experiment.",
            category="Startup",

            defaultValue = "1"
    )
    public int repeat;

    @Option(
            name = "help",
            abbrev = 'h',
            help = "Prints usage info.",
            defaultValue = "false",
            category="Startup"
    )
    public boolean help;
}
