package MDVRPTW;


import Common.*;
import SDVRPTW.SDVRPTW;

import java.util.*;
public class MDVRPTW {

    private ArrayList<SDVRPTW> vrpSubProblems;
    private MDVRPTWplaces places;

    public ArrayList<SDVRPTW> getVrpSubProblems() {
        return vrpSubProblems;
    }

    public MDVRPTWplaces getPlaces() {
        return this.places;
    }

    public Depot getDepot(int i) {
        return this.places.getDepot(i);
    }

    public Customer getCustomer(int i) {
        return this.places.getCustomer(i);
    }

    public int getVerticesCount() {
        return this.places.length();
    }

    public Place getPlace(int i) {
        return this.places.getPlace(i);
    }

    public int getClientsCount() {
        return this.places.getCustomersCount();
    }

    private boolean isDepotFull(Depot d, ArrayList<Customer> c/*, Customer new_c*/){
        double sumDemand = 0;
        for (Customer customer: c){
            sumDemand += customer.getDemand();
        }
        return d.getTotalDemand() <= sumDemand - sumDemand*0.05/* + new_c.getDemand()*/;
    }

    private ArrayList<Double> getSortedDistsToDepots(Customer c, ArrayList<Depot> depots) {
        ArrayList<Double> tmp = new ArrayList<>(depots.size());
        for (Depot d: depots) {
            tmp.add(Place.getDistAndTimeBetweenPlaces(c, d).fst);
        }
        Collections.sort(tmp);
        return tmp;
    }

    private HashMap<Customer, Double> buildUrgencyArray(ArrayList<Customer> na_customers, ArrayList<Depot> dns_depots) {
        HashMap<Customer, Double> tmp = new HashMap<>();
        for (Customer c: na_customers) {
            ArrayList<Double> dists = getSortedDistsToDepots(c, dns_depots);
            if (dists.size() > 1)
                tmp.put(c, dists.get(1)-dists.get(0));
            else
                tmp.put(c, dists.get(0));
        }
        return tmp;
    }

    private Depot findNearestDepot(Customer c, ArrayList<Depot> dns_depots) {
        Depot nearest_depot = dns_depots.get(0);
        Double min_dist = Place.getDistAndTimeBetweenPlaces(c, nearest_depot).fst;
        for (Depot d: dns_depots) {
            Double dist = Place.getDistAndTimeBetweenPlaces(c, d).fst;
            if (dist < min_dist) {
                nearest_depot = d;
                min_dist = dist;
            }
        }
        return nearest_depot;
    }

    private ArrayList<SDVRPTW> divideIntoSubProblems(ArrayList<Customer> customers, ArrayList<Depot> depots) {
        ArrayList<SDVRPTW> sdvrptwList = new ArrayList<>();
        if (depots.size() == 1) {
            sdvrptwList.add(new SDVRPTW(1, depots.get(0), customers));
            return sdvrptwList;
        }
        ArrayList<Customer> NA_customers = new ArrayList<>(customers);
        ArrayList<Depot> DNS_depots = new ArrayList<>(depots);
        HashMap<Depot, ArrayList<Customer>> subsets = new HashMap<>();
        for (Depot depot : depots) {
            subsets.put(depot, new ArrayList<Customer>());
        }

        while (!NA_customers.isEmpty() /*&& !(DNS_depots.size()==0)*/) {
            HashMap<Customer, Double> urgencyArray = buildUrgencyArray(NA_customers, DNS_depots);
            Customer mostUrgentCustomer = null;
            Double minValue = Double.MAX_VALUE;
            for (Map.Entry<Customer, Double> entry : urgencyArray.entrySet()) {
                if (minValue > entry.getValue()) {
                    minValue = entry.getValue();
                    mostUrgentCustomer = entry.getKey();
                }
            }
            //if mostUrgentCustomer == null - smth is wrong
            Depot nearestDepot = findNearestDepot(mostUrgentCustomer, DNS_depots);
            ArrayList<Customer> listOfCustomersForNearestDepot = subsets.get(nearestDepot);
            listOfCustomersForNearestDepot.add(mostUrgentCustomer);
            subsets.put(nearestDepot, listOfCustomersForNearestDepot);
            NA_customers.remove(mostUrgentCustomer);
//            if (isDepotFull(nearestDepot, listOfCustomersForNearestDepot)) {
//                DNS_depots.remove(nearestDepot);
//            }
            //Take the most urgent customer and find its nearest depot. Assign them to each other.
            //Remove this customer from na_customers array. If depot capacity is exceeded, remove it from dns_depots array
        }
        int tmp_counter = 1;
        for (Map.Entry<Depot, ArrayList<Customer>> entry: subsets.entrySet()) {
            sdvrptwList.add(new SDVRPTW(tmp_counter++, entry.getKey(), entry.getValue()));
        }
        return sdvrptwList;
    }

    public MDVRPTW(int depotsCount, int customersCount) {
        this.places = new MDVRPTWplaces(depotsCount, customersCount);
        this.vrpSubProblems = divideIntoSubProblems(this.places.getCustomers(), this.places.getDepots());
    }

    public MDVRPTW(ArrayList<Depot> depots, ArrayList<Customer> customers) {
        this.places = new MDVRPTWplaces(depots, customers);
        this.vrpSubProblems = divideIntoSubProblems(this.places.getCustomers(), this.places.getDepots());
    }
}
