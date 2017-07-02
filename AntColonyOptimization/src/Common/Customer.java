package Common;

import java.util.Random;

public class Customer extends Place {
    private boolean isReal = true;
    private double becomeRealProbability = 0;

    public Customer(int n, double xs, double ys, double demand, double time_start, double time_end, double service_time,  boolean isReal, double becomeRealProbability) {
        super(n, xs, ys, demand, time_start, time_end, service_time);
        this.isReal = isReal;
        this.becomeRealProbability = becomeRealProbability;
    }

    public Customer(int n, String address, double demand, double time_start, double time_end, double service_time,  boolean isReal, double becomeRealProbability) {
        super(n, address, demand, time_start, time_end, service_time);
        this.isReal = isReal;
        this.becomeRealProbability = becomeRealProbability;
    }

    public Customer(int n, Random rand) {
        super(n, rand.nextDouble(), rand.nextDouble(), Utilities.randInt(rand, 10, 20),
                Utilities.randInt(rand, 0, 100), Utilities.randInt(rand, 130, 230),
                Utilities.randInt(rand, 5, 10));
        this.isReal = Math.random() < 0.8;
        if (!this.isReal) {
            this.becomeRealProbability = Math.random();
        }
    }

    public boolean isReal() {
        return this.isReal;
    }

    public double getBecomeRealProbability() {
        return this.becomeRealProbability;
    }

}
