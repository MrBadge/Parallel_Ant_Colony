package API;

import ACOSolver.AntColony;
import Common.Place;
import Common.Vehicle;
import Helpers.Tuple;
import SDVRPTW.SDVRPTW;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.util.ServerRunner;

import java.io.*;
import java.util.*;

import Utils.Parser;
import MDVRPTW.MDVRPTW;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class API extends NanoHTTPD {
    public API() {
        super(8080);
    }

    public static void main(String[] args) {
        ServerRunner.run(API.class);
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        try {
            session.parseBody(new HashMap<String, String>());
        } catch (IOException | ResponseException e) {
            e.printStackTrace();
        }

        String task_data = session.getParms().get("data");
        MDVRPTW task = Parser.parseMDVRPTW(task_data);

        InputStream config;
        Properties settings = new Properties();
        try {
            config = new FileInputStream("config");
            settings.load(config);
        } catch (IOException ex) {
            settings.setProperty("pheromone_exp", "1");
            settings.setProperty("distance_exp", "0.2");
            settings.setProperty("f_exp", "0.4");
            settings.setProperty("w_exp", "0.8");
            settings.setProperty("savings_exp", "0.9");
            settings.setProperty("savings_heuristic_f_coeff", "2");
            settings.setProperty("savings_heuristic_g_coeff", "2");
            settings.setProperty("f_heuristic_delta_coeff", "2");
            try {
                OutputStream output = new FileOutputStream("config");
                settings.store(output, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        ArrayList<AntColony> antColonies = new ArrayList<>();
        for (SDVRPTW sdvrptw : task.getVrpSubProblems()) {
            AntColony tmp = new AntColony(sdvrptw, settings);
            tmp.init(0);
            tmp.setExploit(0.75);
            tmp.setTrail(0.1);
            tmp.setEvap(1);
            antColonies.add(tmp);
        }

        System.out.println("Calculating...");
        for (int i = 0; i <= 50000; i++) {
            for (AntColony ac : antColonies) {
                ac.runEpoch();
                if (i % 5000 == 0) {
                    System.out.print(String.valueOf(i) + "... (best run: " + String.valueOf(ac.getBestTourLen()) + ") ");
                }
            }
        }
        System.out.println("");

        JSONArray result = new JSONArray();
        for (AntColony ac : antColonies) {
            JSONObject sdwrp_solution = new JSONObject();

            Map<Integer, Tuple<Vehicle, ArrayList<Place>>> routes = ac.getBestSolution();
            JSONArray sub_routes = new JSONArray();
            for (Tuple<Vehicle, ArrayList<Place>> value : routes.values()) {
                JSONArray sub_route = new JSONArray();
                for (Place p : value.getRight()) {
                    HashMap<String, String> place_info = new HashMap<>();
                    place_info.put("address", p.getAddress());
                    place_info.put("at", String.format("%.0f", p.getArrivalTime()));
                    place_info.put("st", String.format("%.0f", p.getST()));
                    place_info.put("demand", String.format("%.0f", p.getDemand()));
                    place_info.put("tis", String.format("%.0f", p.getTIS()));
                    place_info.put("tie", String.format("%.0f", p.getTIE()));
                    place_info.put("lat", String.format("%.10f", p.getLat()));
                    place_info.put("lng", String.format("%.10f", p.getLng()));
                    sub_route.add(place_info);
                }
                sub_routes.add(sub_route);
            }
            sdwrp_solution.put("routes", sub_routes);
            HashMap<String, String> totals = new HashMap<>();
            totals.put("distance", String.format("%.0f", ac.getBestTourLen()));
            totals.put("clients_visited", String.format("%d", ac.getClientsCountVisited()));
            sdwrp_solution.put("totals", totals);

            result.add(sdwrp_solution);
        }

        System.out.println(result.toString());
        return newFixedLengthResponse(result.toString());
    }
}
