//package keep;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.function.ToDoubleFunction;
//
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.core.config.Config;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.misc.Counter;
//import org.matsim.facilities.ActivityFacility;
//import org.matsim.facilities.MatsimFacilitiesReader;
//
//import ch.sbb.matsim.analysis.skims.CalculateSkimMatrices;
//import ch.sbb.matsim.analysis.skims.StreamingFacilities;
//
//import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
////iterativ
//
//public class RunTTMatrix {
//
//	public static void main(String[] args) throws IOException {
//
//		String zonesShapeFilename = "T:/bienzeisler/USEfUL-XT/matsim-hanover/01_MATSimModelCreator/res/shape/RH_useful__zone.SHP";
//		String networkFilename = "T:/bienzeisler/USEfUL-XT/matsim-hanover/03_RunSimulation/output_Run_new_2/hanover-v1.0-10pct-calibration.output_network.xml.gz";
//		String eventsFilename = "T:/bienzeisler/USEfUL-XT/matsim-hanover/03_RunSimulation/output_Run_new_2/hanover-v1.0-10pct-calibration.output_events.xml.gz";
//		String scheduleFilename = "T:/bienzeisler/USEfUL-XT/matsim-hanover/03_RunSimulation/output_Run_new_2/hanover-v1.0-10pct-calibration.output_transitSchedule.xml.gz";
//
//		String outputDirectory = "T:/bienzeisler/USEfUL-XT/matsim-hanover/01_MATSimModelCreator/output/ttmatrix_neu_pt/";
//        String zonesIdAttributeName = "NAME";
//
//        int numberOfPointsPerZone = 5;
//
//        Random r = new Random();
//
//        String mode = "car";
//
//        Config config = new Config();
//        config.addCoreModules();
//
//        CalculateSkimMatrices skims = new CalculateSkimMatrices( outputDirectory, 8);
//
//        skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, numberOfPointsPerZone, zonesShapeFilename, zonesIdAttributeName,r);
//
//
//        for (int i = 0; i <= 23; i++) {
//        	int min = i;
//        	int max = i + 1;
//            double[] timesCar = new double[]{min * 3600, max * 3600};
//            skims.calculateAndWriteNetworkMatrices(networkFilename, eventsFilename, timesCar, config, "hour_Min_" + min + "_Max " + max, link -> true);
////            skims.calculatePTMatrices(networkFilename, scheduleFilename, timesCar[0], timesCar[1], config, null, (line, route) -> route.getTransportMode().equals("train"));
//            skims.calculateAndWritePTMatrices(networkFilename, scheduleFilename, timesCar[0], timesCar[1], config, "pt_hour_Min_" + min + "_Max ", (line, route) -> route.getTransportMode().equals("train"));
//        }
//
//
////        skims.calculatePTMatrices(networkFilename, transitScheduleFilename, earliestTime, latestTime, config, null, (line, route) -> route.getTransportMode().equals("train"));
////        skims.calculateBeelineMatrix();
//	}
//
//
//    private static class WeightedCoord {
//
//        Coord coord;
//        double weight;
//
//        private WeightedCoord(Coord coord, double weight) {
//            this.coord = coord;
//            this.weight = weight;
//        }
//    }
//
//}



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

import ch.sbb.matsim.analysis.skims.CalculateSkimMatrices;
import ch.sbb.matsim.analysis.skims.StreamingFacilities;

import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;

public class RunTTMatrix {

    // Define file paths and other parameters as constants
    private static final String ZONES_SHAPE_FILENAME = "path/to/zonesShapeFile";
    private static final String NETWORK_FILENAME = "D:\\_MA uncloud Backup\\01_MATSimModelCreator\\res\\network\\network.xml";
    private static final String EVENTS_FILENAME = "path/to/eventsFile";
    private static final String SCHEDULE_FILENAME = "path/to/scheduleFile";
    private static final String OUTPUT_DIRECTORY = "path/to/outputDirectory";
    private static final String ZONES_ID_ATTRIBUTE_NAME = "NAME";
    private static final int NUMBER_OF_POINTS_PER_ZONE = 5;
    private static final String MODE = "car";

    public static void main(String[] args) {
        try {
            runTTMatrix();
        } catch (IOException e) {
            System.err.println("An error occurred while running the TT Matrix: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runTTMatrix() throws IOException {
        Random r = new Random();
        Config config = new Config();
        config.addCoreModules();

        CalculateSkimMatrices skims = new CalculateSkimMatrices(OUTPUT_DIRECTORY, 8);

        skims.calculateSamplingPointsPerZoneFromNetwork(NETWORK_FILENAME, NUMBER_OF_POINTS_PER_ZONE, ZONES_SHAPE_FILENAME, ZONES_ID_ATTRIBUTE_NAME, r);

        for (int i = 0; i <= 23; i++) {
            int min = i;
            int max = i + 1;
            double[] timesCar = new double[]{min * 3600, max * 3600};
            skims.calculateAndWriteNetworkMatrices(NETWORK_FILENAME, EVENTS_FILENAME, timesCar, config, "hour_Min_" + min + "_Max " + max, link -> true);
            skims.calculateAndWritePTMatrices(NETWORK_FILENAME, SCHEDULE_FILENAME, timesCar[0], timesCar[1], config, "pt_hour_Min_" + min + "_Max ", (line, route) -> route.getTransportMode().equals("train"));
        }
    }
}