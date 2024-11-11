package com.divyagyan.courierapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.divyagyan.courierapp.databinding.ActivityOrderBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OrderActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    // Constants
    private static final LatLngBounds KATHMANDU_BOUNDS = new LatLngBounds(
            new LatLng(27.605670, 85.206885),  // Southwest corner
            new LatLng(27.815670, 85.375959)   // Northeast corner
    );

    // Fields
    private GoogleMap mMap;
    private LatLng pickupLocation;
    private LatLng deliveryLocation;
    private String userUid;
    private ActivityOrderBinding activityOrderBinding;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityOrderBinding = ActivityOrderBinding.inflate(getLayoutInflater());
        setContentView(activityOrderBinding.getRoot());
        allocateActivityTitle("Orders");

        // User Preferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userUid = sharedPreferences.getString("user_uid", null);

        if (userUid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            return;
        }

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCL2N9v3XaFNGY7UPbuBfS0Ekntuv97D9Q"); // Replace with your API key
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Setup Autocomplete
        setupAutocompletePickup();
        setupAutocompleteDelivery();

        // Order Creation
        activityOrderBinding.buttonCreateOrder.setOnClickListener(v -> {
            String packageDetails = activityOrderBinding.edittextPackageDetails.getText().toString().trim();
            String recipientName = activityOrderBinding.edittextRecipientName.getText().toString().trim();
            String recipientPhone = activityOrderBinding.edittextRecipientPhone.getText().toString().trim();

            // Validate inputs
            String validationMessage = validateInputs(packageDetails, recipientName, recipientPhone);
            if (validationMessage != null) {
                Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show();
                return; // Stop further execution if validation fails
            }

            if (pickupLocation != null && deliveryLocation != null) {
                double distance = calculateDistance(pickupLocation, deliveryLocation);
                double price = calculateDeliveryPrice(distance);

                // Update the UI with the calculated distance and price
                activityOrderBinding.distanceText.setText(String.format("Distance: %.2f km", distance));
                activityOrderBinding.labelPrice.setText(String.format("Price: Rs %.2f", price));

                // Generate a unique order ID
                String orderId = UUID.randomUUID().toString(); // Use UUID for order ID

                String trackingNumber = generateTrackingNumber(); // Generate tracking number
                String orderCreationTime = getCurrentDateTime();

                // Save the order to Firebase
                saveOrderToFirebase(orderId, pickupLocation, deliveryLocation, price, distance,packageDetails, recipientName, recipientPhone, orderCreationTime, trackingNumber);
            } else {
                Toast.makeText(OrderActivity.this, "Please select both Pickup and Delivery locations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Input Validation Method
    private String validateInputs(String packageDetails, String recipientName, String recipientPhone) {
        if (packageDetails.isEmpty()) {
            return "Package details cannot be empty.";
        }
        if (recipientName.isEmpty()) {
            return "Recipient name cannot be empty.";
        }
        if (recipientPhone.isEmpty()) {
            return "Recipient phone number cannot be empty.";
        }
        if (!isValidPhoneNumber(recipientPhone)) {
            return "Invalid phone number. Please enter a valid 10-digit number.";
        }
        return null; // All inputs are valid
    }

    // Phone Number Validation Method
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("\\d{10}"); // Validates that the phone number is exactly 10 digits
    }

    // Autocomplete Methods
    private void setupAutocompletePickup() {
        AutocompleteSupportFragment autocompletePickup = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_pickup_address);
        autocompletePickup.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompletePickup.setLocationRestriction(RectangularBounds.newInstance(KATHMANDU_BOUNDS));

        autocompletePickup.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                pickupLocation = place.getLatLng();
                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, 12));
                drawLineAndCalculatePrice();
            }

            @Override
            public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                Log.e("OrderActivity", "Error selecting pickup location: " + status.getStatusMessage());
            }
        });
    }

    private void setupAutocompleteDelivery() {
        AutocompleteSupportFragment autocompleteDelivery = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_delivery_address);
        autocompleteDelivery.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteDelivery.setLocationRestriction(RectangularBounds.newInstance(KATHMANDU_BOUNDS));

        autocompleteDelivery.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                deliveryLocation = place.getLatLng();
                mMap.addMarker(new MarkerOptions().position(deliveryLocation).title("Delivery Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(deliveryLocation, 12));
                drawLineAndCalculatePrice();
            }

            @Override
            public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                Log.e("OrderActivity", "Error selecting delivery location: " + status.getStatusMessage());
            }
        });
    }

    // Firebase Methods
    private void saveOrderToFirebase(String orderId, LatLng pickupLocation, LatLng deliveryLocation, double price,double distance, String packageDetails, String recipientName, String recipientPhone, String orderCreationTime, String trackingNumber) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

        // Create order data map
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("trackingNumber", trackingNumber);
        orderData.put("userUid", userUid);
        orderData.put("recipientName", recipientName);
        orderData.put("recipientPhone", recipientPhone);
        orderData.put("packageDetails", packageDetails);
        orderData.put("pickupLocation", pickupLocation);
        orderData.put("deliveryLocation", deliveryLocation);
        orderData.put("price", String.format("%.2f", price));
        orderData.put("distance",String.format("%.2f", distance));
        orderData.put("status", "Order Created");

        // Save the order data without overwriting existing nodes
        ordersRef.updateChildren(orderData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Add initial status to statusHistory
                Map<String, Object> statusHistoryEntry = new HashMap<>();
                statusHistoryEntry.put("status", "Order Created");
                statusHistoryEntry.put("timestamp", orderCreationTime);

                DatabaseReference statusHistoryRef = ordersRef.child("statusHistory").push();
                statusHistoryRef.setValue(statusHistoryEntry).addOnCompleteListener(statusTask -> {
                    if (statusTask.isSuccessful()) {
                        Toast.makeText(OrderActivity.this, "Order Created Successfully", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(OrderActivity.this, ViewOrderActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(OrderActivity.this, "Failed to save status history", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(OrderActivity.this, "Failed to create order", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Google Map Setup
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(27.717245, 85.323959), 12));

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        getLocationPermission();

        // Set a map click listener
        mMap.setOnMapClickListener(latLng -> {
            if (pickupLocation == null) {
                // Set pickup location
                pickupLocation = latLng;
                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            } else if (deliveryLocation == null) {
                // Set delivery location
                deliveryLocation = latLng;
                mMap.addMarker(new MarkerOptions().position(deliveryLocation).title("Delivery Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            } else {
                // Reset both locations if both are already set
                mMap.clear();
                pickupLocation = latLng;
                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                deliveryLocation = null;
            }

            // Redraw the line and recalculate price if both locations are set
            if (pickupLocation != null && deliveryLocation != null) {
                drawLineAndCalculatePrice();
            }
        });
    }


    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            showCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                }
            });
        }
    }

    // Line and Price Calculation
    private void drawLineAndCalculatePrice() {
        if (pickupLocation != null && deliveryLocation != null) {
            mMap.addPolyline(new PolylineOptions().add(pickupLocation, deliveryLocation).width(5)
                    .color(ContextCompat.getColor(this, R.color.pure_courier_text)));

            double distance = calculateDistance(pickupLocation, deliveryLocation);
            double price = calculateDeliveryPrice(distance);

            activityOrderBinding.distanceText.setText(String.format("Distance: %.2f km", distance));
            activityOrderBinding.labelPrice.setText(String.format("Price: Rs %.2f", price));
        }
    }

    private double calculateDistance(LatLng pickup, LatLng delivery) {
        double earthRadius = 6371.0;
        double latDiff = Math.toRadians(delivery.latitude - pickup.latitude);
        double lonDiff = Math.toRadians(delivery.longitude - pickup.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(pickup.latitude)) * Math.cos(Math.toRadians(delivery.latitude)) *
                        Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private double calculateDeliveryPrice(double distance) {
        double baseFare = 50;
        double costPerKm = 20;
        double minimumFare = 100;
        double totalPrice = baseFare + (costPerKm * distance);
        return Math.max(totalPrice, minimumFare);
    }

    // Generate a unique tracking number
    private String generateTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // Generates a unique tracking number
    }

    // Get Current Date and Time
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Properly shutdown the Places API to avoid memory leaks
        try {
            if (Places.isInitialized()) {
                com.google.android.libraries.places.api.Places.deinitialize();
                Log.d("OrderActivity", "Places API deinitialized successfully.");
            }
        } catch (Exception e) {
            Log.e("OrderActivity", "Failed to deinitialize Places API", e);
        }
    }


}
