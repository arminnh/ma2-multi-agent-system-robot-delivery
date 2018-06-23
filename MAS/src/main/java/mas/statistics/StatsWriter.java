package mas.statistics;

import com.github.rinde.rinsim.experiment.Experiment;

public class StatsWriter {
    public static void write(Experiment.SimulationResult sr) {
        // The SimulationResult contains all information about a specific simulation,
        // the result object is the object created by the post processor, a String in this case.
        System.out.println(sr.getSimArgs().getScenario().getProblemInstanceId() + " " + sr.getResultObject());
    }
}
