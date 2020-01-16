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


package org.matsim.train;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
* @author smueller
*/

public class RunGTFS2MATSim {
	
	private static final Logger log = Logger.getLogger(RunGTFS2MATSim.class);
	private static final String svnDir = "../";
	private static final String DBGTFSFile = svnDir + "shared-svn/studies/countries/de/train/db-fv-gtfs-master/2019.zip";
	private static final String VRRGTFSFile = svnDir + "shared-svn/projects/nemo_mercator/data/pt/vrr_gtfs_sep19.zip";
	private static final String outputDir = svnDir + "shared-svn/studies/countries/de/train/db-fv-gtfs-master/MATSimFiles/2016/";
	
//	private static final List<Geometry> regions = ShapeFileReader.getAllFeatures("../shared-svn/projects/nemo_mercator/data/matsim_input/baseCase/ruhrgebiet_boundary.shp").stream() 
//			.map(feature -> (Geometry)feature.getDefaultGeometry())
//			.collect(Collectors.toList());
	private final static Map<String, Geometry> regions = ShapeFileReader.getAllFeatures(svnDir + "shared-svn/projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Bundeslaender/vg2500_geo84/vg2500_bld.shp").stream()
			.collect(Collectors.toMap(feature -> (String) feature.getAttribute("GEN"), feature -> (Geometry) feature.getDefaultGeometry()));
	
	private static final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:31467", TransformationFactory.WGS84);
	
	public static void main(String[] args) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario DBScenario = new CreatePtScheduleAndVehiclesFromGtfs().run(DBGTFSFile, "2019-11-23", "DB_");
		Scenario VRRScenario = new CreatePtScheduleAndVehiclesFromGtfs().run(VRRGTFSFile, "2019-11-23", "VRR_");
		
		mergeSchedules("DB_", scenario.getTransitSchedule().getFactory(), scenario.getTransitSchedule(), DBScenario.getTransitSchedule());
		mergeVehicles("DB_", scenario.getTransitVehicles().getFactory(), scenario.getTransitVehicles(), DBScenario.getTransitVehicles());
		
		mergeSchedules("VRR_", scenario.getTransitSchedule().getFactory(), scenario.getTransitSchedule(), VRRScenario.getTransitSchedule());
		mergeVehicles("VRR_", scenario.getTransitVehicles().getFactory(), scenario.getTransitVehicles(), VRRScenario.getTransitVehicles());
	
		new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "").createNetwork();
		
//		sets link speeds to the maximum speed of all train trips that travel on this link
//		this should insure, that no trips are late, some may, however, be early
		setLinkSpeedsToMax(scenario);
		
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(svnDir + "shared-svn/projects/nemo_mercator/data/pt/DB_VRR_GTFS_merged/DB_VRR_GTFS_transitVehicles.xml.gz");
		new TransitScheduleWriterV2(scenario.getTransitSchedule()).write(svnDir + "shared-svn/projects/nemo_mercator/data/pt/DB_VRR_GTFS_merged/DB_VRR_GTFS_transitSchedule.xml.gz");
		new NetworkWriter(scenario.getNetwork()).write(svnDir + "shared-svn/projects/nemo_mercator/data/pt/DB_VRR_GTFS_merged/DB_VRR_GTFS_network.xml.gz");
		
//		sets link speeds to an average speed of all train trips that travel on this link
//		setLinkSpeedsToAverage(scenario);
		
		runScenario(scenario);

	}

//	private static Scenario createScenario(String gtfsZipFile, String date, String networkPrefix) {
//		Scenario scenario = new CreatePtScheduleAndVehiclesFromGtfs().run(gtfsZipFile, date, networkPrefix);
//		return scenario;
//	}
	
	private static void runScenario(Scenario scenario) {
		
		Config config = scenario.getConfig();
		
		config.controler().setOutputDirectory(outputDir+"/Run");
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.global().setNumberOfThreads(8);
		
		config.transit().setUseTransit(true);
		
//		set to a a low value to increase performance
		config.transitRouter().setMaxBeelineWalkConnectionDistance(1);
			
		Controler controler = new Controler(scenario);

		controler.run();

	}
	
	private static void setLinkSpeedsToAverage(Scenario scenario) {
		Map<Id<Link>, Double> linkSpeedSums = new HashMap<>();
		Map<Id<Link>, Double> linkSpeedNumbers = new HashMap<>();
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			linkSpeedSums.put(link.getId(), 0.);
			linkSpeedNumbers.put(link.getId(), 0.);
		}
		
		
		
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				double arrivalTime = 0;
				double departureTime = 0;
				for (int ii = 0; ii < transitRoute.getStops().size(); ii++) {
					
					arrivalTime = transitRoute.getStops().get(ii).getArrivalOffset();
					Id<Link> linkId = null;
					if (ii == 0) {
						linkId = transitRoute.getRoute().getStartLinkId();
						linkSpeedSums.replace(linkId, 50.);
						linkSpeedNumbers.replace(linkId, 1.);

					}
					
					else {
						
						if (ii == transitRoute.getStops().size()-1) {
							linkId = transitRoute.getRoute().getEndLinkId();
						}
						
						else {
							linkId = transitRoute.getRoute().getLinkIds().get(ii-1);
						}
						
						Double speedSum = linkSpeedSums.get(linkId);
						speedSum += scenario.getNetwork().getLinks().get(linkId).getLength() / (arrivalTime - departureTime);
						linkSpeedSums.replace(linkId, speedSum);
						
						Double speedNumber = linkSpeedNumbers.get(linkId);
						speedNumber += 1;
						linkSpeedNumbers.replace(linkId, speedNumber);
						
						
					}
					
					departureTime = transitRoute.getStops().get(ii).getDepartureOffset();
				}
				
				
			}
			
		}
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			double speed = linkSpeedSums.get(link.getId()) / linkSpeedNumbers.get(link.getId());
			link.setFreespeed(speed);
			if (speed>200./3.6) {
				log.warn("Link speed is higher than 200 km/h on link " + link.getId()+ " - Speed is " + Math.round(speed*3.6) + " km/h");
			}
			if (speed<30./3.6) {
				log.warn("Link speed is lower than 30 km/h on link " + link.getId()+ " - Speed is " + Math.round(speed*3.6) + " km/h");
			}
		}
		
	}
	
	private static void setLinkSpeedsToMax(Scenario scenario) {
		Map<Id<Link>, Double> linkMaxSpeed = new HashMap<>();
		
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			linkMaxSpeed.put(link.getId(), 0.);
		}
		
		
		
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				double arrivalTime = 0;
				double departureTime = 0;
				for (int ii = 0; ii < transitRoute.getStops().size(); ii++) {
					
					arrivalTime = transitRoute.getStops().get(ii).getArrivalOffset();
					Id<Link> linkId = null;
					if (ii == 0) {
						linkId = transitRoute.getRoute().getStartLinkId();
						linkMaxSpeed.replace(linkId, 50.);
					}
					
					else {
						
						if (ii == transitRoute.getStops().size()-1) {
							linkId = transitRoute.getRoute().getEndLinkId();
						}
						
						else {
							linkId = transitRoute.getRoute().getLinkIds().get(ii-1);
						}
						
						Double prevSpeed = linkMaxSpeed.get(linkId);
						double newSpeed = 50.;
						if (arrivalTime - departureTime != 0) {
							newSpeed = scenario.getNetwork().getLinks().get(linkId).getLength() / (- 1 + arrivalTime - departureTime);
						}

						if(newSpeed > prevSpeed) {
							linkMaxSpeed.replace(linkId, newSpeed);
							
						}
						
					}
					
					departureTime = transitRoute.getStops().get(ii).getDepartureOffset();
				}
				
				
			}
			
		}
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			double speed = linkMaxSpeed.get(link.getId());
			link.setFreespeed(speed);
			if (speed>200./3.6) {
				log.warn("Link speed is higher than 200 km/h on link " + link.getId()+ " - Speed is " + Math.round(speed*3.6) + " km/h");
			}
			if (speed<30./3.6) {
				log.warn("Link speed is lower than 30 km/h on link " + link.getId()+ " - Speed is " + Math.round(speed*3.6) + " km/h");
			}
		}
		
	}
	
	private static void mergeSchedules(String prefix, TransitScheduleFactory tsf, TransitSchedule schedule, TransitSchedule toBeMerged) {
		 toBeMerged.getTransitLines().values().forEach(transitLine -> {
			TransitLine transitLineWithNewId = tsf.createTransitLine(Id.create(prefix + transitLine.getId().toString(), TransitLine.class));
			if (checkIfLineIsInShape(transitLine)) {
				transitLine.getRoutes().values().forEach(route -> {
					List<TransitRouteStop> stops = new ArrayList<>();
					route.getStops().forEach(stop -> {
						TransitStopFacility transitStopWithNewId;
						if (!schedule.getFacilities().containsKey(Id.create(prefix + stop.getStopFacility().getId(), TransitStopFacility.class))) {
							transitStopWithNewId = tsf.createTransitStopFacility(Id.create(prefix + stop.getStopFacility().getId(), TransitStopFacility.class), stop.getStopFacility().getCoord(), stop.getStopFacility().getIsBlockingLane());
							transitStopWithNewId.setName(stop.getStopFacility().getName());
							schedule.addStopFacility(transitStopWithNewId);
						}
						else {
							transitStopWithNewId = schedule.getFacilities().get(Id.create(prefix + stop.getStopFacility().getId(), TransitStopFacility.class));
						}
						TransitRouteStop transitRouteStopWithNewId = tsf.createTransitRouteStop(transitStopWithNewId, stop.getArrivalOffset(), stop.getDepartureOffset());
						transitRouteStopWithNewId.setAwaitDepartureTime(true);
						stops.add(transitRouteStopWithNewId);
					});
					TransitRoute transitRouteWithNewId = tsf.createTransitRoute(Id.create(prefix + route.getId().toString(), TransitRoute.class), route.getRoute(), stops, route.getTransportMode());
					transitLineWithNewId.addRoute(transitRouteWithNewId);	
					route.getDepartures().values().forEach(dep -> {
						Departure departureWithNewId = tsf.createDeparture(Id.create(prefix + dep.getId().toString(), Departure.class), dep.getDepartureTime());
						departureWithNewId.setVehicleId(Id.createVehicleId(prefix + dep.getVehicleId().toString()));
						transitRouteWithNewId.addDeparture(departureWithNewId);
					});
				});
			transitLineWithNewId.setName(transitLine.getName());
			schedule.addTransitLine(transitLineWithNewId);
			}
		});
	}

	private static void mergeVehicles(String prefix, VehiclesFactory vehiclesFactory, Vehicles vehicles, Vehicles toBeMerged) {
		
		toBeMerged.getVehicleTypes().values().forEach(vehicleType -> {	
			VehicleType vehicleTypeWithNewId = vehiclesFactory.createVehicleType(Id.create(prefix + vehicleType.getId().toString(), VehicleType.class));
			vehicleTypeWithNewId.getCapacity().setSeats(1000);
			vehicles.addVehicleType(vehicleTypeWithNewId);
		});
		toBeMerged.getVehicles().values().forEach(vehicle -> {
			VehicleType vehicleTypeWithNewId = vehiclesFactory.createVehicleType(Id.create(prefix + vehicle.getType().getId().toString(), VehicleType.class));			
			vehicleTypeWithNewId.getCapacity().setSeats(1000);
			Vehicle vehicleWithNewId = vehiclesFactory.createVehicle(Id.createVehicleId(prefix + vehicle.getId().toString()),vehicleTypeWithNewId);
			vehicles.addVehicle(vehicleWithNewId);
		});

	}
	
	private static boolean checkIfLineIsInShape(TransitLine line) {

		boolean lineIsInShape = false;
		for (TransitRoute route : line.getRoutes().values()) {
			for (TransitRouteStop routeStop : route.getStops()) {
				Coord coord = ct.transform(routeStop.getStopFacility().getCoord());
				Geometry ageometry = regions.get("Nordrhein-Westfalen");
				if (isInGeometry(coord,  ageometry)) {
					lineIsInShape = true;
				}
			}
		}
		return lineIsInShape;
	}
	
	private static boolean isInGeometry(Coord coord, Geometry geometry) {
		return geometry.contains(MGC.coord2Point(coord));
//		return geometries.stream().anyMatch(geometry -> geometry.contains(MGC.coord2Point(coord)));
	}
	

}
