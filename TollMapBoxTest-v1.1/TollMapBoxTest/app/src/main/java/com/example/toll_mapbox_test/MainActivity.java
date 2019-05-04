package com.example.toll_mapbox_test;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.gson.JsonObject;
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
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;


//import java.util.List;
//
//import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
//import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
//
//import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
//import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
//import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
//import com.mapbox.api.directions.v5.models.DirectionsResponse;
//import com.mapbox.api.directions.v5.models.DirectionsRoute;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import android.util.Log;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//
//import butterknife.BindView;
//import butterknife.ButterKnife;
//import butterknife.OnClick;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import timber.log.Timber;

/**
 * Use the places plugin to take advantage of Mapbox's location search ("geocoding") capabilities. The plugin
 * automatically makes geocoding requests, has built-in saved locations, includes location picker functionality,
 * and adds beautiful UI into your Android project.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private int tempMarkerCount;
    private CarmenFeature home;
    private CarmenFeature work;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";

    private Marker sourceMarker;
    private Marker destinationMarker;
    private Marker homeMarker;

    private MapboxNavigation navigationMapRoute;


    private static final LatLng locationOne = new LatLng(37.161411, -83.579029);
    private static final LatLng locationTwo = new LatLng(37.409338, -75.507633);

    private MapboxNavigation NavigationRoute;
    private NavigationRoute navigationRoute;

//    private NavigationMapRoute navigationMapRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

// Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.api_key));

// This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        tempMarkerCount = 0;

    }
    // end autocomplete fragment

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        // add nav

        // end nav
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                initSearchFab();

                addUserLocations();

// Add the symbol layer icon to map for future use
                style.addImage(symbolIconId, BitmapFactory.decodeResource(
                        MainActivity.this.getResources(), R.drawable.map_default_map_marker));

// Create an empty GeoJSON source using the empty feature collection
                setUpSource(style);

// Set up a new symbol layer for displaying the searched location's feature coordinates
                setupLayer(style);
            }
        });
        // adding boundaries
        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                .include(locationOne) // Northeast
                .include(locationTwo) // Southwest
                .build();

        mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 50), 5000);
    }

    private void initSearchFab() {
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

    // placing points

    /*

     * could use these for creating the tolls, without displaying them
     * to the users, and only adding a marker when the route passes through
     * the lat / long values.

     */
    private CarmenFeature example, example1, rich;
    private Marker exampleMarker, example1Marker, richMarker;

    private void addUserLocations() {
        home = CarmenFeature.builder().text("Mapbox SF Office")
                .geometry(Point.fromLngLat(-122.399854, 37.7884400))
                .placeName("50 Beale st, San Francisco, CA")
                .id("mapbox-sf")
                .properties(new JsonObject())
                .build();

        work = CarmenFeature.builder().text("Mapbox DC Office")
                .placeName("740 15th Street NW, Washington, DC")
                .geometry(Point.fromLngLat(-77.0338348, 38.899750))
                .id("mapbox-dc")
                .properties(new JsonObject())
                .build();
        // locations added 3-27-19
        example = CarmenFeature.builder().text("Example")
                .placeName("1 College Ave, Wise, VA")
                .geometry(Point.fromLngLat(-82.56019, 36.97048))
                .id("mapbox-ex")
                .properties(new JsonObject())
                .build();

        example1 = CarmenFeature.builder().text("Example1")
                .placeName("1 College Ave, Wise, VA")
                .geometry(Point.fromLngLat(-81.56019, 36.97048))
                .id("mapbox-ex1")
                .properties(new JsonObject())
                .build();

        rich = CarmenFeature.builder().text("Mapbox DC Office")
                .placeName("740 15th Street NW, Washington, DC")
                .geometry(Point.fromLngLat(-77.429612, 37.536870))
                .id("mapbox-rc")
                .properties(new JsonObject())
                .build();

        // adding marker for example
        exampleMarker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) example.geometry()).latitude(),
                ((Point) example.geometry()).longitude())));
        // adding marker for example1
        example1Marker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) example1.geometry()).latitude(),
                ((Point) example1.geometry()).longitude())));
        // for work variable
        homeMarker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) work.geometry()).latitude(),
                ((Point) work.geometry()).longitude())));
        // for work variable
        richMarker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) rich.geometry()).latitude(),
                ((Point) rich.geometry()).longitude())));
    }

    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }

    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                iconImage(symbolIconId),
                iconOffset(new Float[]{0f, -8f})
        ));
    }

//    @Override
//    public boolean onMapClick(@NonNull LatLng point) {
//        LatLngBounds latLngBounds = new LatLngBounds.Builder()
//                .include(locationOne) // Northeast
//                .include(locationTwo) // Southwest
//                .build();
//
//        mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 50), 5000);
//        return true;
//    }

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
                        sourceMarker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                ((Point) selectedCarmenFeature.geometry()).longitude())));

                        tempMarkerCount += 1;
                    } else if (tempMarkerCount == 1) {
                        destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                ((Point) selectedCarmenFeature.geometry()).longitude())));
                        tempMarkerCount = 0;
                        //route
                        // if second marker dropped
//                        NavigationRoute.builder()
//                                .accessToken(R.string.api_key)
//                                .origin(home)
//                                .destination(work)
//                                .build()
//                                .getRoute(new Callback<DirectionsResponse>() {
//                                    @Override
//                                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//
//                                    }
//
//                                    @Override
//                                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
//
//                                    }
//
//                                });
                        // end placing markers

                    }
                }
            }
        }
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
        super.onPause();
        mapView.onPause();
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
