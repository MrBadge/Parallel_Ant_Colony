package Common;

import java.util.ArrayList;
import java.util.Random;

public class Depot extends Place {
    protected double totalDemand = 0;
    protected ArrayList<Vehicle> vehicles;

    public Depot(int n, double xs, double ys, double tis, double tie, double st, ArrayList<Vehicle> v) {
        super(n, xs, ys, 0, tis, tie, st);
        this.vehicles = v;
    }

    public Depot(int n, String address, double tis, double tie, double st, ArrayList<Vehicle> v) {
        super(n, address, 0, tis, tie, st);
        this.vehicles = v;
    }

    public Depot(int n, Random rand, ArrayList<Vehicle> v) {
        super(n, rand.nextDouble(), rand.nextDouble(), 0, -1, -1, 0);
        this.vehicles = v;
    }

    public Depot(int n, Random rand) {
        super(n, rand.nextDouble(), rand.nextDouble(), 0, 0, 230, 0);
        this.vehicles = Vehicle.generateDefaultVehicles(rand.nextInt(20) + 1);
    }

    public int getVehiclesCount() {
        return vehicles.size();
    }

    public ArrayList<Vehicle> getVehicles() {
        return this.vehicles;
    }

    public double getTotalDemand() {
        if (totalDemand == 0) {
            double tmp = 0;
            for (Vehicle v: vehicles){
                tmp += v.getMaxCapacity();
            }
            totalDemand = tmp;
        }
        return totalDemand;
//        return vehicle.getMaxCapacity();
    }
}
