package MDVRPTW;


import Common.Customer;
import Common.Depot;
import Common.Place;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

public class MDVRPTWplaces {
    private ArrayList<Depot> depots;
    private ArrayList<Customer> customers;

    public MDVRPTWplaces(ArrayList<Depot> depots, ArrayList<Customer> customers) {
        this.depots = depots;
        this.customers = customers;
    }

    public MDVRPTWplaces(int depotsCount, int customersCount) {
        this.depots = new ArrayList<>(depotsCount);
        for (int i = 0; i < depotsCount; i++) {
            this.depots.add(new Depot(i+1, new Random()));
        }
        this.customers = new ArrayList<>();
        for (int i = 0; i < customersCount; i++) {
            this.customers.add(new Customer(i+1+depotsCount, new Random()));
        }
    }

    public Point2D.Double findMaxPoint() {
        double maxX = 0, maxY = 0;
        for (Customer c: customers) {
            if (c.getRealX() > maxX) maxX = c.getRealX();
            if (c.getRealY() > maxY) maxY = c.getRealY();
        }
        for (Depot d: depots) {
            if (d.getRealX() > maxX) maxX = d.getRealX();
            if (d.getRealY() > maxY) maxY = d.getRealY();
        }

        return new Point2D.Double(maxX, maxY);
    }

    public Point2D.Double findMinPoint() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        for (Customer c: customers) {
            if (c.getRealX() < minX) minX = c.getRealX();
            if (c.getRealY() < minY) minY = c.getRealY();
        }
        for (Depot d: depots) {
            if (d.getRealX() < minX) minX = d.getRealX();
            if (d.getRealY() < minY) minY = d.getRealY();
        }

        return new Point2D.Double(minX, minY);
    }

    public void transform(double scale, double xoff, double yoff) {
        for (int i = this.length(); --i >= 0; ) {
            this.getPlace(i).setDisplayX(this.getPlace(i).getRealX() * scale + xoff);
            this.getPlace(i).setDisplayY(this.getPlace(i).getRealY() * scale + yoff);
        }
        //this.bb = computeBB();
    }

    public void transform(Point2D.Double translation, double scale_factor, Point2D.Double minPoint) {
        for (int i = this.length(); --i >= 0; ) {
            this.getPlace(i).setDisplayX((this.getPlace(i).getRealX() - minPoint.getX()) * scale_factor + translation.getX());
            this.getPlace(i).setDisplayY((this.getPlace(i).getRealY() - minPoint.getY()) * scale_factor + translation.getY());
        }
    }

    public int length() {
        return customers.size() + depots.size();
    }

    public Place getPlace(int i) {
        if (i < this.depots.size())
            return this.depots.get(i);
        else
            return this.customers.get(i - this.depots.size());
    }

    public Depot getDepot(int i) {
        return this.depots.get(i);
    }

    public Customer getCustomer(int i) {
        return this.customers.get(i);
    }

    public int getDepotsCount() {
        return this.depots.size();
    }

    public int getCustomersCount() {
        return this.customers.size();
    }

    public ArrayList<Depot> getDepots() {
        return this.depots;
    }

    public ArrayList<Customer> getCustomers() {
        return this.customers;
    }

}
