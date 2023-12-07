package org.matsim.tse.demand;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.population.PopulationUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class PopulationMerger {

    public static void main(String[] args) {
        String folderPath = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/all-trips/prepare-for-merge"; // Replace with your folder path
        String mergedPopulationPath = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/all-trips/plan_all-trips_2030_100percent.xml.gz"; // Path for the merged file

        Config config = ConfigUtils.createConfig();
        Population mergedPopulation = ScenarioUtils.createScenario(config).getPopulation();

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xml.gz"))
                    .forEach(path -> {
                        // Create a new scenario for each file to avoid accumulating persons in a single scenario
                        Scenario tempScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
                        PopulationReader reader = new PopulationReader(tempScenario);
                        reader.readFile(path.toString());

                        String fileIdentifier = path.getFileName().toString();
                        fileIdentifier = fileIdentifier.substring(0, fileIdentifier.indexOf(".xml.gz"));
                        System.out.println(fileIdentifier);
                        String finalFileIdentifier = fileIdentifier;

                        tempScenario.getPopulation().getPersons().values().forEach(person -> {
                            Id<Person> personId = person.getId();

                            if (mergedPopulation.getPersons().containsKey(personId)) {
                                Person existingPerson = mergedPopulation.getPersons().get(personId);

                                if (!arePlansIdentical(person, existingPerson)) {
                                    // Assign a new ID and add to merged population if plans are different
                                    String newId = finalFileIdentifier + "_" + personId.toString();
                                    Person newPerson = mergedPopulation.getFactory().createPerson(Id.create(newId, Person.class));
                                    copyPersonData(person, newPerson);
                                    mergedPopulation.addPerson(newPerson);
                                }
                            } else {
                                // Add person to merged population if not already present
                                Person newPerson = mergedPopulation.getFactory().createPerson(personId);
                                copyPersonData(person, newPerson);
                                mergedPopulation.addPerson(newPerson);
                            }
                        });
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        new PopulationWriter(mergedPopulation).write(mergedPopulationPath);
    }

    private static void copyPersonData(Person source, Person destination) {
        // Copy attributes
        source.getAttributes().getAsMap().forEach((key, value) -> destination.getAttributes().putAttribute(key, value));

        // Copy plans
        source.getPlans().forEach(destination::addPlan);
    }

    private static boolean arePlansIdentical(Person person1, Person person2) {
        // Check if the number of plans are the same
        if (person1.getPlans().size() != person2.getPlans().size()) {
            return false;
        }

        // Compare each plan in detail
        for (int i = 0; i < person1.getPlans().size(); i++) {
            Plan plan1 = person1.getPlans().get(i);
            Plan plan2 = person2.getPlans().get(i);

            // Check if both plans have the same number of plan elements
            if (plan1.getPlanElements().size() != plan2.getPlanElements().size()) {
                return false;
            }

            // Iterate over all plan elements and compare them
            for (int j = 0; j < plan1.getPlanElements().size(); j++) {
                PlanElement pe1 = plan1.getPlanElements().get(j);
                PlanElement pe2 = plan2.getPlanElements().get(j);

                if (pe1 instanceof Activity && pe2 instanceof Activity) {
                    Activity a1 = (Activity) pe1;
                    Activity a2 = (Activity) pe2;

                    // Compare activity details
                    if (!a1.getType().equals(a2.getType()) ||
                            a1.getEndTime() != a2.getEndTime() ||
                            !a1.getCoord().equals(a2.getCoord())) {
                        return false;
                    }
                } else if (pe1 instanceof Leg && pe2 instanceof Leg) {
                    Leg l1 = (Leg) pe1;
                    Leg l2 = (Leg) pe2;

                    // Compare leg details, for example the mode of transportation and departure time
                    if (!l1.getMode().equals(l2.getMode())) {
                        return false;
                    }
                    if (!(l1.getDepartureTime().seconds()==l2.getDepartureTime().seconds())) {
                        return false;
                    }
                    // Additional comparisons can be made here based on your specific requirements
                } else {
                    // If the types of plan elements do not match, the plans are not identical
                    return false;
                }
            }
        }

        return true;
    }
}
