package org.matsim.tse.demand;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

public class PopulationSampler {

    private static final Random random = new Random(0);

    public static void main(String[] args) {
        String inputPopulationFile = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/all-trips/all_trips_2030_merged_100percent.xml.gz"; // replace with your input file path
        String outputPopulationFile = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/all-trips/all_trips_2030_merged_0.1percent.xml.gz"; // replace with your output file path
        double scaleFactor = 0.001; // Set your scale factor here

        // Load the scenario with the existing population
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader reader = new PopulationReader(scenario);
        reader.readFile(inputPopulationFile);

        // Create a new population for the output
        Population outputPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
        PopulationFactory populationFactory = outputPopulation.getFactory();

        // Sample the population
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (random.nextDouble() < scaleFactor) {
                outputPopulation.addPerson(person);
            }
        }

        // Write the new, sampled population to a file
        new PopulationWriter(outputPopulation).write(outputPopulationFile);
        System.out.println("Population sampling completed. Output written to: " + outputPopulationFile);
    }
}
