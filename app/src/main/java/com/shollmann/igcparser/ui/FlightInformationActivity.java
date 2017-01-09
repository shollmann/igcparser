/*
 * MIT License
 *
 * Copyright (c) 2016 Santiago Hollmann
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.shollmann.igcparser.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.shollmann.android.igcparser.Parser;
import com.shollmann.android.igcparser.model.CRecordWayPoint;
import com.shollmann.android.igcparser.model.IGCFile;
import com.shollmann.android.igcparser.model.ILatLonRecord;
import com.shollmann.android.igcparser.util.Utilities;
import com.shollmann.igcparser.R;
import com.shollmann.igcparser.tracking.TrackerHelper;
import com.shollmann.igcparser.util.Constants;
import com.shollmann.igcparser.util.ResourcesHelper;

import java.util.List;


public class FlightInformationActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {
    private boolean isFinishReplay = true;
    private int duration;
    private int replaySpeed = Constants.Map.DEFAULT_REPLAY_SPEED;
    private String fileToLoadPath;
    private IGCFile igcFile;
    private List<LatLng> listLatLngPoints;
    private MapView mapView;
    private GoogleMap googleMap;
    private Marker markerGlider;
    private TextView txtDistance;
    private TextView txtMaxAltitude;
    private TextView txtMinAltitude;
    private TextView txtTakeOffTime;
    private TextView txtLandingTime;
    private TextView txtFlightTime;
    private TextView txtPilot;
    private TextView txtGlider;
    private LinearLayout layoutPilot;
    private LinearLayout layoutGlider;
    private View btnCloseInformation;
    private View btnShowInformation;
    private ImageView btnPlay;
    private ImageView btnFastForward;
    private CardView cardviewInformation;
    private ProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        handleIntent();
        setClickListeners();
        initMap(savedInstanceState);
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            fileToLoadPath = (String) getIntent().getExtras().get(Constants.FILE_TO_LOAD_PATH);
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            fileToLoadPath = uri.getPath();
        }
    }

    private void initMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void setClickListeners() {
        btnCloseInformation.setOnClickListener(this);
        btnShowInformation.setOnClickListener(this);
    }

    private void findViews() {
        setContentView(R.layout.activity_flight_information);
        mapView = (MapView) findViewById(R.id.main_map);
        txtDistance = (TextView) findViewById(R.id.main_txt_distance);
        txtMaxAltitude = (TextView) findViewById(R.id.main_txt_max_altitude);
        txtMinAltitude = (TextView) findViewById(R.id.main_txt_start_altitude);
        txtTakeOffTime = (TextView) findViewById(R.id.main_txt_takeoff);
        txtLandingTime = (TextView) findViewById(R.id.main_txt_landing);
        txtFlightTime = (TextView) findViewById(R.id.main_txt_duration);
        txtGlider = (TextView) findViewById(R.id.main_txt_glider);
        txtPilot = (TextView) findViewById(R.id.main_txt_pilot);
        layoutGlider = (LinearLayout) findViewById(R.id.main_layout_glider);
        layoutPilot = (LinearLayout) findViewById(R.id.main_layout_pilot);
        cardviewInformation = (CardView) findViewById(R.id.main_cardview_information);
        loading = (ProgressBar) findViewById(R.id.main_loading);
        btnCloseInformation = findViewById(R.id.main_cardview_close);
        btnShowInformation = findViewById(R.id.main_information_btn);
        btnPlay = (ImageView) findViewById(R.id.main_btn_play);
        btnFastForward = (ImageView) findViewById(R.id.main_btn_speed_up);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setMapToolbarEnabled(false);
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.getUiSettings().setZoomGesturesEnabled(true);
        this.googleMap.getUiSettings().setRotateGesturesEnabled(false);

        new ParseIGCFileAsynkTask().execute();
    }

    private void displayFlightInformation() {
        txtDistance.setText(String.format(getString(R.string.information_distance), String.valueOf(Utilities.getFormattedNumber((int) (igcFile.getDistance() / Constants.Map.METERS_IN_ONE_KILOMETER), getResources().getConfiguration().locale))));
        txtMaxAltitude.setText(String.format(getString(R.string.information_max_altitude), Utilities.getFormattedNumber(igcFile.getMaxAltitude(), getResources().getConfiguration().locale)));
        txtMinAltitude.setText(String.format(getString(R.string.information_min_altitude), Utilities.getFormattedNumber(igcFile.getMinAltitude(), getResources().getConfiguration().locale)));
        txtLandingTime.setText(String.format(getString(R.string.information_landing), Utilities.getTimeHHMM(igcFile.getLandingTime())));
        txtTakeOffTime.setText(String.format(getString(R.string.information_takeoff), Utilities.getTimeHHMM(igcFile.getTakeOffTime()), igcFile.getDate()));
        txtFlightTime.setText(String.format(getString(R.string.information_duration), igcFile.getFlightTime()));
        displayPilot();
        displayGliderInformation();
    }

    private void displayGliderInformation() {
        final String gliderTypeAndId = igcFile.getGliderTypeAndId();
        if (!TextUtils.isEmpty(gliderTypeAndId)) {
            layoutGlider.setVisibility(View.VISIBLE);
            txtGlider.setText(String.format(getResources().getString(R.string.information_glider), gliderTypeAndId));
        }
    }

    private void displayPilot() {
        if (!TextUtils.isEmpty(igcFile.getPilotInCharge())) {
            layoutPilot.setVisibility(View.VISIBLE);
            txtPilot.setText(String.format(getResources().getString(R.string.information_pilot), Utilities.capitalizeText(igcFile.getPilotInCharge())));
        }
    }

    private void displayTrack() {
        PolylineOptions polyline = new PolylineOptions().width(ResourcesHelper.getDimensionPixelSize(R.dimen.map_line_width)).color(Color.BLUE);
        listLatLngPoints = Utilities.getLatLngPoints(igcFile.getTrackPoints());
        polyline.addAll(listLatLngPoints);
        googleMap.addPolyline(polyline);
    }

    private void displayWayPoints() {
        if (!igcFile.getWaypoints().isEmpty()) {
            PolylineOptions polyline = new PolylineOptions().width(ResourcesHelper.getDimensionPixelSize(R.dimen.map_line_width)).color(Color.RED);
            listLatLngPoints = Utilities.getLatLngPoints(igcFile.getWaypoints());
            polyline.addAll(listLatLngPoints);
            googleMap.addPolyline(polyline);
        }

        for (ILatLonRecord wayPoint : igcFile.getWaypoints()) {
            if (wayPoint.getLatLon().getLat() != 0 && wayPoint.getLatLon().getLat() != 0) {
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(wayPoint.getLatLon().getLat(), wayPoint.getLatLon().getLon()))
                        .draggable(false)
                        .title(((CRecordWayPoint) wayPoint).getDescription()));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_cardview_close:
                cardviewInformation.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_dae_out));
                cardviewInformation.setVisibility(View.GONE);
                btnShowInformation.setVisibility(View.VISIBLE);
                btnShowInformation.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_information_btn_enter));
                TrackerHelper.trackCloseInformation();
                break;
            case R.id.main_information_btn:
                btnShowInformation.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_information_btn_leave));
                btnShowInformation.setVisibility(View.GONE);
                cardviewInformation.setVisibility(View.VISIBLE);
                cardviewInformation.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_fade_in));
                TrackerHelper.trackOpenInformation();
                break;
        }
    }

    private class ParseIGCFileAsynkTask extends AsyncTask<Void, Void, Void> implements View.OnClickListener {
        protected Void doInBackground(Void... something) {
            igcFile = Parser.parse(Uri.parse(fileToLoadPath));
            return null;
        }

        protected void onProgressUpdate(Void... something) {
        }

        protected void onPostExecute(Void result) {
            handleIGCFileLoaded();
        }

        private void handleIGCFileLoaded() {
            displayWayPoints();
            displayTrack();
            displayFlightInformation();
            displayReplayViews();
            mapView.setVisibility(View.VISIBLE);
            cardviewInformation.setVisibility(View.VISIBLE);
            loading.setVisibility(View.GONE);
        }

        private void displayReplayViews() {
            if (listLatLngPoints != null && !listLatLngPoints.isEmpty()) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(listLatLngPoints.get(0).latitude + Constants.Map.FIX_INITIAL_LATITUDE, listLatLngPoints.get(0).longitude), Constants.Map.MAP_DEFAULT_ZOOM));
                markerGlider = googleMap.addMarker(new MarkerOptions()
                        .position(listLatLngPoints.get(0))
                        .zIndex(1.0f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_glider))
                );
                setReplayButtons();
                TrackerHelper.trackFlightDisplayed();
            }
        }

        private void setReplayButtons() {
            btnPlay.setOnClickListener(this);
            btnFastForward.setOnClickListener(this);

            btnPlay.setImageResource(R.drawable.ic_play_arrow);
            btnFastForward.setImageResource(R.drawable.ic_fast_forward);

            btnPlay.setVisibility(View.VISIBLE);
            btnFastForward.setVisibility(View.GONE);

        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.main_btn_play:
                    startFlightReplay();
                    break;
                case R.id.main_btn_speed_up:
                    speedUpReplay();
                    break;
            }
        }

    }

    private void speedUpReplay() {
        if (replaySpeed >= Constants.Map.MAX_REPLAY_SPEED) {
            replaySpeed = (int) (replaySpeed / Constants.Map.REPLAY_SPEED_INCREASER);
            TrackerHelper.trackFastForwardFlight();
        }
    }

    public void startFlightReplay() {
        if (listLatLngPoints == null || listLatLngPoints.isEmpty()) {
            return;
        }
        isFinishReplay = !isFinishReplay;
        if (!isFinishReplay) {
            TrackerHelper.trackPlayFlight();
            btnPlay.setImageResource(R.drawable.ic_stop);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerGlider.getPosition(), googleMap.getCameraPosition().zoom));
            animateMarker();
            btnFastForward.setVisibility(View.VISIBLE);
        } else {
            TrackerHelper.trackStopFlight();
        }
    }


    private void animateMarker() {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        duration = 300000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            int i = 0;

            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                if (i < listLatLngPoints.size()) {
                    Double heading = SphericalUtil.computeHeading(markerGlider.getPosition(), listLatLngPoints.get(i));
                    markerGlider.setRotation(heading.floatValue());
                    markerGlider.setPosition(listLatLngPoints.get(i));
                }
                i++;

                if (t < 1.0 && i < listLatLngPoints.size() && !isFinishReplay) {
                    handler.postDelayed(this, replaySpeed);
                } else {
                    finishReplay();
                }
            }
        });
    }

    private void finishReplay() {
        markerGlider.setPosition(listLatLngPoints.get(0));
        markerGlider.setRotation(0);
        btnFastForward.setVisibility(View.GONE);
        replaySpeed = Constants.Map.DEFAULT_REPLAY_SPEED;
        btnPlay.setImageResource(R.drawable.ic_play_arrow);
        isFinishReplay = true;
    }

    @Override
    protected void onStop() {
        isFinishReplay = true;
        super.onStop();
    }
}
