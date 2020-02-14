package org.matsim.car;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Example on how to convert osm data from e.g. http://download.geofabrik.de into a MATSim network. This examle puts all
 * motorways and primary roads into the MATSim network. If a link is contained in the supplied shape, also minor and
 * residential raods are put into the MATsim network.
 * <p>
 * After parsing the OSM-data, unreachable areas of the network are removed by using the network cleaner
 */
public class RunCreateNetworkFromOSM {

	private static String UTM32nAsEpsg = "EPSG:31467";
	private static Path input = Paths.get("../public-svn/matsim/scenarios/countries/de/germany/original_data/osm/germanyFilter.osm");


	public static void main(String[] args) {
		new RunCreateNetworkFromOSM().create();
	}

	private void create() {

		// create an empty network which will contain the parsed network data
		Network network = NetworkUtils.createNetwork();

		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, UTM32nAsEpsg
		);

		// create an osm network reader with a filter
		OsmNetworkReader reader = new OsmNetworkReader(network, transformation, true, true);

		// the actual work is done in this call. Depending on the data size this may take a long time
		reader.parse(input.toString());

		// clean the network to remove unconnected parts where agents might get stuck
		new NetworkCleaner().run(network);

		// write out the network into a file
		new NetworkWriter(network).write("./output/network.xml.gz");
	}
}
