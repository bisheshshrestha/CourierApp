package com.divyagyan.adminapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.divyagyan.adminapp.databinding.ActivityOrderDetailsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
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

public class OrderDetailsActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    private ActivityOrderDetailsBinding binding;
    private GoogleMap mMap;
    private LatLng pickupLocation, deliveryLocation;
    private String orderId;
    private DatabaseReference orderRef;
    private String currentStatus = ""; // To keep track of the latest status update

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        allocateActivityTitle("Order Details");

        orderId = getIntent().getStringExtra("orderId");
        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_order_details);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadOrderDetails();

        binding.buttonPickupComplete.setOnClickListener(v -> showStatusConfirmationDialog("Pickup Complete"));
        binding.buttonSentForDelivery.setOnClickListener(v -> showStatusConfirmationDialog("Sent For Delivery"));
        binding.buttonDelivered.setOnClickListener(v -> showStatusConfirmationDialog("Delivered"));
        binding.updateButton.setOnClickListener(v -> saveFinalOrderDetails());

    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("OrderDetailsActivity", "Data snapshot: " + dataSnapshot.toString());
                if (dataSnapshot.exists()) {
                    String packageDetails = dataSnapshot.child("packageDetails").getValue(String.class);
                    String recipientName = dataSnapshot.child("recipientName").getValue(String.class);
                    String recipientPhone = dataSnapshot.child("recipientPhone").getValue(String.class);
                    // Handling the price value safely
                    Object priceValue = dataSnapshot.child("price").getValue();
                    Object distanceValue = dataSnapshot.child("distance").getValue();

                    double price = 0.0;
                    double distance = 0.0;
                    if (priceValue instanceof String) {
                        try {
                            price = Double.parseDouble((String) priceValue);
                        } catch (NumberFormatException e) {
                            Log.e("OrderDetailsActivity", "Invalid price format", e);
                        }
                    } else if (priceValue instanceof Double) {
                        price = (Double) priceValue;
                    }

                    // Parse distance value safely
                    if (distanceValue instanceof String) {
                        try {
                            distance = Double.parseDouble((String) distanceValue);
                        } catch (NumberFormatException e) {
                            Log.e("OrderDetailsActivity", "Invalid distance format", e);
                        }
                    } else if (distanceValue instanceof Double) {
                        distance = (Double) distanceValue;
                    }
                    String priceString = String.format(Locale.getDefault(), "%.2f", price);
                    String distanceString = String.format(Locale.getDefault(), "%.2f", distance);

                    binding.labelPrice.setText("Price: Rs " + priceString);
                    binding.distanceText.setText("Distance: " + distanceString + " km");
                    binding.editTextPackageDetails.setText(packageDetails);
                    binding.editTextRecipientName.setText(recipientName);
                    binding.editTextRecipientPhone.setText(recipientPhone);

                    Double pickupLat = dataSnapshot.child("pickupLocation/latitude").getValue(Double.class);
                    Double pickupLng = dataSnapshot.child("pickupLocation/longitude").getValue(Double.class);
                    Double deliveryLat = dataSnapshot.child("deliveryLocation/latitude").getValue(Double.class);
                    Double deliveryLng = dataSnapshot.child("deliveryLocation/longitude").getValue(Double.class);

                    if (pickupLat != null && pickupLng != null) {
                        pickupLocation = new LatLng(pickupLat, pickupLng);
                    }
                    if (deliveryLat != null && deliveryLng != null) {
                        deliveryLocation = new LatLng(deliveryLat, deliveryLng);
                    }

                    currentStatus = dataSnapshot.child("status").getValue(String.class);
                    updateButtonVisibility();

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
            mMap.clear(); // Clear existing markers

            // If both locations are null, just return after clearing
            if (pickup == null && delivery == null) {
                return;
            }

            // Add markers only if locations exist
            if (pickup != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(pickup)
                        .title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
            if (delivery != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(delivery)
                        .title("Delivery Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }

            // Draw polyline only if both locations exist
            if (pickup != null && delivery != null) {
                mMap.addPolyline(new PolylineOptions()
                        .add(pickup, delivery)
                        .width(5)
                        .color(ContextCompat.getColor(this, R.color.pure_courier_text)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, 12));
            }
        }
    }


    private void showStatusConfirmationDialog(String status) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Status Update")
                .setMessage("Do you want to set the status to " + status + "?")
                .setPositiveButton("Yes", (dialog, which) -> updateOrderStatus(status))
                .setNegativeButton("No", null)
                .show();
    }

    private void updateOrderStatus(String status) {
        currentStatus = status;
        updateButtonVisibility();
        Toast.makeText(this, status + " updated.", Toast.LENGTH_SHORT).show();
    }

    private void saveFinalOrderDetails() {
        String updatedPackageDetails = binding.editTextPackageDetails.getText().toString().trim();
        String updatedRecipientName = binding.editTextRecipientName.getText().toString().trim();
        String updatedRecipientPhone = binding.editTextRecipientPhone.getText().toString().trim();
        String timestamp = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(new Date());

        // Calculate the updated distance and price
        double distance = 0.0;
        double price = 0.0;
        if (pickupLocation != null && deliveryLocation != null) {
            distance = calculateDistance(pickupLocation, deliveryLocation);
            price = calculateDeliveryPrice(distance);
        }

        // Format distance and price
        String formattedDistance = String.format(Locale.getDefault(), "%.2f", distance);
        String formattedPrice = String.format(Locale.getDefault(), "%.2f", price);

        // Prepare data to update, including pickup and delivery locations
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("packageDetails", updatedPackageDetails);
        updatedData.put("recipientName", updatedRecipientName);
        updatedData.put("recipientPhone", updatedRecipientPhone);
        updatedData.put("status", currentStatus);
        updatedData.put("distance", formattedDistance);
        updatedData.put("price", formattedPrice);

        // Save the updated pickup and delivery locations
        if (pickupLocation != null) {
            Map<String, Object> pickupData = new HashMap<>();
            pickupData.put("latitude", pickupLocation.latitude);
            pickupData.put("longitude", pickupLocation.longitude);
            updatedData.put("pickupLocation", pickupData);
        }

        if (deliveryLocation != null) {
            Map<String, Object> deliveryData = new HashMap<>();
            deliveryData.put("latitude", deliveryLocation.latitude);
            deliveryData.put("longitude", deliveryLocation.longitude);
            updatedData.put("deliveryLocation", deliveryData);
        }

        // Check the last status in statusHistory before adding a new entry
        DatabaseReference statusHistoryRef = orderRef.child("statusHistory");
        statusHistoryRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastStatus = "";
                for (DataSnapshot child : snapshot.getChildren()) {
                    lastStatus = child.child("status").getValue(String.class);
                }

                // Only add a new status if it is different from the last status
                if (!currentStatus.equals(lastStatus)) {
                    Map<String, Object> statusUpdate = new HashMap<>();
                    statusUpdate.put("status", currentStatus);
                    statusUpdate.put("timestamp", timestamp);
                    statusHistoryRef.push().setValue(statusUpdate);
                }

                // Update main order data in Firebase
                orderRef.updateChildren(updatedData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(OrderDetailsActivity.this, "Order details updated successfully", Toast.LENGTH_SHORT).show();
                        // Redirect to OrderActivity
                        Intent intent = new Intent(OrderDetailsActivity.this, OrderActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(OrderDetailsActivity.this, "Failed to update order details", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("OrderDetailsActivity", "Failed to check last status", error.toException());
                Toast.makeText(OrderDetailsActivity.this, "Error checking last status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateButtonVisibility() {
        // Hide all buttons if the current status is "Delivered"
        if ("Delivered".equals(currentStatus)) {
            binding.buttonPickupComplete.setVisibility(View.GONE);
            binding.buttonSentForDelivery.setVisibility(View.GONE);
            binding.buttonDelivered.setVisibility(View.GONE);
            return;
        }

        // Show or hide "Pickup Complete" button based on the current status
        binding.buttonPickupComplete.setVisibility(
                "Pickup Complete".equals(currentStatus) || "Sent For Delivery".equals(currentStatus) ? View.GONE : View.VISIBLE
        );

        // Show or hide "Sent For Delivery" button based on the current status
        binding.buttonSentForDelivery.setVisibility(
                "Sent For Delivery".equals(currentStatus) || "Delivered".equals(currentStatus) ? View.GONE : View.VISIBLE
        );

        // Show or hide "Delivered" button based on the current status
        binding.buttonDelivered.setVisibility(
                "Delivered".equals(currentStatus) ? View.GONE : View.VISIBLE
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (pickupLocation != null && deliveryLocation != null) {
            showMarkersOnMap(pickupLocation, deliveryLocation);
        }

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Set a map click listener
        mMap.setOnMapClickListener(latLng -> {
            if (pickupLocation == null) {
                showConfirmationDialog("Set Pickup Location", "Do you want to set this location as the pickup location?", latLng, true);
            } else if (deliveryLocation == null) {
                showConfirmationDialog("Set Delivery Location", "Do you want to set this location as the delivery location?", latLng, false);
            } else {
                // Call the reset locations dialog directly
                showResetLocationsConfirmationDialog();
            }
        });

    }

    // Method to show confirmation dialog for setting locations
    private void showConfirmationDialog(String title, String message, LatLng latLng, boolean isPickup) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (isPickup) {
                        pickupLocation = latLng;
                        Toast.makeText(this, "Pickup Location Set", Toast.LENGTH_SHORT).show();
                    } else {
                        deliveryLocation = latLng;
                        Toast.makeText(this, "Delivery Location Set", Toast.LENGTH_SHORT).show();
                    }
                    showMarkersOnMap(pickupLocation, deliveryLocation);
                    if (pickupLocation != null && deliveryLocation != null) {
                        drawLineAndCalculatePrice();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showResetLocationsConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Locations")
                .setMessage("Do you want to reset both pickup and delivery locations?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    pickupLocation = null;
                    deliveryLocation = null;
                    showMarkersOnMap(pickupLocation, deliveryLocation);
                    Toast.makeText(this, "Locations reset", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }


    private void drawLineAndCalculatePrice() {
        if (pickupLocation != null && deliveryLocation != null) {
            mMap.addPolyline(new PolylineOptions().add(pickupLocation, deliveryLocation).width(5)
                    .color(ContextCompat.getColor(this, R.color.pure_courier_text)));

            double distance = calculateDistance(pickupLocation, deliveryLocation);
            double price = calculateDeliveryPrice(distance);

            // Update UI with calculated distance and price
            binding.distanceText.setText(String.format("Distance: %.2f km", distance));
            binding.labelPrice.setText(String.format("Price: Rs %.2f", price));
        }
    }

    private double calculateDistance(LatLng pickup, LatLng delivery) {
        double earthRadius = 6371.0; // Radius of the Earth in km
        double latDiff = Math.toRadians(delivery.latitude - pickup.latitude);
        double lonDiff = Math.toRadians(delivery.longitude - pickup.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(pickup.latitude)) * Math.cos(Math.toRadians(delivery.latitude)) *
                        Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c; // Distance in km
    }

    private double calculateDeliveryPrice(double distance) {
        double baseFare = 50; // Base fare
        double costPerKm = 20; // Cost per km
        double minimumFare = 100; // Minimum fare
        double totalPrice = baseFare + (costPerKm * distance);
        return Math.max(totalPrice, minimumFare); // Ensure minimum fare is applied
    }
}
