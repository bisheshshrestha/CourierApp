package com.divyagyan.clientapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText editTextTrackingNumber;
    private Button buttonTrack;
    private TextView orderDetailsContent;
    private TextView orderStatusContent;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextTrackingNumber = findViewById(R.id.editTextTrackingNumber);
        buttonTrack = findViewById(R.id.buttonTrack);
        orderDetailsContent = findViewById(R.id.orderDetailsContent);
        orderStatusContent = findViewById(R.id.orderStatusContent);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getView().setVisibility(View.GONE);
            mapFragment.getMapAsync(this);
        }

        buttonTrack.setOnClickListener(v -> {
            Log.d(TAG, "Track button clicked.");
            String trackingNumber = editTextTrackingNumber.getText().toString().trim();
            if (!trackingNumber.isEmpty()) {
                fetchOrderDetails(trackingNumber);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a tracking number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOrderDetails(String trackingNumber) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        Log.d(TAG, "Fetching order details for tracking number: " + trackingNumber);

        ordersRef.orderByChild("trackingNumber").equalTo(trackingNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Log.d(TAG, "Order found in database.");
                            findViewById(R.id.cardOrderDetails).setVisibility(View.VISIBLE);  // Show Order Details card
                            findViewById(R.id.cardOrderStatus).setVisibility(View.VISIBLE);  // Show Order Status card
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                                displayOrderDetails(snapshot.getKey(), orderData);
                                showLocationsOnMap(orderData);
                            }
                        } else {
                            Log.d(TAG, "Order not found.");
                            Toast.makeText(MainActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error fetching order details: " + databaseError.getMessage());
                        Toast.makeText(MainActivity.this, "Failed to fetch order details", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayOrderDetails(String orderId, Map<String, Object> orderData) {
        StringBuilder details = new StringBuilder();
        details.append("Package Details: ").append(orderData.get("packageDetails")).append("\n");
        details.append("Recipient Name: ").append(orderData.get("recipientName")).append("\n");
        details.append("Recipient Phone: ").append(orderData.get("recipientPhone")).append("\n");
        details.append("Price: Rs ").append(orderData.get("price")).append("\n");

        orderDetailsContent.setText(details.toString());

        fetchLatestStatus(orderId);
    }

    private void fetchLatestStatus(String orderId) {
        DatabaseReference statusHistoryRef = FirebaseDatabase.getInstance().getReference("orders")
                .child(orderId).child("statusHistory");

        statusHistoryRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    StringBuilder statusDetails = new StringBuilder();
                    for (DataSnapshot statusSnapshot : snapshot.getChildren()) {
                        String status = statusSnapshot.child("status").getValue(String.class);
                        statusDetails.append("Status: ").append(status).append("\n");
                    }
                    orderStatusContent.setText(statusDetails.toString());
                } else {
                    orderStatusContent.setText("Status: Not available\n");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load order status: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Failed to load order status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLocationsOnMap(Map<String, Object> orderData) {
        if (mMap != null) {
            mapFragment.getView().setVisibility(View.VISIBLE);
            findViewById(R.id.cardMap).setVisibility(View.VISIBLE);  // Show Map card
            mMap.clear();

            Map<String, Object> pickupLocation = (Map<String, Object>) orderData.get("pickupLocation");
            LatLng pickupLatLng = null;
            if (pickupLocation != null) {
                double pickupLat = (double) pickupLocation.get("latitude");
                double pickupLng = (double) pickupLocation.get("longitude");
                pickupLatLng = new LatLng(pickupLat, pickupLng);
                mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }

            Map<String, Object> deliveryLocation = (Map<String, Object>) orderData.get("deliveryLocation");
            LatLng deliveryLatLng = null;
            if (deliveryLocation != null) {
                double deliveryLat = (double) deliveryLocation.get("latitude");
                double deliveryLng = (double) deliveryLocation.get("longitude");
                deliveryLatLng = new LatLng(deliveryLat, deliveryLng);
                mMap.addMarker(new MarkerOptions().position(deliveryLatLng).title("Delivery Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }

            if (pickupLatLng != null && deliveryLatLng != null) {
                mMap.addPolyline(new PolylineOptions().add(pickupLatLng, deliveryLatLng)
                        .width(5).color(ContextCompat.getColor(this, R.color.pure_courier_text)));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(pickupLatLng);
                builder.include(deliveryLatLng);
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }
}
