package mas.statistics;

import com.github.rinde.rinsim.experiment.Experiment;
import com.google.common.collect.ImmutableSet;
import mas.experiments.ExperimentParameters;

public class StatsWriter {
    public static void writeToJson(int id, ExperimentParameters p, ImmutableSet<Experiment.SimulationResult> results) {
        System.out.print(
                "{\n" +
                "    experiment: " + id + ",\n" +
                "    parameters: " + p.toJson() + ",\n" +
                "    results: [\n"
        );

        for (final com.github.rinde.rinsim.experiment.Experiment.SimulationResult sr : results) {
            // The SimulationResult contains all information about a specific simulation,
            // the result object is the object created by the post processor, a String in this case.
            System.out.print("        " + sr.getResultObject() + ",\n");
        }

        System.out.print("    ]\n");
        System.out.print("}");
    }
}
