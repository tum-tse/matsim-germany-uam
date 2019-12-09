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


package org.matsim.run;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

import org.matsim.contrib.accessibility.utils.MergeNetworks;


/**
* @author smueller
*/

public class RunGermany {
	
	private static final String inputNetworkFile = "../matsim-input/germany/network.xml.gz";
	private static final String inputDBGTFSNetworkFile = "../shared-svn/studies/countries/de/train/db-fv-gtfs-master/MATSimFiles/2016/GTFSNetworkDB.xml.gz";
	private static final String inputAirplaneNetworkFile = "../matsim-input/germany/air_network.xml";
	private static final String inputPlansFile = "../matsim-input/germany/populationTrainPlaneBERMUC5.0pct.xml";
	private static final String inputDBGTFSScheduleFile ="../shared-svn/studies/countries/de/train/db-fv-gtfs-master/MATSimFiles/2016/GTFSTransitScheduleDB.xml.gz";
	private static final String inputDBGTFSVehiclesFile ="../shared-svn/studies/countries/de/train/db-fv-gtfs-master/MATSimFiles/2016/GTFSTransitVehiclesDB.xml.gz";
	private static final String inputAirplaneScheduleFile = "../matsim-input/germany/flight_transit_schedule.xml";
	private static final String inputAirplaneVehiclesFile ="../matsim-input/germany/flight_transit_vehicles.xml";
	private static final String outputDir = "../matsim-input/germany/Output/";
	private static final int noOfThreads = 8;

	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDir);
		
		config.global().setCoordinateSystem("EPSG:31467");
		config.global().setNumberOfThreads(noOfThreads);
		
		Collection<String> networkModes = new HashSet<>();
		networkModes.add("car");
		config.plansCalcRoute().setNetworkModes(networkModes);

		ActivityParams originAcitivityParams = new ActivityParams("origin");
		originAcitivityParams.setTypicalDuration(3600);
		config.planCalcScore().addActivityParams(originAcitivityParams);
		ActivityParams shopAcitivityParams = new ActivityParams("shop");
		shopAcitivityParams.setTypicalDuration(3600);
		config.planCalcScore().addActivityParams(shopAcitivityParams);
		ActivityParams businessAcitivityParams = new ActivityParams("business");
		businessAcitivityParams.setTypicalDuration(3600);
		config.planCalcScore().addActivityParams(businessAcitivityParams);
		ActivityParams holidayAcitivityParams = new ActivityParams("holiday");
		holidayAcitivityParams.setTypicalDuration(3600);
		config.planCalcScore().addActivityParams(holidayAcitivityParams);
		ActivityParams workAcitivityParams = new ActivityParams("work");
		workAcitivityParams.setTypicalDuration(3600);
		config.planCalcScore().addActivityParams(workAcitivityParams);
		ActivityParams educationAcitivityParams = new ActivityParams("education");
		educationAcitivityParams.setTypicalDuration(3600);
		config.planCalcScore().addActivityParams(educationAcitivityParams);
		ActivityParams otherAcitivityParams = new ActivityParams("other");
		otherAcitivityParams.setTypicalDuration(3600);
		config.planCalcScore().addActivityParams(otherAcitivityParams);
		
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(36. * 3600);
		config.qsim().setNumberOfThreads(noOfThreads);
		config.qsim().setMainModes(networkModes);
		
		// vsp defaults
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.strategy().setMaxAgentPlanMemorySize(3);	
		config.strategy().clearStrategySettings();
		
		StrategySettings stratSetsReRoute = new StrategySettings();
		stratSetsReRoute.setStrategyName(DefaultStrategy.ReRoute);
		stratSetsReRoute.setWeight(1.0);
		
		config.strategy().addStrategySettings(stratSetsReRoute);
		
		config.network().setInputFile(inputNetworkFile);
		
		config.plans().setInputFile(inputPlansFile);
		
		config.transit().setUseTransit(true);
		
		Set<String> transitModes = new HashSet<>();
		transitModes.add(TransportMode.train);
		transitModes.add(TransportMode.airplane);
		config.transit().setTransitModes(transitModes );

		SwissRailRaptorConfigGroup srrConfig = new SwissRailRaptorConfigGroup();
		srrConfig.setUseModeMappingForPassengers(true);
		
		ModeMappingForPassengersParameterSet modeMappingTrain = new ModeMappingForPassengersParameterSet();
		modeMappingTrain.setPassengerMode(TransportMode.train);
		modeMappingTrain.setRouteMode(TransportMode.train);
		srrConfig.addModeMappingForPassengers(modeMappingTrain);
		
		ModeMappingForPassengersParameterSet modeMappingAirplane = new ModeMappingForPassengersParameterSet();
		modeMappingAirplane.setPassengerMode(TransportMode.airplane);
		modeMappingAirplane.setRouteMode(TransportMode.airplane);
		srrConfig.addModeMappingForPassengers(modeMappingAirplane);
		
		srrConfig.setUseIntermodalAccessEgress(true);
		
		IntermodalAccessEgressParameterSet intermodalAccessEgressParameterSet = new IntermodalAccessEgressParameterSet();
		intermodalAccessEgressParameterSet.setMode("car");
		intermodalAccessEgressParameterSet.setMaxRadius(500 * 1000);
		intermodalAccessEgressParameterSet.setInitialSearchRadius(100 * 1000);
		intermodalAccessEgressParameterSet.setSearchExtensionRadius(150 * 1000);
//		intermodalAccessEgressParameterSet.setStopFilterAttribute("");
		srrConfig.addIntermodalAccessEgress(intermodalAccessEgressParameterSet);
		
		config.addModule(srrConfig);
		
		ModeParams scorePt = config.planCalcScore().getModes().get(TransportMode.pt);
		
		ModeParams scoreTrain = new ModeParams(TransportMode.train);
		scoreTrain.setConstant(scorePt.getConstant());
		scoreTrain.setDailyMonetaryConstant(scorePt.getDailyMonetaryConstant());
		scoreTrain.setDailyUtilityConstant(scorePt.getDailyUtilityConstant());
		scoreTrain.setMarginalUtilityOfDistance(scorePt.getMarginalUtilityOfDistance());
		scoreTrain.setMarginalUtilityOfTraveling(scorePt.getMarginalUtilityOfTraveling());
		scoreTrain.setMonetaryDistanceRate(scorePt.getMonetaryDistanceRate());
		config.planCalcScore().addModeParams(scoreTrain);
		
		ModeParams scoreAirplane = new ModeParams(TransportMode.airplane);
		scoreAirplane.setConstant(scorePt.getConstant());
		scoreAirplane.setDailyMonetaryConstant(scorePt.getDailyMonetaryConstant());
		scoreAirplane.setDailyUtilityConstant(scorePt.getDailyUtilityConstant());
		scoreAirplane.setMarginalUtilityOfDistance(scorePt.getMarginalUtilityOfDistance());
		scoreAirplane.setMarginalUtilityOfTraveling(scorePt.getMarginalUtilityOfTraveling());
		scoreAirplane.setMonetaryDistanceRate(scorePt.getMonetaryDistanceRate());
		config.planCalcScore().addModeParams(scoreAirplane);
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Network DBNetwork = NetworkUtils.readNetwork(inputDBGTFSNetworkFile);
		Set<String> trainModes = new HashSet<>();
		trainModes.add(TransportMode.train);
		DBNetwork.getLinks().values().forEach(l -> l.setAllowedModes(trainModes));
		MergeNetworks.merge(scenario.getNetwork(),"", DBNetwork);
		
		Config trainConfig = ConfigUtils.createConfig();
		trainConfig.transit().setTransitScheduleFile(inputDBGTFSScheduleFile);
		trainConfig.transit().setVehiclesFile(inputDBGTFSVehiclesFile);
		Scenario trainScenario = ScenarioUtils.loadScenario(trainConfig);
		trainScenario.getTransitSchedule().getTransitLines().values().forEach(line -> line.getRoutes().values().forEach(route -> route.setTransportMode(TransportMode.train)));
		mergeSchedules(scenario.getTransitSchedule(), trainScenario.getTransitSchedule());
		mergeVehicles(scenario.getTransitVehicles(), trainScenario.getTransitVehicles());
		
		Network airplaneNetwork = NetworkUtils.readNetwork(inputAirplaneNetworkFile);
		Set<String> airplaneModes = new HashSet<>();
		airplaneModes.add(TransportMode.airplane);
		airplaneNetwork.getLinks().values().forEach(l -> l.setAllowedModes(airplaneModes));
		MergeNetworks.merge(scenario.getNetwork(),"", airplaneNetwork);
		
		Config airplaneConfig = ConfigUtils.createConfig();
		airplaneConfig.transit().setTransitScheduleFile(inputAirplaneScheduleFile);
		airplaneConfig.transit().setVehiclesFile(inputAirplaneVehiclesFile);
		Scenario airplaneScenario = ScenarioUtils.loadScenario(airplaneConfig);
		airplaneScenario.getTransitSchedule().getTransitLines().values().forEach(line -> line.getRoutes().values().forEach(route -> route.setTransportMode(TransportMode.airplane)));
		mergeSchedules(scenario.getTransitSchedule(), airplaneScenario.getTransitSchedule());
		mergeVehicles(scenario.getTransitVehicles(), airplaneScenario.getTransitVehicles());
		
		Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new SwissRailRaptorModule());
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.bindScoringFunctionFactory().to( MyScoringFunctionFactory.class ) ;
//				install( new SwissRailRaptorModule() );
				bind(RaptorParametersForPerson.class).to(AirplaneTrainSwitcherIndividualRaptorParametersForPerson.class);
			}
		} );
		
		controler.run();

	}
	
	private static void mergeSchedules(TransitSchedule schedule, TransitSchedule toBeMerged) {
		toBeMerged.getFacilities().values().forEach(schedule::addStopFacility);
		toBeMerged.getTransitLines().values().forEach(schedule::addTransitLine);
	}

	private static void mergeVehicles(Vehicles vehicles, Vehicles toBeMerged) {
		toBeMerged.getVehicleTypes().values().forEach(vehicles::addVehicleType);
		toBeMerged.getVehicles().values().forEach(vehicles::addVehicle);
	}

}

class MyScoringFunctionFactory implements ScoringFunctionFactory {

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		// TODO Auto-generated method stub
		return new MyScoringFunction();
	}
	
}

class MyScoringFunction implements ScoringFunction {
	
	private double score;

	@Override
	public void handleActivity(Activity activity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleLeg(Leg leg) {
		score -= leg.getTravelTime();
		
		if (leg.getMode().equals(TransportMode.airplane) ) {
			score -= 2 * 3600;
			
	
		}
		
		
	}

	@Override
	public void agentStuck(double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addMoney(double amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getScore() {
		// TODO Auto-generated method stub
		return score;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
	
}
