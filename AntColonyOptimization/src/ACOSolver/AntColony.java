package ACOSolver;

import Common.Customer;
import Common.Place;
import SDVRPTW.SDVRPTW;
import Common.Vehicle;

import Helpers.Tuple;

import java.awt.geom.Point2D;
import java.util.*;

public class AntColony {
    private SDVRPTW vrp;
    private int task_size;
    private Map<Integer, Tuple<Vehicle, ArrayList<Place>>> solution;
    private ArrayList<Vehicle> vehicles;
    private double evapFactor;
    private double pheromonLayExp;
    private double bestKnownEdgeUsageProbability;
    private double[][] pheromoneDelta;

    private double[][] nearnessDesirability;
    private double[][] pheromoneTrail;
    private double[][] savingsHeuristic;
    private int[] possibleClients;

    private double bestTourLength;
    private double averageTourLength;
    private Map<Integer, Tuple<Vehicle, ArrayList<Place>>> bestSolution;

    private int epoch;
    private int clientsVisited;
    private double maxTrailLevel;
    private double averageTrailLevel;
    private double totalFleetRunLen;
    private int clientsVisitedByFleet;

    private double pheromone_exp, distance_exp, f_exp, w_exp, savings_exp,
            savings_heuristic_f_coeff, savings_heuristic_g_coeff,
            f_heuristic_delta_coeff;

    private boolean[] visited;

    public boolean hasBetterSolution = false;

    private Random rand;

    public AntColony(SDVRPTW vrp, Properties settings) {
        this.vrp = vrp;
        this.task_size = vrp.getTaskSize();
        this.solution = new HashMap<>();
        this.pheromoneTrail = new double[task_size][task_size];
        this.visited = new boolean[task_size];
        this.pheromoneTrail = new double[task_size][task_size];

        this.nearnessDesirability = new double[task_size][task_size];
        this.savingsHeuristic = new double[task_size][task_size];

        this.possibleClients = new int[vrp.places().getPossible_customers().size()];
        int i = 0;
        for (Customer c : this.vrp.places().getCustomers()) {
            if (!c.isReal()) {
                this.possibleClients[i++] = this.vrp.places().getPlaceIndex(c);
                if (i == this.possibleClients.length) {
                    break;
                }
            }
        }

        this.pheromoneDelta = new double[task_size][task_size];
        this.vehicles = vrp.depo().getVehicles();

        this.bestKnownEdgeUsageProbability = 0.0;
        this.evapFactor = 0.1;
        this.pheromonLayExp = 1;

        this.rand = new Random();

        parseSettings(settings);
        init(-1);
    }

    private void parseSettings(Properties settings) {
        pheromone_exp = Double.parseDouble(settings.getProperty("pheromone_exp", "1"));
        distance_exp = Double.parseDouble(settings.getProperty("distance_exp", "0.2"));
        f_exp = Double.parseDouble(settings.getProperty("f_exp", "0.4"));
        w_exp = Double.parseDouble(settings.getProperty("w_exp", "0.8"));
        savings_exp = Double.parseDouble(settings.getProperty("savings_exp", "0.9"));

        savings_heuristic_f_coeff = Double.parseDouble(settings.getProperty("savings_heuristic_f_coeff", "2"));
        savings_heuristic_g_coeff = Double.parseDouble(settings.getProperty("savings_heuristic_g_coeff", "2"));

        f_heuristic_delta_coeff = Double.parseDouble(settings.getProperty("f_heuristic_delta_coeff", "0.0001"));
    }

    public void init(double val) {
        double sum = 0;

        for (int i = this.task_size; --i >= 0; )
            for (int j = this.task_size; --j >= 0; )
                sum += vrp.getDistBetween(i, j);
        this.averageTourLength = sum / this.task_size;

        if (val <= 0) val = 1;

        for (int i = this.task_size; --i >= 0; ) {
            for (int j = this.task_size; --j >= 0; ) {
                this.nearnessDesirability[i][j] = Math.pow(vrp.getDistBetween(i, j), -this.distance_exp);
                this.savingsHeuristic[i][j] = Math.pow(vrp.getDistBetween(i, 0) + vrp.getDistBetween(j, 0) -
                        savings_heuristic_g_coeff * vrp.getDistBetween(i, j) +
                        savings_heuristic_f_coeff * Math.abs(vrp.getDistBetween(i, 0) - vrp.getDistBetween(j, 0)), savings_exp);
                this.pheromoneTrail[i][j] = val;
            }
        }
        this.maxTrailLevel = this.averageTrailLevel = val;
        this.bestTourLength = Double.MAX_VALUE;
        this.epoch = 0;
        this.clientsVisited = 0;
    }

    private boolean allVisited() {
        for (boolean v : this.visited) {
            if (!v)
                return false;
        }
        return true;
    }

    private void newEpochInit() {
        for (int i = this.pheromoneDelta.length; --i >= 0; ) {
            for (int j = this.pheromoneDelta.length; --j >= 0; ) {
                this.pheromoneDelta[i][j] = 0;
            }
        }
        this.totalFleetRunLen = 0;
//        this.clientsVisited = 0;
        for (Vehicle v : vehicles) {
            v.setCurrentCapacity(0);
            v.setCurrentTime(v.getTIS());
        }
    }

    public Map<Integer, Tuple<Vehicle, ArrayList<Place>>> runEpoch() {

        newEpochInit();

        this.hasBetterSolution = false;

        ArrayList<Double> old_places_at = new ArrayList<>();
        for (Place p : this.vrp.places().getCustomers()) {
            old_places_at.add(p.getArrivalTime());
        }

        this.solution = runVehicles();
        globalPheromoneUpdate();
        int clients_visited_diff = this.clientsVisitedByFleet - this.clientsVisited;
        if (clients_visited_diff >= 0) { // if we have visited more or the same amount of clients
            if (clients_visited_diff == 0) {
                if (this.totalFleetRunLen < this.bestTourLength) {
                    this.hasBetterSolution = true;
                } else {
                    if (this.totalFleetRunLen <= this.bestTourLength * 1.05 && this.solution.size() < this.bestSolution.size()) // if less vehicles was used -> better solution
                        this.hasBetterSolution = true;
                }
            } else {
                this.hasBetterSolution = true; // if we have visited more clients, we definitely have better solution
            }
        }
        if (this.hasBetterSolution) {
            this.clientsVisited = this.clientsVisitedByFleet;
            this.bestTourLength = this.totalFleetRunLen;
            this.bestSolution = new HashMap<>(this.solution);
        } else {
            ArrayList<Customer> customers = this.vrp.places().getCustomers();
            for (int i = 0; i < customers.size(); i++) {
                Place p = customers.get(i);
                p.setArrivalTime(old_places_at.get(i));
            }
        }
        this.epoch++;
        return this.bestSolution;
    }

    private void placePheromone(ArrayList<Integer> tour, double amount) {
        int i;
        int src, dst;

        src = 0;
        for (i = tour.size(); --i >= 0; ) {
            dst = src;
            src = tour.get(i);
            this.pheromoneDelta[src][dst] += amount;
        }
    }

    private int chooseRandDest(double[] p) {
        double val = rand.nextDouble();
        double cumulativeProbability = 0.0;
        for (int i = 0; i < p.length; i++) {
            if (p[i] == 0)
                continue;
            cumulativeProbability += p[i];
            if (val <= cumulativeProbability)
                return i;
        }
        return p.length - 1;
    }

    private void globalPheromoneUpdate() {
        double minPheromoneAmount, newTrailVal;

        minPheromoneAmount = this.averageTrailLevel / this.vrp.getTaskSize();
        this.maxTrailLevel = 0;
        double stick = 1 - this.evapFactor;

        for (int i = this.pheromoneTrail.length; --i >= 0; ) {
            for (int j = i; --j >= 0; ) {
                newTrailVal = stick * this.pheromoneTrail[i][j]
                        + this.evapFactor * (this.pheromoneDelta[i][j] + this.pheromoneDelta[j][i]);
                if (newTrailVal < minPheromoneAmount)
                    newTrailVal = minPheromoneAmount;
                this.pheromoneTrail[i][j] =
                        this.pheromoneTrail[j][i] = newTrailVal;
                if (newTrailVal > this.maxTrailLevel)
                    this.maxTrailLevel = newTrailVal;
                this.averageTrailLevel += newTrailVal;
            }
        }
        this.averageTrailLevel /= 0.5 * this.vrp.getTaskSize() * this.vrp.getTaskSize();

    }

    private HashMap<Integer, Tuple<Vehicle, ArrayList<Place>>> runVehicles() {
        ArrayList<Place> subTour;
//        ArrayList<Vehicle> usedVehicles = new ArrayList<>();
        ArrayList<Vehicle> availableVehicles = new ArrayList<>(vehicles);
        HashMap<Integer, Tuple<Vehicle, ArrayList<Place>>> result = new HashMap<>();

        for (int i = 1; i < this.vrp.getTaskSize(); ++i) {
            this.visited[i] = !vrp.getCustomer(i).isReal();
        }

        this.totalFleetRunLen = 0;
        this.clientsVisitedByFleet = 0;

        int route_number = 0;
        while (!allVisited() && availableVehicles.size() != 0) {
            Vehicle curVehicle = availableVehicles.get(rand.nextInt(availableVehicles.size()));

            subTour = runVehicle(curVehicle);
            this.clientsVisitedByFleet += subTour.size() - 1;
            result.put(route_number++, new Tuple<>(curVehicle, subTour));
//            vehicles.setCurrentCapacity(0);
//            vehicles.setCurrentTime(vrp.depo().getTIS());
//            usedVehicles.add(curVehicle);
            availableVehicles.remove(curVehicle);
        }

        this.epoch++;

        return result;
    }

    private double[] calculateHeuristicF(int cur_place, double cur_time, Vehicle v) {
        double[] heuristic_f = new double[task_size];
        double[] F = new double[task_size];
        Arrays.fill(F, 0);
        double sum = 0;
        int count = 0;
        Place curPlace = vrp.getPlace(cur_place);
        for (int i = 0; i < task_size; i++) {
            F[i] = vrp.getPlace(i).getTIE() - Math.max(curPlace.getTIS(), cur_time) - curPlace.getST() - vrp.getTimeBetween(i, cur_place);
            if (F[i] >= 0) {
                sum += F[i];
                count++;
            }
        }
        double mu = sum / count;
        for (int i = 0; i < task_size; i++) {
            if (F[i] >= 0 || !isReachable(cur_place, i, v.getCurrentTime())) {
                heuristic_f[i] = 1 / (1 + Math.exp(f_heuristic_delta_coeff * (F[i] - mu)));
            } else {
                heuristic_f[i] = 0;
            }
        }
        return heuristic_f;
    }

    private double[] calculateHeuristicW(int cur_place, double cur_time, Vehicle v) {
        double[] heuristic_w = new double[task_size];
        double[] W = new double[task_size];
        Arrays.fill(W, 0);
        Place curPlace = vrp.getPlace(cur_place);
        for (int i = 0; i < task_size; i++) {
            W[i] = vrp.getPlace(i).getTIS() - cur_time - curPlace.getST() - vrp.getTimeBetween(i, cur_place);
            if (W[i] == 0) {
                W[i] = 1;
            }
        }
        for (int i = 0; i < task_size; i++) {
            if (isReachable(cur_place, i, v.getCurrentTime()))
                heuristic_w[i] = 1 / Math.abs(W[i]);
            else
                heuristic_w[i] = 0;
        }
        return heuristic_w;
    }

    private boolean isReachable(int from, int to, double v_cur_time) {
        return (v_cur_time + vrp.getTimeBetween(from, to) /*+ vrp.getPlace(to).getST()*/ <= vrp.getPlace(to).getTIE());
    }

    private boolean hasReachableSummits(int cur_place, Vehicle v) {
        for (int i = 1; i < task_size; i++) {
            if (cur_place != i && isReachable(cur_place, i, v.getCurrentTime()))
                return true;
        }
        return false;
    }

    private double calcMovementPrice(int from, int to, double[] heuristic_f, double[] heuristic_w, Vehicle v) {
        double tmp = Math.pow(pheromoneTrail[from][to], pheromone_exp)
                * nearnessDesirability[from][to]
                * Math.pow(heuristic_f[to], f_exp)
                * Math.pow(heuristic_w[to], w_exp)
                * savingsHeuristic[from][to];

//        double[] new_heuristic_f = calculateHeuristicF(to, v.getCurrentTime() + vrp.getTimeBetween(from, to), v);
//        double[] new_heuristic_w = calculateHeuristicW(to, v.getCurrentTime() + vrp.getTimeBetween(from, to), v);
        for (int i : this.possibleClients) {
            Customer c = this.vrp.getCustomer(i);
            double addition = (Math.pow(pheromoneTrail[to][i], pheromone_exp)
                    * nearnessDesirability[to][i]
                    * savingsHeuristic[to][i])
                    * c.getBecomeRealProbability();
            if (!Double.isNaN(addition) && isReachable(to, i, v.getCurrentTime() + vrp.getTimeBetween(from, to))) {
                tmp += addition;
            }
        }
        return tmp;
    }

    private ArrayList<Place> runVehicle(Vehicle vehicle) {
        int cur_place = 0;
        ArrayList<Integer> solutionIndexes = new ArrayList<>();

        vehicle.setCurrentTime(vehicle.getTIS());

        Place[] solutionPart = new Place[task_size];
        solutionPart[0] = vrp.depo();
        this.visited[0] = true;

        while (!vehicle.isFull() && !allVisited() && hasReachableSummits(cur_place, vehicle)) {
            solutionIndexes.add(0);
            for (int i = 1; i < this.task_size; ++i) {
                int next_place = -1;

                double[] heuristic_f = calculateHeuristicF(cur_place, vehicle.getCurrentTime(), vehicle);
                double[] heuristic_w = calculateHeuristicW(cur_place, vehicle.getCurrentTime(), vehicle);

                if ((bestKnownEdgeUsageProbability > 0) && (rand.nextDouble() <= bestKnownEdgeUsageProbability)) {
                    double bestEdgeVal = -1.0;
                    for (int j = task_size - 1; j > 0; --j) {
                        if (visited[j] || !isReachable(cur_place, j, vehicle.getCurrentTime()))
                            continue;
                        double tmp = calcMovementPrice(cur_place, j, heuristic_f, heuristic_w, vehicle);
                        if (tmp > bestEdgeVal && tmp > 0) {
                            bestEdgeVal = tmp;
                            next_place = j;
                        }
                    }

                } else {                    /* choose edge randomly */
                    double[] customerSelectProbabilities = new double[task_size];
                    double sum = 0;
                    for (int j = 1; j < this.task_size; ++j) {
                        if (visited[j] || !isReachable(cur_place, j, vehicle.getCurrentTime())) {
                            customerSelectProbabilities[j] = 0;
                            continue;
                        }
                        customerSelectProbabilities[j] = calcMovementPrice(cur_place, j, heuristic_f, heuristic_w, vehicle);
                        if (Double.isNaN(customerSelectProbabilities[j]))
                            customerSelectProbabilities[j] = 0;
                        sum += customerSelectProbabilities[j];
                    }
                    if (sum != 0) {
                        for (int j = 0; j < customerSelectProbabilities.length; j++) {
                            customerSelectProbabilities[j] /= sum;
                        }
                        next_place = chooseRandDest(customerSelectProbabilities);
                    } else {
                        next_place = -1;
                    }
                }
                if (next_place == -1 || vehicle.getCurrentCapacity() + vrp.getPlace(next_place).getDemand() > vehicle.getMaxCapacity()
                        || Math.max(vehicle.getCurrentTime() + vrp.getTimeBetween(cur_place, next_place), vrp.getPlace(next_place).getTIS()) > vehicle.getTIE()) {
                    this.totalFleetRunLen += getMoveCost(cur_place, 0);
                    localPheromoneUpdate(solutionIndexes);
                    return arrayToArrayList(solutionPart);
                }
                if (next_place == 0) {
                    vehicle.setCurrentCapacity(0);
                }

                vehicle.addLoad(vrp.getPlace(next_place).getDemand());

//                Place t = vrp.getPlace(next_place);
//                if (vehicle.getCurrentTime() + vrp.getTimeBetween(cur_place, next_place) == 6969 && cur_place != 0){
//                    System.out.println();
//                }
                vrp.getPlace(next_place).setArrivalTime(vehicle.getCurrentTime() + vrp.getTimeBetween(cur_place, next_place));

                this.visited[next_place] = true;
                this.totalFleetRunLen += getMoveCost(cur_place, next_place);
                vehicle.setCurrentTime(Math.max(vrp.getPlace(next_place).getTIS() + vrp.getPlace(next_place).getST(),
                        vehicle.getCurrentTime() + vrp.getTimeBetween(cur_place, next_place)) + vrp.getPlace(next_place).getST());
                cur_place = next_place;
                solutionPart[i] = vrp.getPlace(next_place);
                solutionIndexes.add(next_place);
            }

        }
        this.totalFleetRunLen += getMoveCost(cur_place, 0);
        localPheromoneUpdate(solutionIndexes);

        return arrayToArrayList(solutionPart);
    }

    private double getMoveCost(int from, int to) {
        return vrp.getDistBetween(from, to);
    }

    private ArrayList<Place> arrayToArrayList(Place[] arr) {
        ArrayList<Place> tmp = new ArrayList<>();
        for (Place anArr : arr) {
            if (anArr != null)
                tmp.add(anArr);
        }

        return tmp;
    }

    private void localPheromoneUpdate(ArrayList<Integer> solutionPartIndexes) {
        double pheromone_delta = this.averageTourLength / this.totalFleetRunLen;
        if (this.pheromonLayExp != 1)
            pheromone_delta = Math.pow(pheromone_delta, this.pheromonLayExp);
        this.placePheromone(solutionPartIndexes, pheromone_delta);
    }

    public void setExploit(double exploit) {
        this.bestKnownEdgeUsageProbability = exploit;
    }

    public void setEvap(double evap) {
        this.evapFactor = evap;
    }

    public void setTrail(double trail) {
        this.pheromonLayExp = trail;
    }

    public int getEpoch() {
        return this.epoch;
    }

    public double getBestTourLen() {
        return this.bestTourLength;
    }

    public int getClientsCountVisited() {
        return this.clientsVisited;
    }

    public Map<Integer, Tuple<Vehicle, ArrayList<Place>>> getBestSolution() {
        return bestSolution;
    }


}
