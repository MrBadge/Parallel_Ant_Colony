package Common;

import java.util.ArrayList;
import java.util.Random;

public class Vehicle {
    private double max_capacity;
    private double capacity;
    private double default_max_capacity = Utilities.randInt(new Random(), 150, 200);
    //    private int max_travel_time;
    protected double time_interval_start = 0;
    protected double time_interval_end = 200;
    private double current_time;

    public void setCurrentCapacity(double c) {
        this.capacity = c;
    }

    public void addLoad(double c) {
        this.capacity += c;
    }

    public void addTime(double time) {
        this.current_time += time;
    }

    public Vehicle() {
        this.max_capacity = default_max_capacity;
        this.capacity = 0;
//        this.max_travel_time = Integer.MAX_VALUE;
    }

    public Vehicle(double max_c, double tis, double tie) {
        this.max_capacity = max_c;
        this.capacity = 0;
        this.time_interval_start = tis;
        this.time_interval_end = tie;
//        this.max_travel_time = max_travel_time;
    }

    public static ArrayList<Vehicle> generateDefaultVehicles(int vCount, double max_c) {
        ArrayList<Vehicle> tmp = new ArrayList<>(vCount);
        for (int i = 0; i < vCount; i++) {
            if (max_c != 0)
                tmp.add(new Vehicle(max_c, 0, 230));
            else
                tmp.add(new Vehicle());
        }
        return tmp;
    }

    public static ArrayList<Vehicle> generateDefaultVehicles(int vCount) {
        return generateDefaultVehicles(vCount, 0);
    }

    public boolean isFull() {
        return this.capacity >= this.max_capacity;
    }

    public double getMaxCapacity() {
        return this.max_capacity;
    }

    public double getCurrentCapacity() {
        return this.capacity;
    }

    public void setCurrentTime(double time) {
        this.current_time = time;
    }

    public double getCurrentTime() {
        return this.current_time;
    }

    public double getTIS() {
        return time_interval_start;
    }

    public double getTIE() {
        return time_interval_end;
    }

}
