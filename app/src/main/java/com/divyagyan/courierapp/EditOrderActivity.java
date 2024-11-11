package com.divyagyan.courierapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.divyagyan.courierapp.databinding.ActivityEditOrderBinding;
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

import java.util.HashMap;
import java.util.Map;

public class EditOrderActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    private ActivityEditOrderBinding binding;
    private EditText packageDetails, recipientName, recipientPhone;
    private TextView priceLabel, distanceText;
    private Button updateButton;
    private LatLng pickupLocation, deliveryLocation;
    private String orderId;
    private DatabaseReference orderRef;
    private GoogleMap mMap;

    private static final double PRICE_PER_KM = 20.0;
    private static final double BASE_FARE = 50.0;
    private static final double MINIMUM_FARE = 100.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        allocateActivityTitle("Edit Order");

        orderId = getIntent().getStringExtra("orderId");
        packageDetails = binding.edittextPackageDetails;
        recipientName = binding.edittextRecipientName;
        recipientPhone = binding.edittextRecipientPhone;
        priceLabel = binding.labelPrice;
        distanceText = binding.distanceText;
        updateButton = binding.buttonUpdateOrder;

        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadOrderDetails();
        updateButton.setOnClickListener(v -> updateOrder());
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                    packageDetails.setText((String) orderData.get("packageDetails"));
                    recipientName.setText((String) orderData.get("recipientName"));
                    recipientPhone.setText((String) orderData.get("recipientPhone"));

                    pickupLocation = getLatLngFromMap((Map<String, Object>) orderData.get("pickupLocation"));
                    deliveryLocation = getLatLngFromMap((Map<String, Object>) orderData.get("deliveryLocation"));

                    // Use saved distance and price from the database
                    String savedDistance = (String) orderData.get("distance");
                    String savedPrice = (String) orderData.get("price");

                    if (savedDistance != null && savedPrice != null) {
                        distanceText.setText(String.format("Distance: %s km", savedDistance));
                        priceLabel.setText(String.format("Price: Rs %s", savedPrice));
                    } else {
                        // Fallback to calculating if values are not saved
                        calculateAndDisplayPrice();
                    }

                    drawMarkers();
                } else {
                    Toast.makeText(EditOrderActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EditOrderActivity", "Failed to load order data", error.toException());
            }
        });
    }


    private LatLng getLatLngFromMap(Map<String, Object> locationData) {
        return new LatLng((double) locationData.get("latitude"), (double) locationData.get("longitude"));
    }

    private void calculateAndDisplayPrice() {
        if (pickupLocation != null && deliveryLocation != null) {
            double distance = calculateDistance(pickupLocation, deliveryLocation);
            double price = calculateDeliveryPrice(distance);
            distanceText.setText(String.format("Distance: %.2f km", distance));
            priceLabel.setText(String.format("Price: Rs %.2f", price));
        } else {
            distanceText.setText("Distance: 0 km");
            priceLabel.setText("Price: Rs 0.00");
        }
    }

    private double calculateDistance(LatLng start, LatLng end) {
        Location startLocation = new Location("Start");
        startLocation.setLatitude(start.latitude);
        startLocation.setLongitude(start.longitude);

        Location endLocation = new Location("End");
        endLocation.setLatitude(end.latitude);
        endLocation.setLongitude(end.longitude);

        return startLocation.distanceTo(endLocation) / 1000; // Return distance in kilometers
    }

    private double calculateDeliveryPrice(double distance) {
        double totalPrice = BASE_FARE + (PRICE_PER_KM * distance);
        return Math.max(totalPrice, MINIMUM_FARE);
    }

    private void updateOrder() {
        String details = packageDetails.getText().toString().trim();
        String name = recipientName.getText().toString().trim();
        String phone = recipientPhone.getText().toString().trim();

        if (validateInputs(details, name, phone)) {
            double distance = calculateDistance(pickupLocation, deliveryLocation);
            double price = calculateDeliveryPrice(distance);
            String formattedPrice = String.format("%.2f", price);

            Map<String, Object> data = new HashMap<>();
            data.put("packageDetails", details);
            data.put("recipientName", name);
            data.put("recipientPhone", phone);
            data.put("pickupLocation", getLocationMap(pickupLocation));
            data.put("deliveryLocation", getLocationMap(deliveryLocation));
            data.put("price", formattedPrice);
            data.put("distance", String.format("%.2f", distance));

            orderRef.updateChildren(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Order Updated Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to Update Order", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private Map<String, Object> getLocationMap(LatLng latLng) {
        Map<String, Object> map = new HashMap<>();
        map.put("latitude", latLng.latitude);
        map.put("longitude", latLng.longitude);
        return map;
    }

    private boolean validateInputs(String details, String name, String phone) {
        if (details.isEmpty() || name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!phone.matches("\\d{10}")) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        drawMarkers();

        mMap.setOnMapClickListener(latLng -> showConfirmationDialog(latLng));
    }

    private void showConfirmationDialog(LatLng latLng) {
        new AlertDialog.Builder(this)
                .setTitle("Update Location")
                .setMessage("Do you want to update the location?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (pickupLocation == null) {
                        pickupLocation = latLng;
                        Toast.makeText(this, "Pickup Location Set", Toast.LENGTH_SHORT).show();
                    } else if (deliveryLocation == null) {
                        deliveryLocation = latLng;
                        Toast.makeText(this, "Delivery Location Set", Toast.LENGTH_SHORT).show();
                    } else {
                        pickupLocation = latLng;
                        deliveryLocation = null;
                        Toast.makeText(this, "Pickup Location Reset. Tap again to set Delivery Location.", Toast.LENGTH_SHORT).show();
                    }

                    drawMarkers();
                    calculateAndDisplayPrice();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void drawMarkers() {
        if (mMap != null) {
            mMap.clear();
            if (pickupLocation != null) {
                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
            if (deliveryLocation != null) {
                mMap.addMarker(new MarkerOptions().position(deliveryLocation).title("Delivery Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }
            if (pickupLocation != null && deliveryLocation != null) {
                mMap.addPolyline(new PolylineOptions().add(pickupLocation, deliveryLocation).width(5).color(ContextCompat.getColor(this, R.color.pure_courier_text)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, 12));
            }
        }
    }
}
