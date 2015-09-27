package com.ola.olatourism.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ola.materialdesign.views.ButtonRectangle;
import com.ola.olatourism.R;
import com.ola.olatourism.adapter.PlacesListViewAdapter;
import com.ola.olatourism.parser.DirectionsJsonParser;
import com.ola.olatourism.util.OlaConstants;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import model.PlacesDTO;
import model.RideDetailsDTO;

public class TripPriorityActivity extends Activity {

    ListView lstPlaces;
    ArrayList<PlacesDTO> placeList = HopperActivity.placeList;
    private PlacesListViewAdapter placesListViewAdapter;
    private RadioGroup radioGroup;
    private LatLng originPos;
    private LatLng destPos;
    private String[] urlList;
    private int[] distaceList;
    private int index = -1;

    private ButtonRectangle updatePlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_priority);

        final String originLoc = getIntent().getStringExtra(OlaConstants.ORIGIN_LOC);
        lstPlaces = (ListView) findViewById(R.id.placeListView);
        radioGroup = (RadioGroup) findViewById(R.id.radioPriorityType);
        placesListViewAdapter = new PlacesListViewAdapter(getApplicationContext(), placeList);
        lstPlaces.setAdapter(placesListViewAdapter);
        originPos = getLocationFromAddress(originLoc);
        urlList = new String[placeList.size()];
        distaceList = new int[placeList.size()];
        updatePlan = (ButtonRectangle) findViewById(R.id.upadtePlan);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                if(checkedId == R.id.radioDistance) {
                    index = -1;
                    Toast.makeText(getApplication(), "Distance", Toast.LENGTH_SHORT).show();
                    for(int i = 0; i < placeList.size(); i++) {
                        destPos = new LatLng(placeList.get(i).getLatitude(),placeList.get(i).getLongitude());
                        urlList[i] = getDirectionsUrl(originPos, destPos);
                        Log.e("ListPlace", urlList[i] + "");
                        DownloadTask downloadTask = new DownloadTask();
                        downloadTask.execute(urlList[i]);
                    }
                }
                else if(checkedId == R.id.radioFare) {
                    Toast.makeText(getApplication(), "Fare", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updatePlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacesDTO temp;
                for(int i = 0; i < placeList.size() - 1; i++){
                    for(int j = 1; j < placeList.size() - i; j++)
                    {
                        if(distaceList[j - 1] > distaceList[j])
                        {
                            temp = placeList.get(j - 1);
                            placeList.set(j - 1, placeList.get(j));
                            placeList.set(j, temp);
                        }
                    }
                }
                placesListViewAdapter = new PlacesListViewAdapter(getApplicationContext(), placeList);
                lstPlaces.setAdapter(placesListViewAdapter);
            }
        });

        lstPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(TripPriorityActivity.this, DriveMeActivity.class);
                String selectItem = placeList.get(position).getAddress();
                intent.putExtra(OlaConstants.ORIGIN_LOC, originLoc);
                intent.putExtra(OlaConstants.SELECT_LOC, selectItem);
                startActivity(intent);
            }
        });
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){
        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        String str_dest = "destination="+dest.latitude+","+dest.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin+"&"+str_dest+"&"+sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
        return url;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }



        class CabEstimate extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String startLan = params[0];
                String startLon = params[1];
                String dropLan = params[2];
                String dropLon = params[3];
                String category = params[4];

                URL url = null;
                String response = null;
                String parameters = "pickup_lat=" + startLan + "&pickup_lng=" + startLon + "&drop_lat=" + dropLan + "&drop_lng=" + dropLon;

                if (category != null) {
                    parameters = parameters + "&category=" + category;
                }

                String endpointURL = "http://sandbox-t.olacabs.com/v1/products?" + parameters;

                try {
                    url = new URL("http://sandbox-t.olacabs.com/v1/products?" + parameters);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                HttpURLConnection urlConnection = null;
                try {

                    urlConnection = (HttpURLConnection) url
                            .openConnection();

                    urlConnection.setRequestProperty("X-APP-TOKEN", "24139f46bfe14fac85fc6799240e0a7a");
                    urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
                    urlConnection.setRequestProperty("Accept", "*/*");
                    urlConnection.setRequestMethod("GET");
                    int status = urlConnection.getResponseCode();
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    response = convertInputStreamToString(inputStream);

                    Log.d("TripPriority", "resp : "+response);


                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace(); //If you want further info on failure...
                    }
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                Gson gson = new Gson();

                Type listType = new TypeToken<RideDetailsDTO>(){}.getType();

                RideDetailsDTO rideDetailsDTO = gson.fromJson(result, listType);

                Log.d("HELLO", "rideDetailsDTO : "+rideDetailsDTO.categories.get(0).display_name+" "+rideDetailsDTO.ride_estimate.get(0).amount_max);

            }

        }

        class CabAvailability extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String startLan = params[0];
                String startLon = params[1];
                String category = params[2];

                URL url = null;
                String response = null;
                String parameters = "pickup_lat=" + startLan + "&pickup_lng=" + startLon;

                if (category != null) {
                    parameters = parameters + "&category=" + "sedan";
                }

                String endpointURL = "http://sandbox-t.olacabs.com/v1/products?" + parameters;

                try {
                    url = new URL("http://sandbox-t.olacabs.com/v1/products?" + parameters);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                HttpURLConnection urlConnection = null;
                try {

                    urlConnection = (HttpURLConnection) url
                            .openConnection();

                    urlConnection.setRequestProperty("X-APP-TOKEN", "24139f46bfe14fac85fc6799240e0a7a");
                    urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
                    urlConnection.setRequestProperty("Accept", "*/*");
                    urlConnection.setRequestMethod("GET");
                    int status = urlConnection.getResponseCode();
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    response = convertInputStreamToString(inputStream);
                    Log.d("HELLO", "response : " + response);


                    Gson gson = new Gson();

                    Type listType = new TypeToken<RideDetailsDTO>(){}.getType();
                    response = response.replace("{}", "null");
                    RideDetailsDTO cabAvailabilityDTO = gson.fromJson(response, listType);

                    Log.d("HELLO", "cab availabitilty rideDetailsDTO : " + cabAvailabilityDTO.categories.get(0).display_name + " ");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace(); //If you want further info on failure...
                    }
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

            }
        }

        private String convertInputStreamToString(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            String result = "";
            while ((line = bufferedReader.readLine()) != null) {
                result += line;
            }
            if (null != inputStream) {
                inputStream.close();
            }
            return result;
        }

        class BookACab extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {
                String startLan = (String) params[0];
                String startLon = (String) params[1];
                String category = (String) params[2];
                String accessToken = (String) params[3];


                URL url = null;
                String response = null;
                String parameters = "pickup_lat=" + startLan + "&pickup_lng=" + startLon + "&pickup_mode=NOW";

                if (category != null) {
                    parameters = parameters + "&category=" + "sedan";
                }

                String endpointURL = "http://devapi.olacabs.com/v1/booking/create?" + parameters;

                try {
                    url = new URL("http://sandbox-t.olacabs.com/v1/products?" + parameters);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                HttpURLConnection urlConnection = null;
                try {

                    urlConnection = (HttpURLConnection) url
                            .openConnection();

                    urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
                    urlConnection.setRequestProperty("X-APP-TOKEN", "24139f46bfe14fac85fc6799240e0a7a");
                    urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
                    urlConnection.setRequestProperty("Accept", "*/*");
                    urlConnection.setRequestMethod("GET");
                    int status = urlConnection.getResponseCode();
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    response = convertInputStreamToString(inputStream);
                    return response;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace(); //If you want further info on failure...
                    }
                }
                return "";
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

            }

            private String convertInputStreamToString(InputStream inputStream) throws IOException {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                String result = "";
                while ((line = bufferedReader.readLine()) != null) {
                    result += line;
                }
                if (null != inputStream) {
                    inputStream.close();
                }
                return result;
            }
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> > {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJsonParser parser = new DirectionsJsonParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";

            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    if(j==0){    // Get distance from the list
                        distance = (String)point.get("distance");
                        continue;
                    }else if(j==1){ // Get duration from the list
                        duration = (String)point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(2);
                lineOptions.color(Color.RED);
            }
            Log.e("Distance", distance);
            int len = 0;
            try {
                len = ((Number) NumberFormat.getInstance().parse(distance)).intValue();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Log.e("Distance", len + " ");
            index++;
            storeDistance(index, len);
//            tvDistanceDuration.setText("Distance:"+distance + ", Duration:"+duration);

            // Drawing polyline in the Google Map for the i-th route
            //googleMap.addPolyline(lineOptions);
        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("EXC", "Exception : "+e.getMessage());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    public LatLng getLocationFromAddress(String strAddress) {

        Geocoder coder = new Geocoder(this);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude() );

        } catch (Exception ex) {

            ex.printStackTrace();
        }

        return p1;
    }

    void storeDistance(int counter, int distanceVal)
    {
        distaceList[counter] = distanceVal;
    }
}

