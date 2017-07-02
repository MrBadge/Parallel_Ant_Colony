package Utils;

/**
 * Created by anatolymaltsev on 6/1/16.
 */

import Common.Customer;
import Common.Depot;
import Common.Vehicle;
import MDVRPTW.MDVRPTW;

import java.util.ArrayList;
import java.util.Objects;

public class Parser {
    public static MDVRPTW parseMDVRPTW(String data) {
        ArrayList<Depot> depots = new ArrayList<>();
        ArrayList<Customer> customers = new ArrayList<>();
        int vehicleLinesLeft = 0;
        String[] depotLine = new String[0];
        ArrayList<Vehicle> vehiclesAcc = new ArrayList<>();
        for (String line: data.split("\n")) {
            if (line.startsWith("#") || Objects.equals(line, "")) {
                continue;
            }
            String[] parts = line.split("(( |\t)+;( |\t)+)");
            if (vehicleLinesLeft > 0) {
                vehiclesAcc.add(new Vehicle(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                if (--vehicleLinesLeft == 0) {
                    depots.add(new Depot(Integer.parseInt(depotLine[0]), depotLine[1], Double.parseDouble(depotLine[3]),
                            Double.parseDouble(depotLine[4]), Double.parseDouble(depotLine[5]), new ArrayList<>(vehiclesAcc)));
                }
                continue;
            }
            if (parts.length <= 7) {
                depotLine = parts;
                vehicleLinesLeft = Integer.parseInt(parts[6]);
                vehiclesAcc.clear();
            } else {
                customers.add(new Customer(Integer.parseInt(parts[0]), parts[1],
                        Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                        Double.parseDouble(parts[5]), parts[6].equals("1"), Double.parseDouble(parts[7])));
            }
        }

        return new MDVRPTW(depots, customers);
    }
}
