package com.bussa.splashaward2018;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;



public class BusAssistance {

    private List<BusInfo> mBusList;

    public String giveMeBusList() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("stopno", "44151");
            jsonObject.put("busname", "Opp Bt Batok Fire Stn");

            JSONArray jsonBusArray = new JSONArray();


            for (BusInfo bi : mBusList) {
                JSONObject bus1 = new JSONObject();
                bus1.put("serviceno", bi.busNo);
                bus1.put("reminded", inReminderList(bi.busNo));
                bus1.put("distance", bi.getDistance());
                bus1.put("time", bi.getMinutes()); // 2 minutes t
                jsonBusArray.put(bus1);
            }


            jsonObject.put("buses",jsonBusArray );

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();


    }
    public static Date parseJSONDate( String input ) throws java.text.ParseException {

        //NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
        //things a bit.  Before we go on we have to repair this.
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" );

        //this is zero time so we need to add that TZ indicator for
        if ( input.endsWith( "Z" ) ) {
            input = input.substring( 0, input.length() - 1) + "GMT-00:00";
        } else {
            int inset = 6;

            String s0 = input.substring( 0, input.length() - inset );
            String s1 = input.substring( input.length() - inset, input.length() );

            input = s0 + "GMT" + s1;
        }

        return df.parse( input );

    }

    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters


        return distance;
    }

    public boolean insideBusList(String busNo) {
        for (BusInfo bi : mBusList) {
            if (bi.busNo.equalsIgnoreCase(busNo)) {
                return true;
            }
        }
        return false;
    }


    class BusInfo {
        public String busNo;
        public Date EstimatedArrival;
        public double Longitude;
        public double Latitude;

        public double getMinutes() {
            double distance = 30.0;
            Long time = this.EstimatedArrival.getTime() - (new Date()).getTime();
            return time / 60.0 / 1000;

        }

        public double getDistance() {
            return BusAssistance.distance(this.Latitude, BusAssistance.this.mLat, this.Longitude, BusAssistance.this.mLong);

        }


    }

    List<String> reminderList = new ArrayList<String>();

    private double mLong;
    private double mLat;

    public BusAssistance(double Long, double Lat) {
        mLong = Long;
        mLat = Lat;


    }

    public boolean inReminderList(String busNo ) {
        for (String reminder : reminderList) {
            if (busNo.equalsIgnoreCase(reminder))
                return true;
        }

        return false;
    }
    public void addBusToReminder(String busNo) {
        reminderList.add(busNo);
    }

    private Boolean qualifyToRemove(BusInfo busInfo) {

        long miliSeconds = 45 * 1000;

        double distance = 100.0;
        Long time = busInfo.EstimatedArrival.getTime() - (new Date()).getTime();

        if (time <= miliSeconds) {
            return true;
        }

        if (distance(mLat, busInfo.Latitude, mLong, busInfo.Longitude) < distance) {
            return true;
        }
        return false;


    }

    public List<String> checkAndRemoveBusReminder(String busInfo)  {

        if (reminderList.size() == 0) {
            Log.i("splash","no reminder set");
            return null;
        }
        List<BusInfo> busList = getAndStoreBusListUsingJSON(busInfo);

        if (busList == null) {
            Log.i("splash", "return null");
            return null;

        }

        List<String> reminderToRemove = new ArrayList<String>();
        for (String reminder : reminderList) {
            for (BusInfo bi : busList) {
                if (bi.busNo == reminder) {
                    // check if nearby
                    if (qualifyToRemove(bi)) {
                        reminderToRemove.add(reminder);
                    }
                }
            }
        }

        reminderList.removeAll(reminderToRemove);

        return null;
    }

    public List<BusInfo> getAndStoreBusListUsingJSON(String busInfo) {

        try {
            JSONObject jsonObject = new JSONObject(busInfo);
            JSONArray services = jsonObject.getJSONArray("Services");

            List<BusInfo> BusList = new ArrayList<BusInfo>();

            SimpleDateFormat sdf = new SimpleDateFormat();

            for (int i = 0; i < services.length(); i++) {

                JSONObject service = services.getJSONObject(i);
                JSONObject nextBus = service.getJSONObject("NextBus");

                BusInfo bi = new BusInfo();

                bi.busNo =service.getString("ServiceNo");
                bi.EstimatedArrival= parseJSONDate(nextBus.getString("EstimatedArrival"));
                bi.Longitude= nextBus.getDouble("Longitude");
                bi.Latitude= nextBus.getDouble("Latitude");
                BusList.add(bi);
            }
            mBusList = BusList;
            return BusList;


        } catch (JSONException ex) {
            Log.i("splash", "error parsing businfo");
            ex.printStackTrace();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        
        return null;
    }


}
