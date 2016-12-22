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

package com.shollmann.android.igcparser.model;

import com.shollmann.android.igcparser.util.Utilities;

import java.util.ArrayList;
import java.util.List;

public class IGCFile {
    List<BRecord> listTrackPoints;
    private double distance;
    private int maxAltitude;
    private int minAltitude;
    private String departureTime;
    private String landingTime;

    public IGCFile() {
        listTrackPoints = new ArrayList<>();
    }

    public void appendTrackPoint(BRecord bRecord) {
        listTrackPoints.add(bRecord);
    }

    public List<BRecord> getTrackPoints() {
        return listTrackPoints;
    }

    @Override
    public String toString() {
        return "IGCFile --- Track Points: "
                + listTrackPoints.size() + " :: distance (in m): " + distance
                + " :: maxAltitude: " + maxAltitude
                + " :: minAltitude: " + minAltitude;
    }


    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setMaxAltitude(int maxAltitude) {
        this.maxAltitude = maxAltitude;
    }

    public int getMaxAltitude() {
        return maxAltitude;
    }

    public void setMinAltitude(int minAltitude) {
        this.minAltitude = minAltitude;
    }

    public int getMinAltitude() {
        return this.minAltitude;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public void setLandingTime(String landingTime) {
        this.landingTime = landingTime;
    }

    public String getLandingTime() {
        return landingTime;
    }

    public String getFlightTime() {
       return Utilities.getFlightTime(departureTime, landingTime);
    }
}
