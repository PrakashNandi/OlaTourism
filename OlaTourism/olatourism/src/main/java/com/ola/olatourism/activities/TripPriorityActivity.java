package com.ola.olatourism.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ola.olatourism.R;
import com.ola.olatourism.adapter.PlacesListViewAdapter;
import com.ola.olatourism.util.OlaConstants;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import model.CabAvailabilityDTO;
import model.PlacesDTO;
import model.RideDetailsDTO;

public class TripPriorityActivity extends Activity {

    ListView lstPlaces;
    ArrayList<PlacesDTO> placeList = HopperActivity.placeList;
    private PlacesListViewAdapter placesListViewAdapter;
    private RadioGroup radioGroup;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_priority);

        sharedPreferences = getSharedPreferences(OlaConstants.USER_TOKEN,
                Context.MODE_PRIVATE);



        lstPlaces = (ListView) findViewById(R.id.placeListView);
        radioGroup = (RadioGroup) findViewById(R.id.radioPriorityType);
        placesListViewAdapter = new PlacesListViewAdapter(getApplicationContext(), placeList);
        lstPlaces.setAdapter(placesListViewAdapter);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                if (checkedId == R.id.radioDistance)
                    Toast.makeText(getApplication(), "Distance", Toast.LENGTH_SHORT).show();
                else if (checkedId == R.id.radioFare)
                    Toast.makeText(getApplication(), "Fare", Toast.LENGTH_SHORT).show();
            }
        });

//        new CabEstimate().execute("12.842471", "77.675480", "12.954749", "77.675480", "sedan");
        new CabAvailability().execute("12.842471", "77.675480", "sedan");
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
