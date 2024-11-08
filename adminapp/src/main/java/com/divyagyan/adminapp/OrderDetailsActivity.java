package com.divyagyan.adminapp;

import static java.lang.Double.parseDouble;

import android.content.Intent;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private DatabaseReference orderRef;
    private String orderId;

    private TextView labelOrderId, labelPrice;
    private EditText editTextPackageDetails, editTextRecipientName, editTextRecipientPhone;
    private Button buttonPickupComplete, buttonSentForDelivery, buttonDelivered, updateButton;
    private GoogleMap mMap;
    private LatLng pickupLocation, deliveryLocation;
    private String currentStatus = ""; // To keep track of the latest status update

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        labelPrice = findViewById(R.id.label_price);
        editTextPackageDetails = findViewById(R.id.editTextPackageDetails);
        editTextRecipientName = findViewById(R.id.editTextRecipientName);
        editTextRecipientPhone = findViewById(R.id.editTextRecipientPhone);
        buttonPickupComplete = findViewById(R.id.buttonPickupComplete);
        buttonSentForDelivery = findViewById(R.id.buttonSentForDelivery);
        buttonDelivered = findViewById(R.id.buttonDelivered);
        updateButton = findViewById(R.id.updateButton);

        orderId = getIntent().getStringExtra("orderId");
        Log.d("OrderDetailsActivity", "Received Order ID: " + orderId);

        if (orderId != null) {
            orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Order ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_order_details);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        buttonPickupComplete.setOnClickListener(v -> {
            currentStatus = "Pickup Complete";
            buttonPickupComplete.setVisibility(View.GONE);
        });

        buttonSentForDelivery.setOnClickListener(v -> {
            currentStatus = "Sent For Delivery";
            buttonSentForDelivery.setVisibility(View.GONE);
        });

        buttonDelivered.setOnClickListener(v -> {
            currentStatus = "Delivered";
            buttonDelivered.setVisibility(View.GONE);
        });

        updateButton.setOnClickListener(v -> saveFinalOrderDetails());
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("OrderDetailsActivity", "Data snapshot: " + dataSnapshot.toString());
                if (dataSnapshot.exists()) {
                    // Get basic details as Strings
                    String packageDetails = dataSnapshot.child("packageDetails").getValue(String.class);
                    String recipientName = dataSnapshot.child("recipientName").getValue(String.class);
                    String recipientPhone = dataSnapshot.child("recipientPhone").getValue(String.class);

                    // Handle the "price" field with simple try-catch for conversion
                    String priceString;
                    try {
                        priceString = String.format(Locale.getDefault(), "%.2f", dataSnapshot.child("price").getValue(Double.class));
                    } catch (Exception e) {
                        priceString = dataSnapshot.child("price").getValue(String.class);
                    }

                    if (priceString != null) {
                        labelPrice.setText("Price: Rs " + priceString);
                    }

                    // Set text fields
                    editTextPackageDetails.setText(packageDetails);
                    editTextRecipientName.setText(recipientName);
                    editTextRecipientPhone.setText(recipientPhone);

                    // Handle pickup and delivery locations with try-catch
                    Double pickupLat = null, pickupLng = null, deliveryLat = null, deliveryLng = null;
                    try {
                        pickupLat = dataSnapshot.child("pickupLocation/latitude").getValue(Double.class);
                        pickupLng = dataSnapshot.child("pickupLocation/longitude").getValue(Double.class);
                        deliveryLat = dataSnapshot.child("deliveryLocation/latitude").getValue(Double.class);
                        deliveryLng = dataSnapshot.child("deliveryLocation/longitude").getValue(Double.class);
                    } catch (Exception e) {
                        Log.e("OrderDetailsActivity", "Failed to read location coordinates", e);
                    }

                    // Set pickup and delivery locations if they are not null
                    if (pickupLat != null && pickupLng != null) {
                        pickupLocation = new LatLng(pickupLat, pickupLng);
                    }
                    if (deliveryLat != null && deliveryLng != null) {
                        deliveryLocation = new LatLng(deliveryLat, deliveryLng);
                    }

                    // Get the current status and update UI
                    currentStatus = dataSnapshot.child("status").getValue(String.class);
                    updateButtonVisibility();

                    // Show markers on the map if both locations are available
                    if (mMap != null && pickupLocation != null && deliveryLocation != null) {
                        showMarkersOnMap(pickupLocation, deliveryLocation);
                    }
                } else {
                    Toast.makeText(OrderDetailsActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("OrderDetailsActivity", "Failed to load order data", error.toException());
            }
        });
    }



    private void showMarkersOnMap(LatLng pickup, LatLng delivery) {
        if (mMap != null) {
            mMap.clear();

            // Add markers for pickup and delivery locations
            mMap.addMarker(new MarkerOptions().position(pickup).title("Pickup Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            mMap.addMarker(new MarkerOptions().position(delivery).title("Delivery Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Draw a polyline between the pickup and delivery locations
            mMap.addPolyline(new PolylineOptions()
                    .add(pickup, delivery)
                    .width(5) // Line width
                    .color(ContextCompat.getColor(this, R.color.pure_courier_text))); // Customize the color

            // Move the camera to show both locations with a zoom level that fits both markers
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(pickup);
            builder.include(delivery);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        }
    }


    private void saveFinalOrderDetails() {
        String updatedPackageDetails = editTextPackageDetails.getText().toString().trim();
        String updatedRecipientName = editTextRecipientName.getText().toString().trim();
        String updatedRecipientPhone = editTextRecipientPhone.getText().toString().trim();
        String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("packageDetails", updatedPackageDetails);
        updatedData.put("recipientName", updatedRecipientName);
        updatedData.put("recipientPhone", updatedRecipientPhone);
        updatedData.put("status", currentStatus); // Save the latest status to the database
        updatedData.put("lastUpdated", timestamp);

        // Add status to status history if a status update was made
        if (!currentStatus.isEmpty()) {
            DatabaseReference statusHistoryRef = orderRef.child("statusHistory").push();
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("status", currentStatus);
            statusUpdate.put("timestamp", timestamp);
            statusHistoryRef.setValue(statusUpdate);
        }

        // Update main order data
        orderRef.updateChildren(updatedData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Order details updated successfully", Toast.LENGTH_SHORT).show();
                // Redirect to OrderActivity
                Intent intent = new Intent(OrderDetailsActivity.this, OrderActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to update order details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateButtonVisibility() {
        // Hide buttons based on the current status
        if ("Pickup Complete".equals(currentStatus)) {
            buttonPickupComplete.setVisibility(View.GONE);
            buttonSentForDelivery.setVisibility(View.VISIBLE);
            buttonDelivered.setVisibility(View.VISIBLE);
        }
        if ("Sent For Delivery".equals(currentStatus)) {
            buttonPickupComplete.setVisibility(View.GONE);
            buttonSentForDelivery.setVisibility(View.GONE);
            buttonDelivered.setVisibility(View.VISIBLE);
        }
        if ("Delivered".equals(currentStatus)) {
            buttonPickupComplete.setVisibility(View.GONE);
            buttonSentForDelivery.setVisibility(View.GONE);
            buttonDelivered.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (pickupLocation != null && deliveryLocation != null) {
            showMarkersOnMap(pickupLocation, deliveryLocation);
        }
    }
}
