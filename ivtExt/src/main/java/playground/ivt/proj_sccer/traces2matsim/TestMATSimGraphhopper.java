package playground.ivt.proj_sccer.traces2matsim;


import com.google.protobuf.TextFormat;
import com.graphhopper.GraphHopper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXFile;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Parameters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by molloyj on 23.10.2017.
 */
public class TestMATSimGraphhopper {
    //load a gpx trace in switzerland
    //map it to the network with graphhopper
    //convert to a matsim 'leg'
    //compare with the real result

    final private static String RUN_FOLDER = "P:\\Projekte\\SCCER\\zurich_1pc\\scenario\\";
    final private static String CONFIG_FILE = "defaultIVTConfig_w_emissions.xml"; // "defaultIVTConfig_w_emissions.xml";
    final private static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    private static GPXEntry readGPSLine(String l) {

        try {
            String[] pts = l.split(",");
            double lon = Double.parseDouble(pts[4]); //0
            double lat = Double.parseDouble(pts[5]); //1
            long time = formatter.parse(pts[10+2]).getTime();
            return new GPXEntry(lat, lon, time);
        } catch (ParseException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    public static void main(String[] args) {

        Config config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE);
        config.controler().setOutputDirectory(RUN_FOLDER + "output\\");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // import matsim network data
        GraphHopper hopper = new GraphHopperMATSim(scenario.getNetwork(), new CH1903LV03PlustoWGS84());
        
        hopper.setStoreOnFlush(false)
                .setGraphHopperLocation(new File("").getAbsolutePath())
                .setDataReaderFile(config.network().getInputFile());
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

// create MapMatching object, can and should be shared accross threads
        String algorithm = Parameters.Algorithms.DIJKSTRA_BI;
        Weighting weighting = new FastestWeighting(encoder);
        AlgorithmOptions algoOptions = new AlgorithmOptions(algorithm, weighting);
        MapMatching mapMatching = new MapMatching(hopper, algoOptions);

// do the actual matching, get the GPX entries from a file or via stream
        List<GPXEntry> entries = null;
        try {
         entries =  Files.lines(Paths.get("C:\\Users\\molloyj\\Documents\\SCCER\\gps\\ch_lvs_plus_test.csv"))
                 .skip(1)
                 .map(TestMATSimGraphhopper::readGPSLine).collect(Collectors.toList());
         System.out.println("total entries: " + entries.size());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        MatchResult mr = mapMatching.doWork(entries);

// return GraphHopper edges with all associated GPX entries
        List<EdgeMatch> matches = mr.getEdgeMatches();
// now do something with the edges like storing the edgeIds or doing fetchWayGeometry etc
        String result = mr.getEdgeMatches()
                .stream()
                .map(e -> e.getEdgeState().getEdge())
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        System.out.println(result);

        assert(result.equals("48194,48196,48198,48200,4380,4304,4306,4308,4385,26150,4353,4355,4378,4382,4371,4359,4361,26016,26018,26020,116099,116102,116104,231524,128028,128046,60693,60694,60698,287323,287325,170734,170742,11179,11181,11183,161619,11161,11163,11165,60696,11169,289958,115429,115430,60701,11199,158800,158810,11191,11193,4387,143565,143561,143563,8896,8898,8900,8902,86351,11173,11175,11177,11157,11159,103826,103828,103830,103832,103834,21375,21381,204999,205000,205001,204997,86297,86300,86302,86293,86295,86353,281430,281432,281434,281436,281439,11201,11203,23534,23536,23652,23654,23656,23659,263162,263163,18777,34000,37874,37872,277442,277444,277446,290366,290369,290371,290373,205846,125258,125260,205887,125256,22665,24590,24593,24595,24982,24984,24986,24988,87470,117501,82513,236601,236603,94211,82509,82511,25900,25902,25904,25906,25908,47157,47159,47161,47163,47165,47167,47169,47171,47173,47175,47177,47179,25916,25910,25912,25914,51283,51287,51289,51297,271361,271363,37045,37047,37049,37027,37029,37031,37033,37035,37037,37039,37041,37043,86562,271377,271365,271379,26553,26555,105552,181778,294693,294696,60721,60722,60723,60725,60728,47369,47370,93795,93791,93796,157032,86564,161735,161732,95466,134296,117099,168840,168842,77585,77581,77583,143439,143435,115467,151577,48216,143429,143431,143433,38926,38924,143443,143437,168838,231832,234092,234094,105901,105903,77295,105829,61062,61060,61058,61056,146927,104321,104297,235428,235426,235424,235422,235420,165673,61822,61824,14153,14130,61820,14156,142947,142944,142942,142940,142938,142936,142933,16567,249502,151584,93799,249500,111264,249498,111255,111253,111251,111249,111247,111282,111272,111261,111259,151576,111204,111202,111274,111206,142989,142987,142985,142983,111280,111262,111257,151574,151572,151570,151568,151566,23191,23189,23181,71517,71515,71512,71510,223336,223337,111235,111233,111231,111229,71508,111219,111217,111215,111213,111211,111209,111284,197851,151563,151580,197861,197859,197857,197849,87598,87596,87594,87592,128602,87719,87612,87618,165676,87601,287298,87714,87604,87670,87610,124698,124699,99147,99148,124701,124718,124696,151685,151686,151687,45483,45484,140007,140008,281265,281274,115740,115742,115743,97541,45482,63458,98194,261492,261491,45092,45093,45094,45095,45096,145532,145534,91348,276512,228177,228178,228179,228180,228181,228182,228183,228184,228185,228186,228187,49355,49357,49358,49359,49360,148676,148675,88805,101842,101843,101839,101840,88803,259940,259941,248612,248613,302136,302112,168835,48223,48224,28673,28675,28676,259851,259850,168869,168870,168871,168872,157695,157974,168849,145870,161733,117098,117100,86566,157033,161741,161742,117102,93495,93496,168850,157693,158014,158015,168833,168834,68520,259852,28678,28674,168836,88806,302137,302114,302115,259942,248611,213533,125678,125679,213553,168848,168847,125677,181264,250574,250575,250569,250570,250571,250572,247303,125652,125651,36226,125680,259941,248612,248613,302136,302112,168835,48223,48224,28673,28675,28676,259851,259850,168869,168870,168871,168872,157695,157772,93497,175111,117092,117093,117094,117095,157688,117103,117104,117105,117106,157547,157767,157732,157534,157535,157536,157537,158162,109530,158086,157538,158065,20514,20523,158078,20540,20541,20544,282724,123166,123168,61495,61492,61493,49334,158146,157623,157800,48225,48226,48228,48229,20288,20289,193906,193907,157581,157825,157826,157712,158026,158034,19995,19993,19994,19792,19793,19794,157639,157783,157690,157789,43986,43996,158087,158006,158007,157558,157790,43138,43147,43148,157612,157743,20014,20015,20016,20017,20018,20019,20020,20021,20022,20023,20024,20025,20026,19920,19909,19910,19911,19912,100160,100165,100161,100183,4302,4300,236086,4338,4340,4342,128369,4331,4333,4310,4312,4314\n"));
    }
}
