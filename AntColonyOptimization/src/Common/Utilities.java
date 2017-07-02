package Common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class Utilities {
    public static int randInt(Random rand, int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}


