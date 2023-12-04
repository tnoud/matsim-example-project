/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.project;

import com.google.inject.internal.asm.$Type;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.AccessEgressType;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.run.NetworkCleaner;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 * This class runs MATSim with some modifications.
 * For required resources to run this class, please contact the authors.
 *
 * @author Lasse Bienzeisler (TU Braunschweig), Torben Lelke (TU Braunschweig), Felix Petre
 */
public class RunMatsim {

    //TODO:
    // Reduce duplication,
    // make scalefactor set all relevant configs,
    // make iterations set all relevant configs,
    // auto-make new folder per run CHECK,
    // string to bool?? -> nah CHECK
    public static void main(String[] args) throws InterruptedException {

        String configPath = "scenarios/hanover/config.xml";
        String freetext = "test"; //short string to identify run character
        int iterations = 10;
        int scalefactor = 10;    //percent of population
        String datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd_HHmm"));
        String cadyts = "noCadyts";

        String outputPath = "output/" + datetime + "_" + freetext + "_iter" + iterations + "_scale" + scalefactor + "_" + cadyts;


//		configPath = "./input/config_noModeInnovation.xml";
//		outputPath = "./output/";

//		configPath = args[0];
//		outputPath = args[1];
//		String cadyts = args[2];


        if (cadyts.equals("yesCadyts")) {
            runWithCadyts(configPath, outputPath, scalefactor);
        } else {
            runWithoutCadyts(configPath, outputPath, scalefactor);
        }
    }

    private static void runWithCadyts(String configPath, String outputPath, int scalefactor) {
        final Config config = ConfigUtils.loadConfig(configPath, new CadytsConfigGroup(), new SwissRailRaptorConfigGroup());

        config.controler().setOutputDirectory(outputPath);

        config.qsim().setNumberOfThreads(4);
        config.global().setNumberOfThreads(8);
        config.parallelEventHandling().setNumberOfThreads(2);

        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);

        config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride); // TODO: ?
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
        config.plansCalcRoute().removeModeRoutingParams("undefined");

        config.plansCalcRoute().setAccessEgressType(AccessEgressType.walkConstantTimeToLink);
        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.qsim().setUsePersonIdForMissingVehicleId(false);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.qsim().setUsePersonIdForMissingVehicleId(true);

        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        final Controler controler = new Controler(scenario);
        final Network network = scenario.getNetwork();
        final double flowCapacity = config.qsim().getFlowCapFactor();
        adjustPtNetworkCapacity(network, flowCapacity);

        //Hinzufügen des Kalibrierungsmoduls Cadyts
        controler.addOverridingModule(new CadytsCarModule());
        //Hinzufügen des PT-Moduls
        controler.addOverridingModule(new SwissRailRaptorModule());

        controler.getConfig().plansCalcRoute().getModeRoutingParams().remove("ride");

        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            CadytsContext cadytsContext;
            @Inject
            ScoringParametersForPerson parameters;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                final ScoringParameters params = parameters.getScoringParameters(person);
                SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
                scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta());
                scoringFunctionAccumulator.addScoringFunction(scoringFunction);

                return scoringFunctionAccumulator;
            }
        });

        // tell the system to use the congested car router for the ride mode:
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
                addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
            }
        });

        //Erg�nzen der Aktivit�ten mit Dauerabh�ngigen Zus�tzen
        for (long ii = 600; ii <= 97200; ii += 600) {
            ActivityParams ap = new ActivityParams("home_" + ii);
            ap.setTypicalDuration(ii);
            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("home_" + ii + "_" + ii);
            ap.setTypicalDuration(ii);
            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("work_" + ii);
            ap.setTypicalDuration(ii);
            ap.setOpeningTime(6. * 3600);
            ap.setClosingTime(22. * 3600);
            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("education_" + ii);
            ap.setTypicalDuration(ii);
            ap.setOpeningTime(7. * 3600);
            ap.setClosingTime(20. * 3600);
            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("leisure_" + ii);
            ap.setTypicalDuration(ii);
            ap.setOpeningTime(6. * 3600);
            ap.setClosingTime(22. * 3600);
            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("shopping_" + ii);
            ap.setTypicalDuration(ii);
            ap.setOpeningTime(8. * 3600);
            ap.setClosingTime(22. * 3600);
            config.planCalcScore().addActivityParams(ap);
        }
        adaptPTVehicleSize(controler.getScenario().getTransitVehicles(), scalefactor);

        controler.run();
    }

    private static void runWithoutCadyts(String configPath, String outputPath, int scalefactor) {
        final Config config = ConfigUtils.loadConfig(configPath, new SwissRailRaptorConfigGroup());
        config.controler().setOutputDirectory(outputPath);

        config.qsim().setNumberOfThreads(8);
        config.global().setNumberOfThreads(8);
        config.parallelEventHandling().setNumberOfThreads(1);

        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);

        config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride); //TODO:?
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
        config.plansCalcRoute().removeModeRoutingParams("undefined");

        config.plansCalcRoute().setAccessEgressType(AccessEgressType.accessEgressModeToLink);
        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.qsim().setUsePersonIdForMissingVehicleId(false);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.qsim().setUsePersonIdForMissingVehicleId(false);

        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        final Controler controler = new Controler(scenario);
        final Network network = scenario.getNetwork();
        final double flowCapacity = config.qsim().getFlowCapFactor();
        adjustPtNetworkCapacity(network, flowCapacity);
        new MultimodalNetworkCleaner(network).run(Set.of("car", "ride"));

        //Hinzuf�gen des PT-Moduls
        controler.addOverridingModule(new SwissRailRaptorModule());

        controler.getConfig().plansCalcRoute().getModeRoutingParams().remove("ride");

        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            ScoringParametersForPerson parameters;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                final ScoringParameters params = parameters.getScoringParameters(person);
                SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                return scoringFunctionAccumulator;
            }
        });

        // tell the system to use the congested car router for the ride mode:
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
                addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
            }
        });

        //Erg�nzen der Aktivit�ten mit Dauerabh�ngigen Zus�tzen
        for (long ii = 600; ii <= 97200; ii += 600) {
            ActivityParams ap = new ActivityParams("home_" + ii);
            ap.setTypicalDuration(ii);
            config.planCalcScore().addActivityParams(ap);

//            ap = new ActivityParams("home_" + ii + "_" + ii);
//            ap.setTypicalDuration(ii);
//            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("work_" + ii);
            ap.setTypicalDuration(ii);
            ap.setOpeningTime(6. * 3600);
            ap.setClosingTime(22. * 3600);
            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("education_" + ii);
            ap.setTypicalDuration(ii);
            ap.setOpeningTime(7. * 3600);
            ap.setClosingTime(20. * 3600);
            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("leisure_" + ii);
            ap.setTypicalDuration(ii);
            ap.setOpeningTime(6. * 3600);
            ap.setClosingTime(22. * 3600);
            config.planCalcScore().addActivityParams(ap);

            ap = new ActivityParams("shopping_" + ii);
            ap.setTypicalDuration(ii);
            ap.setOpeningTime(8. * 3600);
            ap.setClosingTime(22. * 3600);
            config.planCalcScore().addActivityParams(ap);
        }
        adaptPTVehicleSize(controler.getScenario().getTransitVehicles(), scalefactor);

        controler.run();
    }

    /**
     * this is useful for pt links when only a fraction of the population is
     * simulated, but bus frequency remains the same. Otherwise, pt vehicles may get
     * stuck.
     */
    private static void adjustPtNetworkCapacity(Network network, double flowCapacityFactor) {
        if (flowCapacityFactor < 1.0) {
            for (Link l : network.getLinks().values()) {
                if (l.getAllowedModes().contains(TransportMode.pt)) {
                    l.setCapacity(l.getCapacity() / flowCapacityFactor);
                }
            }
        }
    }

    private static void adaptPTVehicleSize(Vehicles TransitVehicles, int ScaleFactor) {
        for (VehicleType type : TransitVehicles.getVehicleTypes().values()) {
            int seats = type.getCapacity().getSeats();
            int standingroom = type.getCapacity().getStandingRoom();
            type.getCapacity().setSeats(seats / ScaleFactor);
            type.getCapacity().setStandingRoom(standingroom / ScaleFactor);
        }
    }

}
