package SDVRPTW;

import Common.Customer;
import Common.Depot;
import Common.Place;
import Common.Vehicle;

import java.util.ArrayList;
import java.util.Random;

public class SDVRPTWplaces {
    private Depot depot;
    private ArrayList<Customer> customers;
    private ArrayList<Customer> possible_customers;

    public Depot getDepot() {
        return this.depot;
    }

    public Customer getCustomer(int i) {
        return this.customers.get(i - 1);
    }

    public Place getPlace(int i) {
        if (i == 0)
            return this.depot;
        else
            return this.customers.get(i - 1);
    }

    public ArrayList<Customer> getCustomers() {
        return this.customers;
    }
    public ArrayList<Customer> getPossible_customers() {
        return this.possible_customers;
    }

    public SDVRPTWplaces(int n, Depot d, ArrayList<Customer> c) {
        this.depot = d;
        this.customers = c;
        this.possible_customers = new ArrayList<>();

        for (Customer aC : this.customers) {
            if (!aC.isReal()) {
                this.possible_customers.add(aC);
            }
        }
    }

    public SDVRPTWplaces(int custCount){
        this.depot = new Depot(1, new Random());
        this.customers = new ArrayList<>(custCount);
        this.possible_customers = new ArrayList<>();
        for (int i = 0; i < custCount; i++) {
            this.customers.add(i, new Customer(i+1, new Random()));
        }
    }

    public SDVRPTWplaces(Random rand) {
        this.depot = new Depot(1, rand);
        int cCount = 4;//rand.nextInt(10) + 2;
        this.customers = new ArrayList<>(cCount);
        this.possible_customers = new ArrayList<>();
        for (int i = 0; i < cCount; i++) {
            this.customers.add(i, new Customer(i+1, rand));
        }
    }

    public int getVerticesCount() {
        return this.customers.size() + 1;
    }

    public int getPlaceIndex(Place p) {
        for (int i = 0; i < this.customers.size(); i++) {
            Place _p = this.customers.get(i);
            if (_p == p) {
                return i + 1; // + 1 cause of the depot on the first position
            }
        }
        return -1;
    }

}
