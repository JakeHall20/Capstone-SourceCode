package com.example.toll_mapbox_test;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;

public class MainActivity extends AppCompatActivity implements MapboxMap.OnMapClickListener {

    private MapView mapView;
    private MapboxMap mapboxMap;
    // two points


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.api_key));
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::onMapReady);
    }
        // listens for user click on the map when the map is loaded
        public void onMapReady(@NonNull final MapboxMap mapboxMap) {
            MainActivity.this.mapboxMap = mapboxMap;
            mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    Toast.makeText(
                            MainActivity.this,
                            getString(R.string.tap_on_map_instruction),
                            Toast.LENGTH_LONG
                    ).show();
                    mapboxMap.addOnMapClickListener(MainActivity.this);
                }
            });
        }
        // what is called when the user does click on the map (will be onButtonClick)
        @Override
        public boolean onMapClick(@NonNull LatLng point){
            Toast.makeText(
                    MainActivity.this,
                    getString(R.string.tap_on_map_instruction),
                    Toast.LENGTH_LONG
            ).show();
            // call getAddress
            // call getRoute

            // call the geo code map builder
            // set the points on the map
            // draw the points between the two points
            // use the CameraPosition to reposition

            CameraPosition position = new CameraPosition.Builder()
                    .target(new LatLng(38.9759, -77.0369))
                    .zoom(17)
                    .bearing(180)
                    .tilt(30)
                    .build();
                mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 7000);
            return true;
        }


/* Overloading methods */

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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
}


