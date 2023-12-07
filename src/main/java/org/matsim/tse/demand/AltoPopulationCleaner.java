package org.matsim.tse.demand;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;

public class AltoPopulationCleaner {

    final static double endTime = 3600 * 15; //TODO for SHK: need to define the end time for these activities whose end time is missing!

    public static void main(String[] args) {
        String inputFile = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/long-distance_Wei/LDGermany_2030_Weekday_100percent_base.xml.gz"; // Replace with your input file path
        String outputFile = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/long-distance_Wei/LDGermany_2030_Weekday_100percent_base_cleaned.xml.gz"; // Replace with your output file path

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader reader = new PopulationReader(scenario);
        reader.readFile(inputFile);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                Activity activity = PopulationUtils.getFirstActivity(plan);
                if (activity.getEndTime() == null || activity.getEndTime().isUndefined() || activity.getEndTime().seconds() < 0) {
                    activity.setEndTime(endTime);
                }

                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Leg) {
                        Leg leg = (Leg) element;
                        if (leg.getMode().equals("auto")) {
                            leg.setMode(TransportMode.car);
                        } else if (leg.getMode().equals("air")) {
                            leg.setMode(TransportMode.airplane);
                        }

                        // set the departure time same as the end time of the previous activity
                        if (leg.getDepartureTime() == null || leg.getDepartureTime().isUndefined()) {
                            leg.setDepartureTime(PopulationUtils.getPreviousActivity(plan, leg).getEndTime().seconds());
                        }
                    }
                }
            }
        }
        PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
        writer.write(outputFile);
    }
}
