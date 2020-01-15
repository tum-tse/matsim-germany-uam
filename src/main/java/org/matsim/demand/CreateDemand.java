/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package org.matsim.demand;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;



import org.locationtech.jts.geom.*;

/**
* @author smueller
*/

public class CreateDemand {

	private static final Logger log = Logger.getLogger(CreateDemand.class);
	
	private final static Map<String, Geometry> regions = ShapeFileReader.getAllFeatures("../shared-svn/studies/countries/de/prognose_2030/Shape/NUTS3/NUTS3_2010_DE.shp").stream()
			.collect(Collectors.toMap(feature -> (String) feature.getAttribute("NUTS_ID"), feature -> (Geometry) feature.getDefaultGeometry()));
	
	private static EnumeratedDistribution<Geometry> landcover;
	
	private static Population population; 
	
	private static final GeometryFactory geometryFactory = new GeometryFactory();
	
	private static final Random random = new Random(100);
//	private static final Random random = MatsimRandom.getLocalInstance();
	
	
	public static Population create(String outputPopulationFile, double sample, boolean train, boolean car, boolean airplane, boolean pt, boolean bike, boolean walk) {
		
		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		
		// Read in landcover data to make people stay in populated areas
		// we are using a weighted distribution by area-size, so that small areas receive less inhabitants than more
		// populated ones.
		List<Pair<Geometry, Double>> weightedGeometries = new ArrayList<>();
		for (SimpleFeature feature : ShapeFileReader.getAllFeatures("../shared-svn/studies/countries/de/prognose_2030/Shape/Landschaftsmodell/sie01_f.shp")) {
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			weightedGeometries.add(new Pair<>(geometry, geometry.getArea()));
		}
		
		landcover = new EnumeratedDistribution<>(weightedGeometries);
		File demandInput = new File("../shared-svn/studies/countries/de/prognose_2030/PVMatrix_BVWP15_A2010/SM_PVMatrix_BVWP15_A2010.csv");
		// read the bvwp csv file
		try (CSVParser parser = CSVParser.parse(demandInput, StandardCharsets.UTF_8, CSVFormat.newFormat(';').withFirstRecordAsHeader())) {

			// this will iterate over every line in the csv except the first one which contains the column headers
			for (CSVRecord record : parser) {				
				
				String originZone = record.get("Quelle_Nuts3");
				String destinationZone = record.get("Ziel_Nuts3");
				List<String> csvColumns = new ArrayList<>();
				
				if (train) {
					csvColumns.add("Bahn_Fz1");
					csvColumns.add("Bahn_Fz2");
					csvColumns.add("Bahn_Fz3");
					csvColumns.add("Bahn_Fz4");
					csvColumns.add("Bahn_Fz5");
					csvColumns.add("Bahn_Fz6");
				}
			
				if (car) {
					csvColumns.add("MIV_Fz1");
					csvColumns.add("MIV_Fz2");
					csvColumns.add("MIV_Fz3");
					csvColumns.add("MIV_Fz4");
					csvColumns.add("MIV_Fz5");
					csvColumns.add("MIV_Fz6");
				}
				
				if (airplane) {
					csvColumns.add("Luft_Fz1");
					csvColumns.add("Luft_Fz2");
					csvColumns.add("Luft_Fz3");
					csvColumns.add("Luft_Fz4");
					csvColumns.add("Luft_Fz5");
					csvColumns.add("Luft_Fz6");
				}

				if(pt) {
					csvColumns.add("OESPV_Fz1");
					csvColumns.add("OESPV_Fz2");
					csvColumns.add("OESPV_Fz3");
					csvColumns.add("OESPV_Fz4");
					csvColumns.add("OESPV_Fz5");
					csvColumns.add("OESPV_Fz6");
				}

				
				if (bike) {
					csvColumns.add("Rad_Fz1");
					csvColumns.add("Rad_Fz2");
					csvColumns.add("Rad_Fz3");
					csvColumns.add("Rad_Fz4");
					csvColumns.add("Rad_Fz5");
					csvColumns.add("Rad_Fz6");
				}
				
				if (walk) {
					csvColumns.add("Fuss_Fz1");
					csvColumns.add("Fuss_Fz2");
					csvColumns.add("Fuss_Fz3");
					csvColumns.add("Fuss_Fz4");
					csvColumns.add("Fuss_Fz5");
					csvColumns.add("Fuss_Fz6");
				}
				
				for (int ii = 0; ii < csvColumns.size(); ii++) {
					String mode = null;
					String nextActType = null;
					
					double noOfAgentsDouble = Integer.parseInt((record.get(csvColumns.get(ii)))) / 365.25;
					int noOfAgents = (int) noOfAgentsDouble;
					
					double rest = noOfAgentsDouble - noOfAgents;
					
					if (rest > random.nextDouble()) {
						noOfAgents++;
					}
					
					String[] splitColumn = csvColumns.get(ii).split("_");
					
					switch(splitColumn[0]) {
					case "Bahn":
//						mode = TransportMode.train;
						mode = "longDistancePt";
						break;
					case "MIV":
						mode = TransportMode.car;
						break;
					case "Luft":
//						mode = TransportMode.airplane;
						mode = "longDistancePt";
						break;
					case "OESPV":
						mode = TransportMode.pt;
						break;
					case "Rad":
						mode = TransportMode.bike;
						break;
					case "Fuss":
						mode = TransportMode.walk;
						break;

					}
					
					switch(splitColumn[1]) {
					case "Fz1":
						nextActType = "work";
						break;
					case "Fz2":
						nextActType = "education";
						break;
					case "Fz3":
						nextActType = "shop";
						break;
					case "Fz4":
						nextActType = "business";
						break;
					case "Fz5":
						nextActType = "holiday";
						break;
					case "Fz6":
						nextActType = "other";
						break;

					}
					
					if (!originZone.equals(destinationZone)) {
//					agents travelling from Berlin to Munich
//					if (originZone.equals("DE300") && destinationZone.equals("DE212") ) {
						createPersons(sample, originZone, destinationZone, noOfAgents, mode, nextActType);
					}
					
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		PopulationWriter populationWriter = new PopulationWriter(population);
		populationWriter.write(outputPopulationFile);
		return population;
		
	}


	private static void createPersons(double sample, String originZone, String destinationZone, int noOfAgents, String mode, String nextActType) {
	

		
//			log.warn("Creating persons: " + oGeometry + " --- " + dGeometry + " --- " + noOfAgents + " --- " + mode + " --- " + nextActType);
		if (regions.containsKey(originZone) && regions.containsKey(destinationZone)) {
			for (int ii = 0; ii < noOfAgents; ii++) {
//				sample size
				if (random.nextDouble() < sample)
				createPerson(originZone, destinationZone, mode, nextActType);
			}
		}
		
//		else {
//			if (noOfAgents > 0) {
//				log.warn("Cannot create " + noOfAgents + " persons for relation: " + originZone + "---" + destinationZone);

//			}
			
//		}
		
	}


	
	private static void createPerson(String oGeometry, String dGeometry, String mode, String nextActType) {
		
		PopulationFactory populationFactory = population.getFactory();
		int index = population.getPersons().size();
		Id<Person> id = Id.createPersonId(oGeometry + "---" + dGeometry + "---" + mode + "---" + index);
		Person person = populationFactory.createPerson(id);
		population.addPerson(person);
		
		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		
		Coord originCoord = getCoordInGeometry(oGeometry);
		Activity originAct = populationFactory.createActivityFromCoord("origin", originCoord);
//		Todo: Tagesgang
		int tripStartTime = createTripStartTime(nextActType);
		originAct.setEndTime(tripStartTime);
		plan.addActivity(originAct);
		
		Leg leg = populationFactory.createLeg(mode);
		plan.addLeg(leg);
		
		Coord destinationCoord = getCoordInGeometry(dGeometry);
		Activity destinationAct = populationFactory.createActivityFromCoord(nextActType, destinationCoord);
//		ToDo: Tagesgang
//		destinationAct.setStartTime(12. * 3600);
		plan.addActivity(destinationAct);
		
		
	}



	private static int createTripStartTime(String nextActType) {
//		trip start times are set dependent on activity type
//		source for values is: MID 2017 Tabelle A W7 Startzeit
//		MID doesn't contain values for holiday, so the general values for all trips are used
//		other is interpreted as "Freizeit"
		int tripStartTime = -1;
		double localRandom = random.nextDouble();
		
		switch(nextActType) {
		case "work":
			if(localRandom < 0.04) {
				tripStartTime = 0 + (int)Math.round(-0.5 + 5 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.04 && localRandom < 0.35 ) {
				tripStartTime = 5 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.35 && localRandom < 0.45 ) {
				tripStartTime = 8 * 3600 + (int)Math.round(-0.5 + 2 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.45 && localRandom < 0.54 ) {
				tripStartTime = 10 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.54 && localRandom < 0.72 ) {
				tripStartTime = 13 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.72 && localRandom < 0.94 ) {
				tripStartTime = 16 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.94 ) {
				tripStartTime = 19 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			break;
			
		case "education":
			if(localRandom < 0.0 ) {
				tripStartTime = 0 + (int)Math.round(-0.5 + 5 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.0 && localRandom < 0.36 ) {
				tripStartTime = 5 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.36 && localRandom < 0.47 ) {
				tripStartTime = 8 * 3600 + (int)Math.round(-0.5 + 2 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.47 && localRandom < 0.58 ) {
				tripStartTime = 10 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.58 && localRandom < 0.85 ) {
				tripStartTime = 13 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.85 && localRandom < 0.97 ) {
				tripStartTime = 16 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.97 ) {
				tripStartTime = 19 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			break;
			
		case "shop":
			if(localRandom < 0.0 ) {
				tripStartTime = 0 + (int)Math.round(-0.5 + 5 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.0 && localRandom < 0.03 ) {
				tripStartTime = 5 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.03 && localRandom < 0.21 ) {
				tripStartTime = 8 * 3600 + (int)Math.round(-0.5 + 2 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.21 && localRandom < 0.53 ) {
				tripStartTime = 10 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.53 && localRandom < 0.74 ) {
				tripStartTime = 13 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.74 && localRandom < 0.95 ) {
				tripStartTime = 16 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.95 ) {
				tripStartTime = 19 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			break;
				
		case "business":
			if(localRandom < 0.02 ) {
				tripStartTime = 0 + (int)Math.round(-0.5 + 5 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.02 && localRandom < 0.15 ) {
				tripStartTime = 5 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.15 && localRandom < 0.33 ) {
				tripStartTime = 8 * 3600 + (int)Math.round(-0.5 + 2 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.33 && localRandom < 0.58 ) {
				tripStartTime = 10 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.58 && localRandom < 0.8 ) {
				tripStartTime = 13 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.8 && localRandom < 0.95 ) {
				tripStartTime = 16 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.95 ) {
				tripStartTime = 19 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			break;
			
		case "holiday":
			if(localRandom < 0.03 ) {
				tripStartTime = 0 + (int)Math.round(-0.5 + 5 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.03 && localRandom < 0.14 ) {
				tripStartTime = 5 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.14 && localRandom < 0.26 ) {
				tripStartTime = 8 * 3600 + (int)Math.round(-0.5 + 2 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.26 && localRandom < 0.46 ) {
				tripStartTime = 10 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.46 && localRandom < 0.69 ) {
				tripStartTime = 13 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.69 && localRandom < 0.92 ) {
				tripStartTime = 16 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.92 ) {
				tripStartTime = 19 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			break;
			
		case "other":
			if(localRandom < 0.05 ) {
				tripStartTime = 0 + (int)Math.round(-0.5 + 5 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.05 && localRandom < 0.08 ) {
				tripStartTime = 5 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.08 && localRandom < 0.16 ) {
				tripStartTime = 8 * 3600 + (int)Math.round(-0.5 + 2 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.16 && localRandom < 0.34 ) {
				tripStartTime = 10 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.34 && localRandom < 0.58 ) {
				tripStartTime = 13 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.58 && localRandom < 0.86 ) {
				tripStartTime = 16 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			if(localRandom >= 0.86 ) {
				tripStartTime = 19 * 3600 + (int)Math.round(-0.5 + 3 * 3600 * random.nextDouble());
			}
			break;
		
		}
		
		
		return tripStartTime;
	}


	private static Coord getCoordInGeometry(String geometryKey) {

		double x, y;
		Geometry region = regions.get(geometryKey);
		Point point;
		Geometry selectedLandcover;
		Point centroid;
		int counter = 0;
		// select a landcover feature and test whether it's centroid is in the right region. If not select a another one.
		do {
			selectedLandcover = landcover.sample();
			centroid = selectedLandcover.getCentroid();
			counter++;
//		} while (!region.contains(selectedLandcover) && counter < 100000);
		} while (!region.contains(centroid) && counter < 100000);
		
		if (counter < 100000) {
		// if the landcover feature is in the correct region generate a random coordinate within the bounding box of the
		// landcover feature. Repeat until a coordinate is found which is actually within the landcover feature.
		
			do {
				Envelope envelope = selectedLandcover.getEnvelopeInternal();

				x = envelope.getMinX() + envelope.getWidth() * random.nextDouble();
				y = envelope.getMinY() + envelope.getHeight() * random.nextDouble();
				point = geometryFactory.createPoint(new Coordinate(x, y));
			} while (point == null || !selectedLandcover.contains(point));
			return new Coord(x, y); }
		
		else {
			log.warn("No coord found in landcover, used region centroid. Region: " + geometryKey) ;
			return new Coord(region.getCentroid().getX(), region.getCentroid().getY() );
		}
	}

}
