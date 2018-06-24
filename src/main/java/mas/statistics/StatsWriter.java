package mas.statistics;

import com.github.rinde.rinsim.experiment.Experiment;
import com.google.common.collect.ImmutableSet;
import mas.experiments.ExperimentParameters;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

public class StatsWriter {
    public static void writeToJson(int id, ExperimentParameters p, ImmutableSet<Experiment.SimulationResult> results) {
        String filename = "experiment-statistics/experiment-" + id + ".json";
        System.out.println("Writing results to file: " + filename);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            writer.write("{\n" +
                    "    \"experiment\": " + id + ",\n" +
                    "    \"parameters\": " + p.toJson() + ",\n" +
                    "    \"results\": [\n"
            );

            final Iterator<Experiment.SimulationResult> it = results.iterator();
            while (it.hasNext()) {
                // The SimulationResult contains all information about a specific simulation,
                // the result object is the object created by the post processor, a String in this case.
                writer.write("        " + it.next().getResultObject());

                if (it.hasNext()) {
                    writer.write(",\n");
                } else {
                    writer.write("\n");
                }
            }

            writer.write("    ]\n");
            writer.write("}");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
