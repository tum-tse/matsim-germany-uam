package org.matsim.tse.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;

import java.util.logging.Logger;

public class NetworkMerger {

    private static final Logger LOGGER = Logger.getLogger(NetworkMerger.class.getName());

    public static void main(String[] args) {
        // Load road network
        Network roadNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(roadNetwork).readFile("/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/long-distance_Wei/germany_w_tertiary_w_connector.xml");

        // Clean the road network
        new NetworkCleaner().run(roadNetwork);

        // Load and clean rail (pt) network
        Network railNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(railNetwork).readFile("/home/tumtse/Documents/haowu/MSM/matsim-germany_vsp/Germany/input/2020_Train_GTFS_network_cleaned.xml.gz");

        // Clean the rail network
        //new NetworkCleaner().run(railNetwork); //TODO: Check if this is necessary

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
                //TODO: if adding Inlinks and outlinks is necessary
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
