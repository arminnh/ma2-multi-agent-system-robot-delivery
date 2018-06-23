package mas;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

public class ExecutionOptions extends OptionsBase {
    @Option(
            name = "numRobots",
            abbrev = 'n',
            help = "The amount of robots to use.",
            category = "Startup",
            defaultValue = "3"
    )
    public int numRobots;

    @Option(
            name = "citySize",
            abbrev = 's',
            help = "The size of the city.",
            category = "Startup",
            defaultValue = "12"
    )
    public int citySize;

    @Option(
            name = "chargingStationCapacity",
            abbrev = 'c',
            help = "The capacity of the charging station. (The amount of robots that can charge at once)",
            category = "Startup",
            defaultValue = "3"
    )
    public int chargingStationCapacity;

    @Option(
            name = "probTask",
            abbrev = 't',
            help = "The probability for a new task at each tick.",
            category = "Startup",

            defaultValue = "0.018"
    )
    public double probNewDeliveryTask;

    @Option(
            name = "probRoadWork",
            abbrev = 'w',
            help = "The probability for a new roadwork at each tick.",
            category = "Startup",
            defaultValue = "0.03"
    )
    public double probNewRoadWorks;

    @Option(
            name = "alterativePaths",
            abbrev = 'p',
            help = "The amount of paths the robot explores when searching for a new path.",
            category = "Startup",

            defaultValue = "3"
    )
    public int alternativePaths;

    @Option(
            name = "repeat",
            abbrev = 'r',
            help = "How many times to repeat a experiment (only for experiments).",
            category = "Startup",

            defaultValue = "2"
    )
    public int repeat;

    @Option(
            name = "help",
            abbrev = 'h',
            help = "Prints usage info.",
            defaultValue = "false",
            category = "Startup"
    )
    public boolean help;

    @Option(
            name = "simSpeedUp",
            abbrev = 'u',
            help = "Speeds up the simulation time between two GUI draw operations.",
            defaultValue = "29",
            category = "Startup"
    )
    public int simSpeedUp;

    @Option(
            name = "verbose",
            abbrev = 'v',
            help = "Whether or not to print more output.",
            defaultValue = "false",
            category = "Startup"
    )
    public boolean verbose;

    @Option(
            name = "showGUI",
            abbrev = 'g',
            help = "Whether or not to show the GUI.",
            defaultValue = "true",
            category = "Startup"
    )
    public boolean showGUI;

}
