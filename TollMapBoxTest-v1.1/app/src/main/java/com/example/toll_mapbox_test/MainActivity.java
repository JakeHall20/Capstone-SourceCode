package com.example.toll_mapbox_test;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;

import com.google.gson.JsonObject;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;






public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private int tempMarkerCount;
    private CarmenFeature home;
    private CarmenFeature work;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";

    private Marker sourceMarker, destinationMarker, homeMarker;
    private Point sourcePoint, destinationPoint;

    private Boolean ready_to_pay = false;




    private static final LatLng locationOne = new LatLng(37.161411, -83.579029);
    private static final LatLng locationTwo = new LatLng(37.409338, -75.507633);

    private NavigationMapRoute navigationMapRoute;
    private static final String TAG = "MainActivity";

//    private NavigationMapRoute navigationMapRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Mapbox access token is configured here.
        Mapbox.getInstance(this, getString(R.string.api_key));
        // mapview from XML
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        tempMarkerCount = 0;
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                initSearchFab();
                addUserLocations();
                // Add the symbol layer icon to map for future use
                style.addImage(symbolIconId, BitmapFactory.decodeResource(
//                        MainActivity.this.getResources(), R.drawable.map_default_map_marker));
                        MainActivity.this.getResources(), R.drawable.map_marker_light));
                // Create an empty GeoJSON source using the empty feature collection
                // Set up a new symbol layer for displaying the searched location's feature coordinates
                setUpSource(style);
                setupLayer(style);
            }
        });
        // adding boundaries
        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                .include(locationOne) // Northeast
                .include(locationTwo) // Southwest
                .build();
        // moving camera -- initial position
        mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 50));
    }

    private void initSearchFab() {
        payButton();
        findViewById(R.id.fab_location_source).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // currently launching autocomplete as an activity
                Intent intent_search = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken())
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
//                                  .addInjectedFeature(home)
//                                  .addInjectedFeature(work)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(MainActivity.this);
//                  startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
                //
                // destination
                Intent intent_dest = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken())
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
//                                  .addInjectedFeature(home)
//                                  .addInjectedFeature(work)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(MainActivity.this);

                startActivityForResult(intent_search, REQUEST_CODE_AUTOCOMPLETE);
            }
        });

    }

    private void getRoute(Point origin, Point destination){
        NavigationRoute.builder(this)
            .accessToken(Mapbox.getAccessToken())
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(new Callback<DirectionsResponse>() {
                @Override
                public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                    if (response.body() == null){
                        Timber.e("No routes found.");
                        return;
                    }
                    else if (response.body().routes().size() == 0){
                        Timber.e("0 routes.");
                        return;
                    }

                    DirectionsRoute currentRoute = response.body().routes().get(0);
                    // remove route after they click a new source, remove route/markers/reset everything.
                    if (navigationMapRoute != null){
                        navigationMapRoute.removeRoute();
                    }

                    navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap);
                    navigationMapRoute.addRoute(currentRoute);
                }

                @Override
                public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    Timber.e("fail%s", t.getMessage());
                }
            });
    }
    // locations - type: CarmenFeature
    // Points - .. addMarker()
    private CarmenFeature example, example1, rich;
    private CarmenFeature powhiteParkway, colemanBridge, sixSixExp, sixFourExp;
    private Marker exampleMarker, example1Marker, richMarker;
    private Marker powhiteParkway_Marker, colemanBridge_Marker, sixSixExp_Marker, sixFourExp_Marker;

    private void addUserLocations() {
        home = CarmenFeature.builder().text("Mapbox SF Office")
                .placeName("50 Beale st, San Francisco, CA")
                .geometry(Point.fromLngLat(-77.622013, 37.462681))
                .id("mapbox-sf")
                .properties(new JsonObject())
                .build();

        work = CarmenFeature.builder().text("Mapbox DC Office")
                .placeName("740 15th Street NW, Washington, DC")
                .geometry(Point.fromLngLat(-77.0338348, 38.899750))
                .id("mapbox-dc")
                .properties(new JsonObject())
                .build();

        // start toll points
        powhiteParkway = CarmenFeature.builder().text("Mapbox DC Office")
                .geometry(Point.fromLngLat(-77.622013, 37.462681))
                .id("mapbox-pw")
                .properties(new JsonObject())
                .build();

        colemanBridge = CarmenFeature.builder().text("Mapbox DC Office")
                .geometry(Point.fromLngLat(-76.511905, 37.235659))
                .id("mapbox-cb")
                .properties(new JsonObject())
                .build();

        sixSixExp = CarmenFeature.builder().text("Mapbox DC Office")
                .geometry(Point.fromLngLat(-77.066577, 38.892586))
                .id("mapbox-cb")
                .properties(new JsonObject())
                .build();

        sixFourExp = CarmenFeature.builder().text("Mapbox DC Office")
                .geometry(Point.fromLngLat(-76.195224, 36.864498))
                .id("mapbox-cb")
                .properties(new JsonObject())
                .build();

        // start of toll points
        /////////////////////////////////////////////////////////////////////////////
        powhiteParkway_Marker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(((Point) powhiteParkway.geometry()).latitude(),
                        ((Point) powhiteParkway.geometry()).longitude())));

        colemanBridge_Marker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(((Point) colemanBridge.geometry()).latitude(),
                        ((Point) colemanBridge.geometry()).longitude())));

        sixSixExp_Marker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(((Point) sixSixExp.geometry()).latitude(),
                        ((Point) sixSixExp.geometry()).longitude())));

        sixFourExp_Marker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(((Point) sixFourExp.geometry()).latitude(),
                        ((Point) sixFourExp.geometry()).longitude())));
    }
// source style
    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }
// icons
    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                iconImage(symbolIconId),
                iconOffset(new Float[]{0f, -8f})
        ));
    }

// when the result is received  (when the user clicks / enters the address;;
// then presses enter / clicks on the suggested address
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
// Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);
// Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
// Then retrieve and update the source designated for showing a selected location's symbol layer icon
            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[]{Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }

// Move map camera to the selected location ( the location the user searches for / selects )
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                            ((Point) selectedCarmenFeature.geometry()).longitude()))
//                                    .zoom(20)
                                    .build()), 4000);
//                    // end animate camera

                    // placing markers
                    if (tempMarkerCount == 0) {
                        sourcePoint = Point.fromLngLat(((Point) selectedCarmenFeature.geometry()).longitude(), ((Point) selectedCarmenFeature.geometry()).latitude());
                        sourceMarker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                ((Point) selectedCarmenFeature.geometry()).longitude())));

                        tempMarkerCount += 1;
                    } else if (tempMarkerCount == 1) {
                        destinationPoint = Point.fromLngLat(((Point) selectedCarmenFeature.geometry()).longitude(), ((Point) selectedCarmenFeature.geometry()).latitude());
                        destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                ((Point) selectedCarmenFeature.geometry()).longitude())));
                        // get route
                        getRoute(sourcePoint, destinationPoint);


                        tempMarkerCount = 0;

                    }
                }
            }
        }
    }
    // TollApplication.java holds these variables
    // they will be accessible to the payment side in order to keep track of
    // what tolls checkboxes are being activated
    public  void onCheckboxClicked(View view){
        boolean checked = ((CheckBox) view).isChecked();
        int count = 0;
        // count is for updating at the end (only because bool was return false in some cases)
        // so now count is keeping track of numtolls checked, just to see if > 0
        // still quits working after a couple of selects
        switch (view.getId()) {
            // toll 1 clicked
            case R.id.toll1:
                if (checked)
                {
                    ((TollApplication) getApplicationContext()).Toll1 = 1;
                    count += 1;
                }
                break;
            // toll 2 clicked
            case R.id.toll2:
                if (checked)
                {
                    ((TollApplication) getApplicationContext()).Toll2 = 1;
                    count += 1;
                }
                break;
            // toll 3 clicked
            case R.id.toll3:
                if (checked)
                {
                    ((TollApplication) getApplicationContext()).Toll3 = 1;
                    count += 1;
                }
                break;
            // toll 4 clicked
            case R.id.toll4:
                if (checked)
                {
                    ((TollApplication) getApplicationContext()).Toll4 = 1;
                    count += 1;
                }
                break;
        }
        // update if ready to pay or not (boolean value)
        ready_to_pay = count > 0;
    }

    public void payButton(){
        findViewById(R.id.pay_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ready_to_pay){
                    // change button to green temporarily ** REMOVE LATER **
                    findViewById(R.id.pay_button).setBackgroundColor(Color.GREEN);
                    // pull up pay activity
                }
                else{
                    Timber.e("No Tolls Selected.");
                    findViewById(R.id.pay_button).setBackgroundColor(Color.RED);
                }
            }
        });
    }




    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {

        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}



//import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.v7.app.AppCompatActivity;
//import android.widget.Toast;
//
//import com.mapbox.mapboxsdk.Mapbox;
//import com.mapbox.mapboxsdk.camera.CameraPosition;
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
//import com.mapbox.mapboxsdk.geometry.LatLng;
//import com.mapbox.mapboxsdk.maps.MapView;
//import com.mapbox.mapboxsdk.maps.MapboxMap;
//import com.mapbox.mapboxsdk.maps.Style;
//
//public class MainActivity extends AppCompatActivity implements MapboxMap.OnMapClickListener {
//
//    private MapView mapView;
//    private MapboxMap mapboxMap;
//    // two points
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Mapbox.getInstance(this, getString(R.string.api_key));
//        setContentView(R.layout.activity_main);
//
//        mapView = findViewById(R.id.mapView);
//        mapView.onCreate(savedInstanceState);
//        mapView.getMapAsync(this::onMapReady);
//    }
//        // listens for user click on the map when the map is loaded
//        public void onMapReady(@NonNull final MapboxMap mapboxMap) {
//            MainActivity.this.mapboxMap = mapboxMap;
//            mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
//                @Override
//                public void onStyleLoaded(@NonNull Style style) {
//                    Toast.makeText(
//                            MainActivity.this,
//                            getString(R.string.tap_on_map_instruction),
//                            Toast.LENGTH_LONG
//                    ).show();
//                    mapboxMap.addOnMapClickListener(MainActivity.this);
//                }
//            });
//        }
//        // what is called when the user does click on the map (will be onButtonClick)
//        @Override
//        public boolean onMapClick(@NonNull LatLng point){
//            Toast.makeText(
//                    MainActivity.this,
//                    getString(R.string.tap_on_map_instruction),
//                    Toast.LENGTH_LONG
//            ).show();
//            // call getAddress
//            // call getRoute
//
//            // call the geo code map builder
//            // set the points on the map
//            // draw the points between the two points
//            // use the CameraPosition to reposition
//
//            CameraPosition position = new CameraPosition.Builder()
//                    .target(new LatLng(38.9759, -77.0369))
//                    .zoom(17)
//                    .bearing(180)
//                    .tilt(30)
//                    .build();
//                mapboxMap.animateCamera(CameraUpdateFactory
//                .newCameraPosition(position), 7000);
//            return true;
//        }
//
//
///* Overloading methods */
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        mapView.onStart();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mapView.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        mapView.onPause();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        mapView.onStop();
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        mapView.onSaveInstanceState(outState);
//    }
//
//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//        mapView.onLowMemory();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mapView.onDestroy();
//    }
//}
//
//
