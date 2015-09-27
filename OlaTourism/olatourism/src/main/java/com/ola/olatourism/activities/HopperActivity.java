package com.ola.olatourism.activities;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.ola.materialdesign.views.ButtonFlat;
import com.ola.materialdesign.views.ButtonRectangle;
import com.ola.olatourism.R;
import com.ola.olatourism.adapter.PlaceAutocompleteAdapter;
import com.ola.olatourism.adapter.PlacesListViewAdapter;
import com.ola.olatourism.parser.DirectionsJsonParser;
import com.ola.olatourism.util.OlaConstants;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import model.PlacesDTO;

public class HopperActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback{

    GoogleMap googleMap;
    ArrayList<LatLng> markerPoints;
//    TextView tvDistanceDuration;
    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private String TAG = "HopperActivity";
    EditText etSource;
    ButtonRectangle planHopBtn;
    ButtonRectangle planHopBtnReal;
    FragmentManager fragmentManager = getFragmentManager();
    public static ArrayList<PlacesDTO> placeList = new ArrayList<>();
    String sourceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hopper_main);
        placeList.clear();
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private void initUI() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

//        tvDistanceDuration = (TextView) findViewById(R.id.tv_distance_time);
        etSource = (EditText) findViewById(R.id.etSource);

        planHopBtn = (ButtonRectangle) findViewById(R.id.planHopBtn);
        planHopBtnReal = (ButtonRectangle) findViewById(R.id.planHopBtnReal);
        planHopBtn.setOnClickListener(new PlanHopListener());
        planHopBtnReal.setOnClickListener(new PlanHopRealListener());

        markerPoints = new ArrayList<LatLng>();

        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        googleMap = fm.getMap();

        googleMap.setMyLocationEnabled(true);

        /*googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                if (markerPoints.size() > 1) {
                    markerPoints.clear();
                    googleMap.clear();
                }

                markerPoints.add(point);

                MarkerOptions options = new MarkerOptions();

                options.position(point);

                if (markerPoints.size() == 1) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else if (markerPoints.size() == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                googleMap.addMarker(options);

                if (markerPoints.size() >= 2) {
                    LatLng origin = markerPoints.get(0);
                    LatLng dest = markerPoints.get(1);

                    String url = getDirectionsUrl(origin, dest);

                    DownloadTask downloadTask = new DownloadTask();

                    downloadTask.execute(url);
                }
            }
        });*/
    }
    DestinationFragment dFragment;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        for(PlacesDTO placesDTO : placeList) {
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(placesDTO.latitude, placesDTO.longitude))
                    .title(placesDTO.address));
        }
    }

    private class PlanHopListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "clicked", Toast.LENGTH_SHORT).show();
            dFragment = new DestinationFragment();
            // Show DialogFragment
            dFragment.show(fragmentManager, "Dialog Fragment");
        }
    }

    private class PlanHopRealListener implements  View.OnClickListener{
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(HopperActivity.this, TripPriorityActivity.class);
            intent.putExtra(OlaConstants.ORIGIN_LOC, sourceAddress);
            startActivity(intent);
        }
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

    @Override
    public void onConnected(Bundle bundle) {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        sourceAddress = getAddressFromCoOrdinates(latLng);
        etSource.setText(sourceAddress);

        Log.d(TAG, "sourceAddress : "+sourceAddress);

        if (mLastLocation != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 16));
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*if (mLastLocation != null) {
            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));*/
//        }
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

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

//            tvDistanceDuration.setText("Distance:"+distance + ", Duration:"+duration);

            // Drawing polyline in the Google Map for the i-th route
            googleMap.addPolyline(lineOptions);
        }
    }

    private String getAddressFromCoOrdinates(LatLng latLng) {

        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        String address1 = "";
        String address2 = "";
        String address3 = "";
        String city = "";
        String state = "";
        String country = "";
        String postalCode = "";
        String knownName = "";

        HashMap<String, String> mAddress = new HashMap<String, String>();

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            address1 = addresses.get(0).getAddressLine(0);
            address2 = addresses.get(0).getAddressLine(1);
            city = addresses.get(0).getLocality();
            state = addresses.get(0).getAdminArea();
            country = addresses.get(0).getCountryName();
            postalCode = addresses.get(0).getPostalCode();
            knownName = addresses.get(0).getFeatureName();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String finalString = "";

        finalString = address1 +", "+ address2+", "+city+", "+ state+", "+ country+", "+ postalCode;
        /*if(address1 != null && !address1.equals(""))
            finalString = finalString + ", " +address1 ;

        if(address2 != null && !address2.equals(""))
            finalString = address1 + ", ";
        if(city != null && !city.equals(""))
            finalString = city + ", ";
        if(state != null && !state.equals(""))
            finalString = state + ", ";

        if(country != null && !country.equals(""))
            finalString = country + ", ";

        if(postalCode != null && !postalCode.equals(""))
            finalString = state + ", ";

        if(knownName != null && !knownName.equals(""))
            finalString = state;

        if(finalString.lastIndexOf(',') == finalString.length()){
            finalString = finalString.substring(0, finalString.length() - 1);
        }*/



        return finalString;
    }


    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));

    public class DestinationFragment extends DialogFragment implements GoogleApiClient.OnConnectionFailedListener{
        private AutoCompleteTextView mAutocompleteView;

        PlaceAutocompleteAdapter mAdapter;
        private TextView mPlaceDetailsText;

        GoogleApiClient mGoogleApiClientPlace;

        private TextView mPlaceDetailsAttribution;
        private ListView placeListView;
        private PlacesListViewAdapter placesListViewAdapter;
        private ButtonFlat doneBtn;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.destination, container,
                    false);
            try {
                if(mGoogleApiClientPlace == null || !mGoogleApiClientPlace.isConnected()) {
                    mGoogleApiClientPlace = new GoogleApiClient.Builder(getApplicationContext())
                            .enableAutoManage(HopperActivity.this, 0 /* clientId */, this)
                            .addApi(Places.GEO_DATA_API)
                            .build();
                }
            } catch (Exception e) {
                Log.d(TAG, "e :: "+e.getMessage());
            }


            getDialog().setTitle("Select Places");
            doneBtn = (ButtonFlat) rootView.findViewById(R.id.doneBtn);
            doneBtn.setOnClickListener(new DoneClickListener());
            placeListView = (ListView) rootView.findViewById(R.id.placeListView);
            mAutocompleteView = (AutoCompleteTextView) rootView.findViewById(R.id.etDestination);

            mAdapter = new PlaceAutocompleteAdapter(getApplicationContext(), mGoogleApiClientPlace, BOUNDS_GREATER_SYDNEY,
                    null);
            mAutocompleteView.setAdapter(mAdapter);

            // Register a listener that receives callbacks when a suggestion has been selected
            mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener);

            placesListViewAdapter = new PlacesListViewAdapter(getApplicationContext(), placeList);
            placeListView.setAdapter(placesListViewAdapter);
            // Do something else
            return rootView;
        }

        private class DoneClickListener implements View.OnClickListener {

            @Override
            public void onClick(View v) {
                getDialog().dismiss();
                for(PlacesDTO placesDTO : placeList) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(placesDTO.latitude, placesDTO.longitude))
                            .title(placesDTO.address));
                }

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(placeList.get(0).getLatitude(), placeList.get(0).getLongitude()), 16));
            }
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        private AdapterView.OnItemClickListener mAutocompleteClickListener
                = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */

                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                mAutocompleteView.setText("");

                final AutocompletePrediction item = mAdapter.getItem(position);
                final String placeId = item.getPlaceId();
                final CharSequence primaryText = item.getPrimaryText(null);

                Log.i(TAG, "Autocomplete item selected: " + primaryText);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
             details about the place.
              */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClientPlace, placeId);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

                LatLng latLng = getLocationFromAddress(item.getFullText(null).toString());
                if(!isSamePlace(latLng)) {
                    placeList.add(new PlacesDTO(item.getFullText(null).toString(), latLng.latitude, latLng.longitude));
                    placesListViewAdapter.notifyDataSetChanged();
                }
            }
        };

        private boolean isSamePlace(LatLng latLng) {

            for(PlacesDTO placesDTO : placeList) {
                if(placesDTO.latitude == latLng.latitude && placesDTO.longitude == latLng.longitude) {
                    return true;
                }
            }

            return false;
        }

        private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
                = new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                if (!places.getStatus().isSuccess()) {
                    // Request did not complete successfully
                    Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                    places.release();
                    return;
                }
                // Get the Place object from the buffer.
                final Place place = places.get(0);

                // Format details of the place for display and show it in a TextView.
                /*mPlaceDetailsText.setText(formatPlaceDetails(getResources(), place.getName(),
                        place.getId(), place.getAddress(), place.getPhoneNumber(),
                        place.getWebsiteUri()));

                // Display the third party attributions if set.
                final CharSequence thirdPartyAttribution = places.getAttributions();
                if (thirdPartyAttribution == null) {
                    mPlaceDetailsAttribution.setVisibility(View.GONE);
                } else {
                    mPlaceDetailsAttribution.setVisibility(View.VISIBLE);
                    mPlaceDetailsAttribution.setText(Html.fromHtml(thirdPartyAttribution.toString()));
                }

                Log.i(TAG, "Place details received: " + place.getName());

                places.release();*/
            }
        };

        private Spanned formatPlaceDetails(Resources res, CharSequence name, String id,
                                                  CharSequence address, CharSequence phoneNumber, Uri websiteUri) {
            Log.e(TAG, res.getString(R.string.place_details, name, id, address, phoneNumber,
                    websiteUri));
            return Html.fromHtml(res.getString(R.string.place_details, name, id, address, phoneNumber,
                    websiteUri));

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                    + connectionResult.getErrorCode());

            // TODO(Developer): Check error code and notify the user of error state and resolution.
            Toast.makeText(getApplicationContext(),
                    "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                    Toast.LENGTH_SHORT).show();
        }

        public LatLng getLocationFromAddress(String strAddress) {

            Geocoder coder = new Geocoder(HopperActivity.this);
            List<Address> address;
            LatLng latLng = null;
            try {
                address = coder.getFromLocationName(strAddress, 5);
                if (address == null) {
                    return null;
                }
                Address location = address.get(0);
                location.getLatitude();
                location.getLongitude();
                Log.d(TAG, "location:" + location.getLatitude() + " " + location.getLongitude());
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
            } catch (Exception e) {
                Log.d(TAG, "exc : " + e.getMessage());
            }

            return latLng;
        }
    }


}