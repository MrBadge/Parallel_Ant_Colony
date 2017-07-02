package Common;

import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.internal.StringJoin;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.GeocodingResult;

import java.awt.geom.Point2D;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
//import com.sun.tools.javac.util.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class Place {
    protected double xs, ys;
    protected double lat, lng;
    protected double xs_display, ys_display;
    protected double time_interval_start = 0;
    protected double time_interval_end = 200;
    protected double service_time;
    protected double arrival_time;
    protected double demand;
    protected String address = null;

    protected int number;

    private static JSONObject localDB = null;

    public double getDisplayX() {
        return xs_display;
    }

    public double getRealX() {
        return xs_display;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getDisplayY() {
        return ys_display;
    }

    public double getRealY() {
        return ys_display;
    }

    public double getTIS() {
        return time_interval_start;
    }

    public double getTIE() {
        return time_interval_end;
    }

    public String getAddress() {
        return address;
    }

    public double getST() {
        return service_time;
    }

    public double getDemand() {
        return this.demand;
    }

    public void setDisplayX(double x) {
        this.xs_display = x;
    }

    public void setDisplayY(double y) {
        this.ys_display = y;
    }

    public void setTIS(double time) {
        this.time_interval_start = time;
    }

    public void setTIE(double time) {
        this.time_interval_end = time;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setArrivalTime(double time) {
        this.arrival_time = time;
    }

    public double getArrivalTime() {
        return this.arrival_time;
    }

    private static GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyA20nUnH7noBCwMAFaG150mheOakPqngVg"); //AIzaSyDUdRxUd-4YMeYtBENfgcMiiU9ciJyOMHU

    private static void ParseLocalDB() {
        JSONParser parser = new JSONParser();

        try {
            Object obj = parser.parse(new FileReader("/Users/anatolymaltsev/Documents/MEPhI/NIR/AntColonyOptimization/data_for_test_purposes/final_matrix.json"));
            localDB = (JSONObject) obj;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private JSONArray getPlaceCoordsFromLocalDB(String address) {
        String _id = (String) ((JSONObject) localDB.get("addresses")).get(address);
        if (_id == null) {
            return null;
        }
        JSONArray coords = (JSONArray) ((JSONObject) localDB.get("coordinates")).get(_id);
        if (coords == null) {
            return null;
        }
        return coords;
    }

    private static JSONObject getTDMfromLocalDB(String addr1, String addr2) {
        String _id1 = (String) ((JSONObject) localDB.get("addresses")).get(addr1);
        String _id2 = (String) ((JSONObject) localDB.get("addresses")).get(addr2);
        if (_id1 == null || _id2 == null) {
            return null;
        }
        JSONObject row = (JSONObject) ((JSONObject) localDB.get("matrix")).get(_id1);
        if (row == null) {
            return null;
        }
        return (JSONObject) row.get(_id2);
    }

    public Place(int n, double xs, double ys, double demand, double tis, double tie, double st) {
        this.xs = this.xs_display = xs;
        this.ys = this.ys_display = ys;
        this.demand = demand;
        this.time_interval_start = tis;
        this.time_interval_end = tie;
        this.service_time = st;
        this.number = n;
    }

    public Place(int n, String address, double demand, double tis, double tie, double st) {
        ParseLocalDB();
        JSONArray coords = getPlaceCoordsFromLocalDB(address);
        if (coords == null) {
            GeocodingResult[] results = new GeocodingResult[0];
            try {
                results = GeocodingApi.geocode(Place.context, address).language("ru").await();
                this.lat = results[0].geometry.location.lat;
                this.lng = results[0].geometry.location.lng;
                this.address = results[0].formattedAddress;
                this.xs = this.xs_display = GoogleMapsProjection.latToYWorld(this.lat);
                this.ys = this.ys_display = GoogleMapsProjection.lonToXWorld(this.lng);
            } catch (Exception e) {
                e.printStackTrace();
                this.address = address;
                this.xs = this.xs_display = 0;
                this.ys = this.ys_display = 0;
            }
        } else {
            this.lat = Double.parseDouble((String) coords.get(0));
            this.lng = Double.parseDouble((String) coords.get(1));
            this.address = address;
            this.xs = this.xs_display = GoogleMapsProjection.latToYWorld(this.lat);
            this.ys = this.ys_display = GoogleMapsProjection.lonToXWorld(this.lng);
        }
        this.demand = demand;
        this.time_interval_start = tis;
        this.time_interval_end = tie;
        this.service_time = st;
        this.number = n;
    }

    //public Place(){}

    public String toString() {
        return "(" + xs + ", " + ys + "), №" + number;
    }

    public static double getEuclidDistBetweenPoints(Point2D.Double a, Point2D.Double b) {
        return Utilities.round(Math.sqrt(Math.pow((a.getX() - b.getX()), 2) + Math.pow((a.getY() - b.getY()), 2)), 2);
    }

    public static double getEuclidDistBetweenPlaces(Place a, Place b) {
        return Utilities.round(Math.sqrt(Math.pow((a.getRealX() - b.getRealX()), 2) + Math.pow((a.getRealY() - b.getRealY()), 2)), 2);
    }

    public static double getTravelTimeBetweenPlaces(Place a, Place b) {
        return Utilities.round(Math.sqrt(Math.pow((a.getRealX() - b.getRealX()), 2) + Math.pow((a.getRealY() - b.getRealY()), 2)), 2);
    }

    public static Pair<Double, Double> getDistAndTimeBetweenPlaces(Place a, Place b) {
        if (a.getAddress() == null || b.getAddress() == null) {
            return new Pair<>(Place.getEuclidDistBetweenPlaces(a, b), Place.getTravelTimeBetweenPlaces(a, b));
        }
        if (Objects.equals(a.getAddress(), b.getAddress())) {
            return new Pair<>(0., 0.);
        }
        JSONObject localDBres = getTDMfromLocalDB(a.getAddress(), b.getAddress());
        if (localDBres == null) {
            DistanceMatrixApiRequest req = new DistanceMatrixApiRequest(context);
            req.origins(a.getAddress());
            req.destinations(b.getAddress());
            DistanceMatrix result;
            try {
                result = req.await();
                Double dist = (double) result.rows[0].elements[0].distance.inMeters;
                Double time = (double) result.rows[0].elements[0].duration.inSeconds;
                return new Pair<>(dist, time);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Pair<>(Double.MAX_VALUE, Double.MAX_VALUE);
        } else {
            return new Pair<>(Double.parseDouble(String.valueOf(localDBres.get("distance"))), Double.parseDouble(String.valueOf(localDBres.get("time"))));
        }
    }

    public String getNo() {
        return String.valueOf(this.number);
    }
}
