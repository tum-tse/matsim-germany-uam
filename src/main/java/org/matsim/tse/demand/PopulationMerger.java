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
        String folderPath = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/long-distance_Wei/LDGermany_2030_Weekday_100percent_base/"; // Replace with your folder path
        String mergedPopulationPath = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/long-distance_Wei/LDGermany_2030_Weekday_100percent_base_merged.xml.gz"; // Path for the merged file

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

            // Compare details of each plan (you may need to extend this comparison based on your plan structure)
            if (!plan1.equals(plan2)) {
                return false;
            }
        }

        return true;
    }
}

