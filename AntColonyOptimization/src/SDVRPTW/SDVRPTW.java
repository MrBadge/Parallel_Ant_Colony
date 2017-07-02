package SDVRPTW;


import Common.Customer;
import Common.Depot;
import Common.Place;
import Common.Pair;

import java.util.ArrayList;
import java.util.Random;

public class SDVRPTW {

    private SDVRPTWplaces places;
    private double[][] distance;
    private double[][] travel_time;

    public int getTaskSize() {
        return places.getVerticesCount();
    }

    public SDVRPTW(int cCount, int vCount) {
        this.places = new SDVRPTWplaces(cCount);
        this.distance = new double[getTaskSize()][getTaskSize()];
        this.travel_time = new double[getTaskSize()][getTaskSize()];
        distInit();
    }

    public SDVRPTW(int n, Depot d, ArrayList<Customer> c) {
        this.places = new SDVRPTWplaces(n, d, c);
        this.distance = new double[getTaskSize()][getTaskSize()];
        this.travel_time = new double[getTaskSize()][getTaskSize()];
        distInit();
    }

    public SDVRPTW() {
        this.places = new SDVRPTWplaces(new Random());
        this.distance = new double[getTaskSize()][getTaskSize()];
        this.travel_time = new double[getTaskSize()][getTaskSize()];
        distInit();
    }

    private void distInit() {
        int size = getTaskSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j <= i; j++) {
                Pair<Double, Double> resIJ = Place.getDistAndTimeBetweenPlaces(places.getPlace(i), places.getPlace(j));
                this.distance[i][j] = resIJ.fst;
                this.travel_time[i][j] = resIJ.snd;
                Pair<Double, Double> resJI = Place.getDistAndTimeBetweenPlaces(places.getPlace(j), places.getPlace(i));
                this.distance[j][i] = resJI.fst;
                this.travel_time[j][i] = resJI.snd;
            }
        }
    }

    public double getDistBetween(int i, int j) {
        return this.distance[i][j];
    }

    public double getTimeBetween(int i, int j) {
        return this.travel_time[i][j];
    }

    public Depot depo() {
        return places.getDepot();
    }

    public Customer getCustomer(int i) {
        return places.getCustomer(i);
    }

    public Place getPlace(int i) {
        return places.getPlace(i);
    }

    public SDVRPTWplaces places() {
        return this.places;
    }

}
