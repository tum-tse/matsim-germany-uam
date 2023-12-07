package org.matsim.tse.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.util.HashSet;
import java.util.Set;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;

public class ConfigureMatsim {

    public static double siloSamplingFactor = 0.01; //TODO: my own setting, need to check if this is correct

    public static final String longDistanceTrain = 		"longDistanceTrain";
    public static final String regionalTrain = 			"regionalTrain";
    public static final String localPublicTransport = 		"localPublicTransport";

    public static Config configureMatsim() {



        //String outputDirectory = outputDirectoryRoot + "/" + runId + "/";
        //matsimConfig.controler().setRunId(runId);
        //matsimConfig.controler().setOutputDirectory(outputDirectory);
        Config config = ConfigUtils.createConfig();
        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(200);
        config.controler().setMobsim("qsim");
        config.controler().setWritePlansInterval(1);
        config.controler().setWriteEventsInterval(1); //TODO: need to fix!
        config.controler().setWriteTripsInterval(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        config.qsim().setEndTime(26 * 3600);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        //config.vspExperimental().setWritingOutputEvents(true); // writes final events into toplevel directory

        {
            StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
            strategySettings.setStrategyName("ChangeExpBeta");
            strategySettings.setWeight(0.85);
            config.strategy().addStrategySettings(strategySettings);
        }
        {
            StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
            strategySettings.setStrategyName("ReRoute");
            strategySettings.setWeight(0.05);
            config.strategy().addStrategySettings(strategySettings);
        }
        {
            StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
            strategySettings.setStrategyName("SubtourModeChoice");
            strategySettings.setWeight(0.05);
            config.strategy().addStrategySettings(strategySettings);
        }
        String[] subtourModes = new String[]{TransportMode.car, /*"carPassenger",*/ "longDistancePt", TransportMode.bike, TransportMode.walk, TransportMode.airplane}; //TODO: need to set the scoring params for carPassenger correctly!
        String[] chainBasedModes = new String[]{TransportMode.car, TransportMode.bike};
        config.subtourModeChoice().setModes(subtourModes);
        config.subtourModeChoice().setChainBasedModes(chainBasedModes);

        {
            StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
            strategySettings.setStrategyName("TimeAllocationMutator");
            strategySettings.setWeight(0.05);
            config.strategy().addStrategySettings(strategySettings);
        }
//        {
//            config.timeAllocationMutator().setMutationRange(1800);
//            config.timeAllocationMutator().setAffectingDuration(true);
//            StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
//            strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute);
//            strategySettings.setWeight(0.1);
//            config.strategy().addStrategySettings(strategySettings);
//        }

        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
        config.strategy().setMaxAgentPlanMemorySize(4);

        // For short distance trips
        PlanCalcScoreConfigGroup.ActivityParams homeActivity = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 60 * 60);
        config.planCalcScore().addActivityParams(homeActivity);

        PlanCalcScoreConfigGroup.ActivityParams workActivity = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(workActivity);

        PlanCalcScoreConfigGroup.ActivityParams educationActivity = new PlanCalcScoreConfigGroup.ActivityParams("education");
        educationActivity.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(educationActivity);

        PlanCalcScoreConfigGroup.ActivityParams shoppingActivity = new PlanCalcScoreConfigGroup.ActivityParams("shopping");
        shoppingActivity.setTypicalDuration(1 * 60 * 60);
        config.planCalcScore().addActivityParams(shoppingActivity);

        PlanCalcScoreConfigGroup.ActivityParams otherActivity = new PlanCalcScoreConfigGroup.ActivityParams("other");
        otherActivity.setTypicalDuration(1 * 60 * 60);
        config.planCalcScore().addActivityParams(otherActivity);

        PlanCalcScoreConfigGroup.ActivityParams airportActivity = new PlanCalcScoreConfigGroup.ActivityParams("airport");
        airportActivity.setTypicalDuration(1 * 60 * 60);
        config.planCalcScore().addActivityParams(airportActivity);

        // For long distance trips
        PlanCalcScoreConfigGroup.ActivityParams privateActivity = new PlanCalcScoreConfigGroup.ActivityParams("private");
        privateActivity.setTypicalDuration(1 * 60 * 60); //TODO: need to set the value correctly!
        config.planCalcScore().addActivityParams(privateActivity);

        PlanCalcScoreConfigGroup.ActivityParams businessActivity = new PlanCalcScoreConfigGroup.ActivityParams("business");
        businessActivity.setTypicalDuration(1 * 60 * 60); //TODO: need to set the value correctly!
        config.planCalcScore().addActivityParams(businessActivity);

        PlanCalcScoreConfigGroup.ActivityParams leisureActivity = new PlanCalcScoreConfigGroup.ActivityParams("leisure");
        leisureActivity.setTypicalDuration(1 * 60 * 60); //TODO: need to set the value correctly!
        config.planCalcScore().addActivityParams(leisureActivity);

        PlanCalcScoreConfigGroup.ActivityParams visitorActivity = new PlanCalcScoreConfigGroup.ActivityParams("visitor");
        visitorActivity.setTypicalDuration(1 * 60 * 60); //TODO: need to set the value correctly!
        config.planCalcScore().addActivityParams(visitorActivity);

/*        PlansCalcRouteConfigGroup.ModeRoutingParams carPassengerParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("carPassenger"); // TODO: I think we do not need to do this for car mode
        carPassengerParams.setTeleportedModeFreespeedFactor(1.0);
        config.plansCalcRoute().addModeRoutingParams(carPassengerParams);*/

/*        //TODO: need to model pt
        PlansCalcRouteConfigGroup.ModeRoutingParams ptParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("longDistancePt");
        ptParams.setBeelineDistanceFactor(1.5);
        ptParams.setTeleportedModeSpeed(50 / 3.6);
        config.plansCalcRoute().addModeRoutingParams(ptParams);*/

        PlansCalcRouteConfigGroup.ModeRoutingParams bicycleParams = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.bike);
        bicycleParams.setBeelineDistanceFactor(1.3);
        bicycleParams.setTeleportedModeSpeed(15 / 3.6);
        config.plansCalcRoute().addModeRoutingParams(bicycleParams);

        PlansCalcRouteConfigGroup.ModeRoutingParams walkParams = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.walk);
        walkParams.setBeelineDistanceFactor(1.3);
        walkParams.setTeleportedModeSpeed(5 / 3.6);
        config.plansCalcRoute().addModeRoutingParams(walkParams);

        PlansCalcRouteConfigGroup.ModeRoutingParams airplaneParams = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.airplane);
        airplaneParams.setBeelineDistanceFactor(1.0);
        airplaneParams.setTeleportedModeSpeed(800 / 3.6); //TODO for SHK: For commercial airliners, the average cruising speeds typically range from 550 to 600 mph (approximately 885 to 965 km/h), while takeoff speeds are generally between 130 to 180 mph (about 209 to 290 km/h), and landing speeds are usually around 160 to 180 mph (257 to 290 km/h).
        config.plansCalcRoute().addModeRoutingParams(airplaneParams);

        String runId = "tse_germany_scenario";
        config.controler().setRunId(runId);
        //config.network().setInputFile();

        config.qsim().setNumberOfThreads(32);
        config.global().setNumberOfThreads(16);
        config.parallelEventHandling().setNumberOfThreads(16);
        //config.qsim().setUsingThreadpool(false); removed for compatibility with 14.0


        config.global().setCoordinateSystem("EPSG:31467");
        //config.plans().setInputCRS("EPSG:31468");

        //set "longDistancePt"
        String[] changeModes = new String[2];
        changeModes[0] = "car";
        changeModes[1] = "longDistancePt";
        config.changeMode().setModes(changeModes);

        config.transit().setUseTransit(true);
        //config.transitRouter().setMaxBeelineWalkConnectionDistance(500);
        Set<String> transitModes = new HashSet<>();
//		transitModes.add(TransportMode.train);
//		transitModes.add(TransportMode.airplane);
        transitModes.add("longDistancePt");
        config.transit().setTransitModes(transitModes );

        SwissRailRaptorConfigGroup srrConfig = new SwissRailRaptorConfigGroup();
        srrConfig.setUseModeMappingForPassengers(true);
        SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet modeMappingLongDistanceTrain = new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet();
        modeMappingLongDistanceTrain.setPassengerMode(longDistanceTrain);
        modeMappingLongDistanceTrain.setRouteMode(longDistanceTrain);
        srrConfig.addModeMappingForPassengers(modeMappingLongDistanceTrain);

        SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet modeMappingRegionalTrain = new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet();
        modeMappingRegionalTrain.setPassengerMode(regionalTrain);
        modeMappingRegionalTrain.setRouteMode(regionalTrain);
        srrConfig.addModeMappingForPassengers(modeMappingRegionalTrain);

        SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet modeMappingTrainLocalPublicTransport = new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet();
        modeMappingTrainLocalPublicTransport.setPassengerMode(localPublicTransport);
        modeMappingTrainLocalPublicTransport.setRouteMode(localPublicTransport);
        srrConfig.addModeMappingForPassengers(modeMappingTrainLocalPublicTransport);

        //TODO: need to check if this is neccessary!
        srrConfig.setUseIntermodalAccessEgress(true);
        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet intermodalAccessEgressParameterSetWalk = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        intermodalAccessEgressParameterSetWalk.setMode("walk");
        intermodalAccessEgressParameterSetWalk.setMaxRadius(5 * 1000);
        intermodalAccessEgressParameterSetWalk.setInitialSearchRadius(1 * 1000);
        intermodalAccessEgressParameterSetWalk.setSearchExtensionRadius(1 * 1000);
        srrConfig.addIntermodalAccessEgress(intermodalAccessEgressParameterSetWalk);
/*        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet intermodalAccessEgressParameterSetAirportWithCar = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        intermodalAccessEgressParameterSetAirportWithCar.setMode("car");
        intermodalAccessEgressParameterSetAirportWithCar.setMaxRadius(200 * 1000);
        intermodalAccessEgressParameterSetAirportWithCar.setInitialSearchRadius(50 * 1000);
        intermodalAccessEgressParameterSetAirportWithCar.setSearchExtensionRadius(50 * 1000);
        intermodalAccessEgressParameterSetAirportWithCar.setStopFilterAttribute("type");
        intermodalAccessEgressParameterSetAirportWithCar.setStopFilterValue("airport");
        srrConfig.addIntermodalAccessEgress(intermodalAccessEgressParameterSetAirportWithCar);*/


        ModeParams scorePt = config.planCalcScore().getModes().get(TransportMode.pt);

//		ModeParams scoreTrain = new ModeParams(TransportMode.train);
//		scoreTrain.setConstant(scorePt.getConstant());
//		scoreTrain.setDailyMonetaryConstant(scorePt.getDailyMonetaryConstant());
//		scoreTrain.setDailyUtilityConstant(scorePt.getDailyUtilityConstant());
//		scoreTrain.setMarginalUtilityOfDistance(scorePt.getMarginalUtilityOfDistance());
//		scoreTrain.setMarginalUtilityOfTraveling(scorePt.getMarginalUtilityOfTraveling());
//		scoreTrain.setMonetaryDistanceRate(-0.0001);
//		config.planCalcScore().addModeParams(scoreTrain);

        ModeParams scoreLongDistanceTrain = new ModeParams(longDistanceTrain);
        scoreLongDistanceTrain.setConstant(-6);
        scoreLongDistanceTrain.setDailyMonetaryConstant(scorePt.getDailyMonetaryConstant());
        scoreLongDistanceTrain.setDailyUtilityConstant(scorePt.getDailyUtilityConstant());
        scoreLongDistanceTrain.setMarginalUtilityOfDistance(scorePt.getMarginalUtilityOfDistance());
        scoreLongDistanceTrain.setMarginalUtilityOfTraveling(-3);
        scoreLongDistanceTrain.setMonetaryDistanceRate(-0.0001);
        config.planCalcScore().addModeParams(scoreLongDistanceTrain);

        ModeParams scoreRegionalTrain = new ModeParams(regionalTrain);
        scoreRegionalTrain.setConstant(scorePt.getConstant());
        scoreRegionalTrain.setDailyMonetaryConstant(scorePt.getDailyMonetaryConstant());
        scoreRegionalTrain.setDailyUtilityConstant(scorePt.getDailyUtilityConstant());
        scoreRegionalTrain.setMarginalUtilityOfDistance(scorePt.getMarginalUtilityOfDistance());
        scoreRegionalTrain.setMarginalUtilityOfTraveling(-3);
        scoreRegionalTrain.setMonetaryDistanceRate(-0.0001);
        config.planCalcScore().addModeParams(scoreRegionalTrain);

        ModeParams scoreLocalPublicTransport = new ModeParams(localPublicTransport);
        scoreLocalPublicTransport.setConstant(scorePt.getConstant());
        scoreLocalPublicTransport.setDailyMonetaryConstant(scorePt.getDailyMonetaryConstant());
        scoreLocalPublicTransport.setDailyUtilityConstant(scorePt.getDailyUtilityConstant());
        scoreLocalPublicTransport.setMarginalUtilityOfDistance(scorePt.getMarginalUtilityOfDistance());
        scoreLocalPublicTransport.setMarginalUtilityOfTraveling(-3);
        scoreLocalPublicTransport.setMonetaryDistanceRate(0);
        config.planCalcScore().addModeParams(scoreLocalPublicTransport);

/*        ModeParams scoreAirplane = new ModeParams(TransportMode.airplane);
        scoreAirplane.setConstant(-15);
        scoreAirplane.setDailyMonetaryConstant(scorePt.getDailyMonetaryConstant());
        scoreAirplane.setDailyUtilityConstant(scorePt.getDailyUtilityConstant());
        scoreAirplane.setMarginalUtilityOfDistance(scorePt.getMarginalUtilityOfDistance());
        scoreAirplane.setMarginalUtilityOfTraveling(-6);
        scoreAirplane.setMonetaryDistanceRate(-0.0001);
        config.planCalcScore().addModeParams(scoreAirplane);*/

        config.addModule(srrConfig);

        //config.transit().setInputScheduleCRS("EPSG:31467");
        //TODO: need to also set the CRS for pt network




        config.controler().setLastIteration(200); //TODO: my own setting
        config.controler().setWritePlansInterval(config.controler().getLastIteration());
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());

        config.qsim().setStuckTime(10);

        config.qsim().setFlowCapFactor(siloSamplingFactor);
        config.qsim().setStorageCapFactor(siloSamplingFactor);


/*        String[] networkModes = Resources.instance.getArray(Properties.MATSIM_NETWORK_MODES, new String[]{"autoDriver"});
        Set<String> networkModesSet = new HashSet<>();

        for (String mode : networkModes) {
            String matsimMode = Mode.getMatsimMode(Mode.valueOf(mode));
            if (!networkModesSet.contains(matsimMode)) {
                networkModesSet.add(matsimMode);
            }
        }*/

        Set<String> networkModesSet = new HashSet<>();
        networkModesSet.add(TransportMode.car);
        //networkModesSet.add("longDistancePt");
        //networkModesSet.add(TransportMode.bike);
        //networkModesSet.add(TransportMode.walk);
        networkModesSet.add("carPassenger");
        config.plansCalcRoute().setNetworkModes(networkModesSet);

        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink); //TODO: need to check what does this do?

        return config;
    }



    @Deprecated
    public static void setDemandSpecificConfigSettings(Config config) {

        config.qsim().setFlowCapFactor(siloSamplingFactor);
        config.qsim().setStorageCapFactor(siloSamplingFactor);

        PlanCalcScoreConfigGroup.ActivityParams homeActivity = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 60 * 60);
        config.planCalcScore().addActivityParams(homeActivity);

        PlanCalcScoreConfigGroup.ActivityParams workActivity = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(workActivity);

        PlanCalcScoreConfigGroup.ActivityParams educationActivity = new PlanCalcScoreConfigGroup.ActivityParams("education");
        educationActivity.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(educationActivity);

        PlanCalcScoreConfigGroup.ActivityParams shoppingActivity = new PlanCalcScoreConfigGroup.ActivityParams("shopping");
        shoppingActivity.setTypicalDuration(1 * 60 * 60);
        config.planCalcScore().addActivityParams(shoppingActivity);

        PlanCalcScoreConfigGroup.ActivityParams otherActivity = new PlanCalcScoreConfigGroup.ActivityParams("other");
        otherActivity.setTypicalDuration(1 * 60 * 60);
        config.planCalcScore().addActivityParams(otherActivity);

        PlanCalcScoreConfigGroup.ActivityParams airportActivity = new PlanCalcScoreConfigGroup.ActivityParams("airport");
        airportActivity.setTypicalDuration(1 * 60 * 60);
        config.planCalcScore().addActivityParams(airportActivity);
    }
}
