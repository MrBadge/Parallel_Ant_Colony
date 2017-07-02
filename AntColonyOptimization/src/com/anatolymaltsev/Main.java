package com.anatolymaltsev;

//import SDVRPTW.AntColony;
//import SDVRPTW.VRP;

import com.google.maps.DistanceMatrixApiRequest;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.GeocodingResult;
import Common.GoogleMapsProjection;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 THIS IS FOR TESTING PURPOSES ONLY
*/

public class Main {

    public static void main(String[] args) {

//        VRP vrp = new VRP(10, new Random());
//        AntColony ac = new AntColony(vrp, 2);
//        ac.runAnt();
        GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyDUdRxUd-4YMeYtBENfgcMiiU9ciJyOMHU"); //This is test API key
        DistanceMatrixApiRequest req = new DistanceMatrixApiRequest(context);
        req.origins("Москва, 5-я Парковая, д.52");
        req.destinations("Москва, Каширское ш., 31");
        DistanceMatrix result;
        try {
            result = req.await();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        GeocodingResult[] results = new GeocodingResult[0];
        try {
            results = GeocodingApi.geocode(context, "Москва, 5-я Парковая, д.52").await();
            System.out.println(GoogleMapsProjection.latToYWorld(results[0].geometry.location.lat));
            System.out.println(GoogleMapsProjection.lonToXWorld(results[0].geometry.location.lng));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(results[0].formattedAddress);

//        GeocodingResult[] results = new GeocodingResult[0];
//        try {
//            results = GeocodingApi.geocode(context,
//                    "1600 Amphitheatre Parkway Mountain View, CA 94043").await();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println(results[0].formattedAddress);
        System.out.println(Math.pow(-0.3333, 0.8));
//
    }
}

