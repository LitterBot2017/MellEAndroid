package edu.cmu.melle.views;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.vision.text.Text;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.cmu.melle.R;
import edu.cmu.melle.models.Location;
import edu.cmu.melle.models.MapState;
import edu.cmu.melle.services.GetLocation;
import edu.cmu.melle.services.SendBoundary;
import edu.cmu.melle.services.SendRunning;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static edu.cmu.melle.R.id.map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Handler.Callback {

    private ProgressBar mProgressBar;
    private GoogleMap mMap;
    private boolean mMapInitialized;

    private Location mLocation;
    private Location mBoundaryTopLeft;
    private Location mBoundaryBottomRight;

    private Handler mHandler;

    private int mMapState;

    private TextView mRetrievingLocationText;
    private TextView mDrawTopLeftInstruction;
    private TextView mDrawBottomRightInstruction;
    private LinearLayout mBoundaryConfirmContainer;
    private TextView mSendingBoundaryText;
    private TextView mStartingMelleText;
    private TextView mMelleRunningText;

    private Button mBoundaryCancelButton;
    private Button mStartMelleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        mMapInitialized = false;

        mHandler = new Handler(this);

        mProgressBar = (ProgressBar) this.findViewById(R.id.progressBar);

        mRetrievingLocationText = (TextView) findViewById(R.id.retrieving_location_text);
        mDrawTopLeftInstruction = (TextView) findViewById(R.id.top_left_instruction_text);
        mDrawBottomRightInstruction = (TextView) findViewById(R.id.bottom_right_instruction_text);
        mBoundaryConfirmContainer = (LinearLayout) findViewById(R.id.boundary_confirm_container);
        mBoundaryCancelButton = (Button) findViewById(R.id.boundary_cancel_button);
        mStartMelleButton = (Button) findViewById(R.id.start_melle_button);
        mSendingBoundaryText = (TextView) findViewById(R.id.sending_boundary_text);
        mStartingMelleText = (TextView) findViewById(R.id.starting_melle_text);
        mMelleRunningText = (TextView) findViewById(R.id.melle_running_text);

        setMapState(MapState.RETRIEVING_LOCATION);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mLocation != null && !mMapInitialized) {
            setMellEMarkerOnMap();
            setMapState(MapState.DRAW_TOP_LEFT);
            mMapInitialized = true;
        }
    }

    public void setTopLeftOnClickListener() {
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                mBoundaryTopLeft = new Location(point.latitude, point.longitude);
                Marker myLocMarker = mMap.addMarker(new MarkerOptions()
                        .position(point)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_top_left))
                        .anchor(0.0f, 0.0f));
                setMapState(MapState.DRAW_BOTTOM_RIGHT);
            }
        });
    }

    public void setBottomRightOnClickListener() {
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                mBoundaryBottomRight = new Location(point.latitude, point.longitude);
                Marker myLocMarker = mMap.addMarker(new MarkerOptions()
                        .position(point)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_bottom_right))
                        .anchor(1.0f, 1.0f));
                PolygonOptions rectOptions = new PolygonOptions().add(
                        new LatLng(mBoundaryTopLeft.getLatitude(), mBoundaryTopLeft.getLongitude()),
                        new LatLng(mBoundaryBottomRight.getLatitude(), mBoundaryTopLeft.getLongitude()),
                        new LatLng(mBoundaryBottomRight.getLatitude(), mBoundaryBottomRight.getLongitude()),
                        new LatLng(mBoundaryTopLeft.getLatitude(), mBoundaryBottomRight.getLongitude()));
                mMap.addPolygon(rectOptions.strokeWidth(0).fillColor(0x3FFF0000));
                setMapState(MapState.BOUNDARY_CONFIRM);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {

            case GetLocation.MSG_FINISHED:

                if (msg.obj.getClass().equals(Location.class)) {
                    mLocation = (Location) msg.obj;
                }
                break;

        }
        return false;
    }

    private void setMapState(int state) {
        mMapState = state;
        switch (state) {
            case MapState.RETRIEVING_LOCATION:
                showInstruction(mRetrievingLocationText);
                new ProgressTask().execute();
                break;
            case MapState.DRAW_TOP_LEFT:
                setTopLeftOnClickListener();
                showInstruction(mDrawTopLeftInstruction);
                break;
            case MapState.DRAW_BOTTOM_RIGHT:
                setBottomRightOnClickListener();
                showInstruction(mDrawBottomRightInstruction);
                break;
            case MapState.BOUNDARY_CONFIRM:
                mMap.setOnMapClickListener(null);
                setStartCancelButtonListeners();
                showInstruction(mBoundaryConfirmContainer);
                break;
            case MapState.SEND_BOUNDARY:
                new ProgressTask().execute();
                showInstruction(mSendingBoundaryText);
                break;
            case MapState.STARTING_MELLE:
                new ProgressTask().execute();
                showInstruction(mStartingMelleText);
                break;
            case MapState.MELLE_RUNNING:
                showInstruction(mMelleRunningText);
                break;
        }
    }

    private void setStartCancelButtonListeners() {
        mBoundaryCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelBoundary();
            }
        });
        mStartMelleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBoundary();
            }
        });
    }

    private void sendBoundary() {
        setMapState(MapState.SEND_BOUNDARY);
    }

    private void cancelBoundary() {
        mMap.clear();
        setMellEMarkerOnMap();
        setMapState(MapState.DRAW_TOP_LEFT);
    }
    private void showInstruction(View visibleTextView) {

        mRetrievingLocationText.setVisibility(GONE);
        mDrawTopLeftInstruction.setVisibility(GONE);
        mDrawBottomRightInstruction.setVisibility(GONE);
        mBoundaryConfirmContainer.setVisibility(GONE);
        mSendingBoundaryText.setVisibility(GONE);
        mStartingMelleText.setVisibility(GONE);
        mMelleRunningText.setVisibility(GONE);

        visibleTextView.setVisibility(VISIBLE);
    }

    private void setMellEMarkerOnMap() {

        // Add a marker at the robot's location and zoom to that location
        LatLng robotLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(robotLocation, 18.0f));

        Marker myLocMarker = mMap.addMarker(new MarkerOptions()
                .position(robotLocation)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_icon)));
    }

    private class ProgressTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected void onPreExecute(){
            mProgressBar.setVisibility(VISIBLE);
            if (mMapState == MapState.RETRIEVING_LOCATION) {
                new GetLocation(mHandler).execute();
            } else if (mMapState == MapState.SEND_BOUNDARY) {
                new SendBoundary(mHandler, mBoundaryTopLeft, mBoundaryBottomRight).execute();
            } else if (mMapState == MapState.STARTING_MELLE) {
                new SendRunning(mHandler, true).execute();
            }
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            //my stuff is here
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mProgressBar.setVisibility(GONE);

            if (mMapState == MapState.RETRIEVING_LOCATION && mMap != null) {
                setMellEMarkerOnMap();
                setMapState(MapState.DRAW_TOP_LEFT);
            } else if (mMapState == MapState.SEND_BOUNDARY) {
                setMapState(MapState.STARTING_MELLE);
            } else if (mMapState == MapState.STARTING_MELLE) {
                setMapState(MapState.MELLE_RUNNING);
            }
        }
    }
}
