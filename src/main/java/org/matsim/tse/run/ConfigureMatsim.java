package org.matsim.tse.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;

public class ConfigureMatsim {

    public static double siloSamplingFactor = 1; //TODO: my own setting, need to check if this is correct

    public static Config configureMatsim() {



        //String outputDirectory = outputDirectoryRoot + "/" + runId + "/";
        //matsimConfig.controler().setRunId(runId);
        //matsimConfig.controler().setOutputDirectory(outputDirectory);
        Config config = ConfigUtils.createConfig();
        config.controler().setFirstIteration(0);
        config.controler().setMobsim("qsim");
        config.controler().setWritePlansInterval(1);
        config.controler().setWriteEventsInterval(1);
        config.controler().setWriteTripsInterval(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        config.qsim().setEndTime(26 * 3600);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.vspExperimental().setWritingOutputEvents(true); // writes final events into toplevel directory

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
        String[] subtourModes = new String[]{TransportMode.car, /*"carPassenger",*/ TransportMode.pt, TransportMode.bike, TransportMode.walk}; //TODO: need to set the scoring params for carPassenger correctly!
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

/*        PlansCalcRouteConfigGroup.ModeRoutingParams carPassengerParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("carPassenger"); // TODO: I think we do not need to do this for car mode
        carPassengerParams.setTeleportedModeFreespeedFactor(1.0);
        config.plansCalcRoute().addModeRoutingParams(carPassengerParams);*/

/*        //TODO: need to model pt
        PlansCalcRouteConfigGroup.ModeRoutingParams ptParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("pt");
        ptParams.setBeelineDistanceFactor(1.5);
        ptParams.setTeleportedModeSpeed(50 / 3.6);
        config.plansCalcRoute().addModeRoutingParams(ptParams);*/

        PlansCalcRouteConfigGroup.ModeRoutingParams bicycleParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("bike");
        bicycleParams.setBeelineDistanceFactor(1.3);
        bicycleParams.setTeleportedModeSpeed(15 / 3.6);
        config.plansCalcRoute().addModeRoutingParams(bicycleParams);

        PlansCalcRouteConfigGroup.ModeRoutingParams walkParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("walk");
        walkParams.setBeelineDistanceFactor(1.3);
        walkParams.setTeleportedModeSpeed(5 / 3.6);
        config.plansCalcRoute().addModeRoutingParams(walkParams);

        String runId = "tse_germany_model";
        config.controler().setRunId(runId);
        //config.network().setInputFile();

        config.qsim().setNumberOfThreads(32);
        config.global().setNumberOfThreads(16);
        config.parallelEventHandling().setNumberOfThreads(16);
        //config.qsim().setUsingThreadpool(false); removed for compatibility with 14.0

        config.transit().setInputScheduleCRS("EPSG:31467");
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

        //config.plansCalcRoute().getNetworkModes().add(TransportMode.pt);
        Set<String> networkModesSet = new HashSet<>();
        networkModesSet.add(TransportMode.car);
        networkModesSet.add(TransportMode.pt);
        //networkModesSet.add(TransportMode.bike);
        //networkModesSet.add(TransportMode.walk);
        networkModesSet.add("carPassenger");
        config.plansCalcRoute().setNetworkModes(networkModesSet);

        //To prevent error message like "No route found from node Train_Fern_488 to node Train_Fern_668 by mode pt."
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

        return config;
    }



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
