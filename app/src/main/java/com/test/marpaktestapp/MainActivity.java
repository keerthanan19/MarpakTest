package com.test.marpaktestapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerDragListener {

    private GoogleMap mMap;
    private Polygon polygon;
    private List<LatLng> shapePoints = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private List<Polyline> dividingLines = new ArrayList<>();
    private EditText rowsEditText;
    private TextView areaTextView;
    private Polyline dividingLine;
    private Marker lineStartMarker, lineEndMarker;
    private boolean drawingEnabled = false;
    private boolean lineDrawingEnabled = false;
    private boolean shapeEditable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        rowsEditText = findViewById(R.id.rows_edit_text);
        areaTextView = findViewById(R.id.area_text_view);

        Button divideButton = findViewById(R.id.divide_button);
        divideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                divideShape();
            }
        });

        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        Button drawShapeButton = findViewById(R.id.draw_shape_button);
        drawShapeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableDrawingMode();
            }
        });

        Button drawLineButton = findViewById(R.id.draw_line_button);
        drawLineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableLineDrawingMode();
            }
        });

        Button editShapeButton = findViewById(R.id.edit_shape_button);
        editShapeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableShapeEditMode();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerDragListener(this);

        // Move the camera to a default location
        LatLng defaultLocation = new LatLng(6.9271, 79.8612);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (drawingEnabled) {
            if (shapePoints.size() >= 3 && shapePoints.get(0).equals(latLng)) {
                // Close the shape
                drawPolygon();
                disableDrawingMode();
            } else {
                // Add point to shape
                shapePoints.add(latLng);
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                markers.add(marker);
                updatePolygon();
            }
        } else if (lineDrawingEnabled) {
            if (lineStartMarker == null) {
                lineStartMarker = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
            } else if (lineEndMarker == null) {
                lineEndMarker = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                dividingLine = mMap.addPolyline(new PolylineOptions()
                        .add(lineStartMarker.getPosition(), lineEndMarker.getPosition())
                        .color(Color.RED));
                lineDrawingEnabled = false;
                mMap.getUiSettings().setScrollGesturesEnabled(true); // Re-enable scrolling
            }
        } else if (shapeEditable) {
            // Modify shape point
            modifyShapePoint(latLng);
        }
    }

    private void drawPolygon() {
        if (polygon != null) {
            polygon.remove();
        }
        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(shapePoints)
                .strokeColor(Color.BLUE)
                .fillColor(0x7F00FF00);
        polygon = mMap.addPolygon(polygonOptions);

        calculateAndDisplayArea();
        highlightShape();
    }

    private void highlightShape() {
        for (Polyline line : dividingLines) {
            line.remove();
        }
        dividingLines.clear();

        for (int i = 0; i < shapePoints.size(); i++) {
            LatLng point1 = shapePoints.get(i);
            LatLng point2 = shapePoints.get((i + 1) % shapePoints.size());
            Polyline line = mMap.addPolyline(new PolylineOptions().add(point1, point2).color(Color.BLUE));
            dividingLines.add(line);
        }
    }

    private void updatePolygon() {
        if (polygon != null) {
            polygon.remove();
        }
        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(shapePoints)
                .strokeColor(Color.BLUE)
                .fillColor(0x7F00FF00);
        polygon = mMap.addPolygon(polygonOptions);

        calculateAndDisplayArea();
        highlightShape();
    }

    private void calculateAndDisplayArea() {
        if (polygon == null) return;
        List<LatLng> points = polygon.getPoints();
        double area = calculatePolygonArea(points);
        areaTextView.setText("Area: " + String.format("%.2f", area) + " sq. meters");
    }

    private double calculatePolygonArea(List<LatLng> points) {
        double area = 0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            LatLng p1 = points.get(i);
            LatLng p2 = points.get((i + 1) % n);
            area += p1.latitude * p2.longitude - p2.latitude * p1.longitude;
        }
        return Math.abs(area / 2.0 * 111000 * 111000); // Approximate conversion to square meters
    }

    private void divideShape() {
        if (polygon != null) {
            List<LatLng> vertices = polygon.getPoints();
            if (vertices.size() < 3) return;

            String rowsText = rowsEditText.getText().toString();
            if (rowsText.isEmpty()) {
                Toast.makeText(this, "Please enter the number of rows to divide", Toast.LENGTH_SHORT).show();
                return;
            }

            int rows = Integer.parseInt(rowsText);

            // Clear existing dividing lines
            clearDividingLines();

            LatLng start = lineStartMarker.getPosition();
            LatLng end = lineEndMarker.getPosition();

            // Calculate unit vector components
            double dx = (end.latitude - start.latitude) / (rows + 1);
            double dy = (end.longitude - start.longitude) / (rows + 1);

            for (int i = 1; i <= rows; i++) {
                double lat = start.latitude + i * dx;
                double lng = start.longitude + i * dy;

                LatLng point1 = new LatLng(lat, lng);

                // Find intersection points with the polygon's edges
                List<LatLng> intersections = findIntersections(vertices, start, end, point1);

                if (intersections.size() == 2) {
                    LatLng point2 = intersections.get(1);

                    // Draw the dividing line
                    Polyline line = mMap.addPolyline(new PolylineOptions()
                            .add(point1, point2)
                            .color(Color.RED)); // Change Color.RED to your desired color
                    dividingLines.add(line);
                }
            }
        }
    }

    private List<LatLng> findIntersections(List<LatLng> vertices, LatLng start, LatLng end, LatLng point) {
        List<LatLng> intersections = new ArrayList<>();
        for (int j = 0; j < vertices.size(); j++) {
            LatLng p1 = vertices.get(j);
            LatLng p2 = vertices.get((j + 1) % vertices.size());
            LatLng intersection = computeIntersection(start, end, p1, p2);
            if (intersection != null && isPointOnSegment(p1, p2, intersection) && isPointOnSegment(start, end, intersection)) {
                intersections.add(intersection);
            }
        }
        return intersections;
    }

    private LatLng computeIntersection(LatLng start1, LatLng end1, LatLng start2, LatLng end2) {
        double x1 = start1.longitude;
        double y1 = start1.latitude;
        double x2 = end1.longitude;
        double y2 = end1.latitude;
        double x3 = start2.longitude;
        double y3 = start2.latitude;
        double x4 = end2.longitude;
        double y4 = end2.latitude;

        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (denom == 0) {
            return null; // Lines are parallel
        }

        double intersectX = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denom;
        double intersectY = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denom;

        LatLng intersection = new LatLng(intersectY, intersectX);
        if (isPointOnSegment(start1, end1, intersection) && isPointOnSegment(start2, end2, intersection)) {
            return intersection;
        } else {
            return null;
        }
    }

    private boolean isPointOnSegment(LatLng start, LatLng end, LatLng point) {
        double minX = Math.min(start.latitude, end.latitude);
        double maxX = Math.max(start.latitude, end.latitude);
        double minY = Math.min(start.longitude, end.longitude);
        double maxY = Math.max(start.longitude, end.longitude);

        return point.latitude >= minX && point.latitude <= maxX
                && point.longitude >= minY && point.longitude <= maxY;
    }

    private void clearDividingLines() {
        for (Polyline line : dividingLines) {
            line.remove();
        }
        dividingLines.clear();
    }

    private void clearAll() {
        if (polygon != null) {
            polygon.remove();
            polygon = null;
        }
        clearDividingLines();
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();
        if (dividingLine != null) {
            dividingLine.remove();
            dividingLine = null;
        }
        shapePoints.clear();
        areaTextView.setText("Area: ");
        if (lineStartMarker != null) {
            lineStartMarker.remove();
            lineStartMarker = null;
        }
        if (lineEndMarker != null) {
            lineEndMarker.remove();
            lineEndMarker = null;
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        // No action needed here
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        // No action needed here
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        int index = markers.indexOf(marker);
        if (index != -1) {
            shapePoints.set(index, marker.getPosition());
            drawPolygon();
        } else if (marker.equals(lineStartMarker) || marker.equals(lineEndMarker)) {
            if (dividingLine != null) {
                dividingLine.remove();
            }
            dividingLine = mMap.addPolyline(new PolylineOptions()
                    .add(lineStartMarker.getPosition(), lineEndMarker.getPosition())
                    .color(Color.RED));
        }
    }

    private void enableDrawingMode() {
        drawingEnabled = true;
        lineDrawingEnabled = false;
        shapeEditable = false;
        clearAll(); // Clear any existing shapes
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        Toast.makeText(this, "Tap on the map to draw the shape. Tap the first point again to finish.", Toast.LENGTH_LONG).show();
    }

    private void disableDrawingMode() {
        drawingEnabled = false;
        mMap.getUiSettings().setScrollGesturesEnabled(true);
    }

    private void enableLineDrawingMode() {
        lineDrawingEnabled = true;
        drawingEnabled = false;
        shapeEditable = false;
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        Toast.makeText(this, "Tap on the map to set the start and end points of the line.", Toast.LENGTH_LONG).show();
    }

    private void enableShapeEditMode() {
        shapeEditable = true;
        drawingEnabled = false;
        lineDrawingEnabled = false;
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        Toast.makeText(this, "Drag the vertices of the shape to modify.", Toast.LENGTH_LONG).show();
    }

    private void modifyShapePoint(LatLng latLng) {
        if (polygon == null || shapePoints.isEmpty()) return;

        // Find the nearest marker to the clicked point
        Marker nearestMarker = null;
        double minDistance = Double.MAX_VALUE;
        for (Marker marker : markers) {
            double distance = distanceBetweenPoints(marker.getPosition(), latLng);
            if (distance < minDistance) {
                minDistance = distance;
                nearestMarker = marker;
            }
        }

        if (nearestMarker != null) {
            int index = markers.indexOf(nearestMarker);
            if (index != -1) {
                shapePoints.set(index, latLng);
                markers.get(index).setPosition(latLng);
                updatePolygon();
            }
        }
    }

    private double distanceBetweenPoints(LatLng point1, LatLng point2) {
        double lat1 = point1.latitude;
        double lon1 = point1.longitude;
        double lat2 = point2.latitude;
        double lon2 = point2.longitude;

        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344 * 1000; // In meters

        return dist;
    }


}
