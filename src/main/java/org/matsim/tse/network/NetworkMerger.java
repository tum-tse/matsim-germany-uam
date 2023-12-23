package org.matsim.tse.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.MergeNetworks;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.matsim.tse.run.ConfigureMatsim.*;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class NetworkMerger {

    private static final Logger LOGGER = Logger.getLogger(NetworkMerger.class.getName());

    public static void main(String[] args) {
        // Load road network
        Network roadNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(roadNetwork).readFile("/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/long-distance_Wei/germany_w_tertiary_w_connector.xml");

        // Clean the road network
        new NetworkCleaner().run(roadNetwork);

        // Modify the car network
        for (Link link : roadNetwork.getLinks().values()) {
            Set<String> allowedModes = new HashSet<>();
            if (link.getAllowedModes().contains(TransportMode.car)) {
                allowedModes.add(TransportMode.car);
                allowedModes.add("carPassenger");
            }
            link.setAllowedModes(allowedModes);
        }
        LOGGER.info("Finished adding carPassenger mode to the road network");

        CoordinateTransformation transformer = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:31467");
        NetworkTransform networkTransform = new NetworkTransform(transformer);
        networkTransform.run(roadNetwork);


        // Load and clean rail (pt) network
        Network originalrailNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(originalrailNetwork).readFile("/home/tumtse/Documents/haowu/MSM/matsim-germany_vsp/Germany/input/2020_Train_GTFS_network.xml.gz");

        Config config = ConfigUtils.createConfig();
        //config.network().setInputCRS("EPSG:31467");
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network trainNetwork = originalrailNetwork;
        Set<String> trainModes = new HashSet<>();
        //trainModes.add(TransportMode.train);
        trainModes.add(longDistanceTrain);
        trainModes.add(regionalTrain);
        trainModes.add(localPublicTransport);
        trainNetwork.getLinks().values().forEach(l -> l.setAllowedModes(trainModes));
        MergeNetworks.merge(scenario.getNetwork(),"", trainNetwork);
        Network railNetwork = scenario.getNetwork();

        // Clean the rail network
        //new NetworkCleaner().run(railNetwork);

        // Merge rail network into road network with renaming in case of duplicates
        railNetwork.getNodes().values().forEach(node -> {
            if (roadNetwork.getNodes().containsKey(node.getId())) {
                LOGGER.warning("Renamed duplicated node ID: " + node.getId());
                LOGGER.warning("You need to change the link's node which contains this node as well!");
                LOGGER.warning("You need to adapt the trasit scheudle and vehicle file as well!");
                System.exit(1); // Aborts the program
                String newIdString = node.getId().toString() + "_rail";
                Id<Node> newId = Id.createNodeId(newIdString);
                Node newNode = roadNetwork.getFactory().createNode(newId, node.getCoord());
                // Copy attributes from the original node
                node.getAttributes().getAsMap().forEach((attributeKey, attributeValue) -> {
                    newNode.getAttributes().putAttribute(attributeKey, attributeValue);
                });
                // add inlinks and outlinks to the new node
                node.getInLinks().values().forEach(newNode::addInLink);
                node.getOutLinks().values().forEach(newNode::addOutLink);
                roadNetwork.addNode(newNode);
            }else {
                roadNetwork.addNode(node);
            }
        });

        railNetwork.getLinks().values().forEach(link -> {
            if (roadNetwork.getLinks().containsKey(link.getId())) {
                LOGGER.warning("Renamed duplicated link ID: " + link.getId());
                LOGGER.warning("You need to adapt the trasit scheudle and vehicle file as well!");
                System.exit(1); // Aborts the program
                String newIdString = link.getId().toString() + "_rail";
                Id<Link> newId = Id.createLinkId(newIdString);
                Link newLink = roadNetwork.getFactory().createLink(newId, link.getFromNode(), link.getToNode());
                copyLinkAttributes(link, newLink);
                roadNetwork.addLink(newLink);
            }else {
                roadNetwork.addLink(link);
            }
        });

        // Write combined network to a file
        new NetworkWriter(roadNetwork).write("/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/final-version/network_with-rail.xml.gz");
    }

    private static void copyLinkAttributes(Link source, Link destination) {
        destination.setFreespeed(source.getFreespeed());
        destination.setLength(source.getLength());
        destination.setAllowedModes(source.getAllowedModes());
        destination.setNumberOfLanes(source.getNumberOfLanes());
        destination.setCapacity(source.getCapacity());
        // TODO: Add any specific attributes copying here if needed
    }
}
