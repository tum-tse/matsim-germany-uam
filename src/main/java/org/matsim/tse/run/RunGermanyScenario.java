package org.matsim.tse.run;

import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.run.AirplaneTrainSwitcherIndividualRaptorParametersForPerson;
import org.matsim.run.MyMainModeIdentifier;
import org.matsim.vehicles.MatsimVehicleWriter;

public class RunGermanyScenario {

    //TODO for SHK: How to deal with the agents whose activity is outside of the Germany network?

    private static final Logger logger = Logger.getLogger(RunGermanyScenario.class);

    public static void main(String[] args) {
/*        logger.info("Started the Microsimulation Transport Orchestrator (MITO) based on 2017 models");
        MitoModelGermany model = MitoModelGermany.standAloneModel(args[0], MunichImplementationConfig.get());
        model.run();
        final DataSet dataSet = model.getData();*/

        boolean runAssignment = true;

        if (runAssignment) {
            logger.info("Running traffic assignment in MATsim");

            Config config;
            if (args.length > 1 && args[1] != null) {
                config = ConfigUtils.loadConfig(args[1]);
                ConfigureMatsim.setDemandSpecificConfigSettings(config);
            } else {
                logger.warn("Using a fallback config with default values as no initial config has been provided.");
                config = ConfigureMatsim.configureMatsim();
            }

            config.controler().setOutputDirectory("scenarios/tse/base");
            config.network().setInputFile("/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/final-version/network_with-rail.xml.gz");
            config.transit().setTransitScheduleFile("/home/tumtse/Documents/haowu/MSM/matsim-germany_vsp/Germany/input/2020_Train_GTFS_transitSchedule.xml.gz");
            config.transit().setVehiclesFile("/home/tumtse/Documents/haowu/MSM/matsim-germany_vsp/Germany/input/2020_Train_GTFS_transitVehicles.xml.gz");
            config.plans().setInputFile("/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/final-version/plan_all-trips_2030_1percent_transformed.xml.gz");

            //MutableScenario matsimScenario = (MutableScenario) ScenarioUtils.loadScenario(config);
            Scenario matsimScenario = ScenarioUtils.loadScenario(config) ;
            Controler controler = new Controler(matsimScenario);

            // TODO for SHK: solve the below quick fix
            matsimScenario.getNetwork().removeLink(matsimScenario.getNetwork().getLinks().get(Id.createLinkId(1795223)).getId());
            //matsimScenario.getNetwork().removeLink(matsimScenario.getNetwork().getLinks().get(Id.createLinkId(1795222)).getId());

            // use the (congested) car travel time for the teleported carPassenger mode
            // Seems like a nice trick, but does not work so well: All carPassenger trips found in the 0th iteration use the free speed travel time, which is much too fast.  And they
            // remember this forever. kai, mar'19
            controler.addOverridingModule( new AbstractModule() {
                @Override public void install() {
                    addTravelTimeBinding( "carPassenger" ).to( networkTravelTime() );
                    addTravelDisutilityFactoryBinding( "carPassenger" ).to( carTravelDisutilityFactoryKey() );
                }
            } );

            controler.addOverridingModule(new SwissRailRaptorModule());
            controler.addOverridingModule( new AbstractModule(){
                @Override public void install() {
//				this.bindScoringFunctionFactory().to( MyScoringFunctionFactory.class ) ;
//				install( new SwissRailRaptorModule() );
                    //bind(RaptorParametersForPerson.class).to(AirplaneTrainSwitcherIndividualRaptorParametersForPerson.class);
                    bind(AnalysisMainModeIdentifier.class).to(MyMainModeIdentifier.class);
                }
            } );

            //write the input files
            String inputDir = "/home/tumtse/Documents/haowu/MSM/matsim-germany_msm/matsim-format/final-version/latest_input_files/";
            ConfigUtils.writeConfig(matsimScenario.getConfig(), inputDir + "configGermany.xml");

            NetworkUtils.writeNetwork(matsimScenario.getNetwork(), inputDir + "GermanyNetwork.xml.gz");
            new MatsimVehicleWriter(matsimScenario.getTransitVehicles()).writeFile(inputDir + "GermanyTransitVehicles.xml.gz");
            new TransitScheduleWriter(matsimScenario.getTransitSchedule()).writeFile(inputDir + "GermanyTransitSchedule.xml.gz");

            controler.run();

        }
    }
}
