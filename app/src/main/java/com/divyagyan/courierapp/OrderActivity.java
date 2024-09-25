package com.divyagyan.courierapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.divyagyan.courierapp.databinding.ActivityOrderBinding;
import com.google.android.gms.common.api.*;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.libraries.places.api.*;
import com.google.android.libraries.places.api.model.*;
import com.google.android.libraries.places.widget.*;
import com.google.android.libraries.places.widget.listener.*;
import com.google.firebase.database.*;

import java.text.*;
import java.util.*;


public class OrderActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private LatLng pickupLocation;
    private LatLng deliveryLocation;
    private String userUid;
    private ActivityOrderBinding activityOrderBinding;
    private FusedLocationProviderClient fusedLocationClient;

    // Kathmandu Valley bounds (restricted to Kathmandu City)
    private static final LatLng KATHMANDU_CENTER = new LatLng(27.717245, 85.323959);
    private static final float KATHMANDU_ZOOM = 12f; // Suitable zoom level for Kathmandu Valley
    private static final LatLngBounds KATHMANDU_BOUNDS = new LatLngBounds(
            new LatLng(27.605670, 85.206885),  // Southwest corner of Kathmandu
            new LatLng(27.815670, 85.375959)); // Northeast corner of Kathmandu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityOrderBinding = ActivityOrderBinding.inflate(getLayoutInflater());
        setContentView(activityOrderBinding.getRoot());
        allocateActivityTitle("Orders");

        // Retrieve userUid from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userUid = sharedPreferences.getString("user_uid", null);

        if (userUid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            return; // Stop the activity if userUid is not found
        }

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCL2N9v3XaFNGY7UPbuBfS0Ekntuv97D9Q"); // Replace with your Places API Key
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Setup Autocomplete for Pickup Location
        setupAutocompletePickup();

        // Setup Autocomplete for Delivery Location
        setupAutocompleteDelivery();

        // Handle Order Creation
        activityOrderBinding.buttonCreateOrder.setOnClickListener(v -> {
            String packageDetails = activityOrderBinding.edittextPackageDetails.getText().toString();
            String recipientName = activityOrderBinding.edittextRecipientName.getText().toString();
            String recipientPhone = activityOrderBinding.edittextRecipientPhone.getText().toString();

            if (pickupLocation != null && deliveryLocation != null) {
                double price = calculateDeliveryPrice(pickupLocation, deliveryLocation);
                activityOrderBinding.labelPrice.setText(String.format("Price: Rs %.2f", price));  // Show price with 2 decimal places

                // Validate recipient details
                if (!packageDetails.isEmpty() && !recipientName.isEmpty() && !recipientPhone.isEmpty()) {
                    // Generate a unique tracking number
                    String trackingNumber = generateTrackingNumber();

                    // Capture the current date and time
                    String currentDateTime = getCurrentDateTime();

                    // Save order to Firebase
                    saveOrderToFirebase(pickupLocation, deliveryLocation, price, packageDetails, recipientName, recipientPhone, trackingNumber, currentDateTime);
                } else {
                    Toast.makeText(OrderActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(OrderActivity.this, "Please select both Pickup and Delivery locations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to setup Pickup location autocomplete restricted to Kathmandu
    // Method to setup Pickup location autocomplete restricted to Kathmandu Valley
    private void setupAutocompletePickup() {
        AutocompleteSupportFragment autocompletePickup = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_pickup_address);
        autocompletePickup.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // Restrict results to Kathmandu Valley using setLocationRestriction
        autocompletePickup.setLocationRestriction(RectangularBounds.newInstance(
                new LatLng(27.605670, 85.206885),  // Southwest corner of Kathmandu Valley
                new LatLng(27.815670, 85.375959)   // Northeast corner of Kathmandu Valley
        ));

        autocompletePickup.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                pickupLocation = place.getLatLng();
                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))); // Blue marker for pickup
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, 12));

                // If delivery location is set, draw a line and calculate price
                if (deliveryLocation != null) {
                    drawLineAndCalculatePrice();
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e("OrderActivity", "Error selecting pickup location: " + status.getStatusMessage());
            }
        });
    }

    // Method to setup Delivery location autocomplete restricted to Kathmandu Valley
    private void setupAutocompleteDelivery() {
        AutocompleteSupportFragment autocompleteDelivery = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_delivery_address);
        autocompleteDelivery.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // Restrict results to Kathmandu Valley using setLocationRestriction
        autocompleteDelivery.setLocationRestriction(RectangularBounds.newInstance(
                new LatLng(27.605670, 85.206885),  // Southwest corner of Kathmandu Valley
                new LatLng(27.815670, 85.375959)   // Northeast corner of Kathmandu Valley
        ));

        autocompleteDelivery.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                deliveryLocation = place.getLatLng();
                mMap.addMarker(new MarkerOptions().position(deliveryLocation).title("Delivery Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))); // Green marker for delivery
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(deliveryLocation, 12));

                // If pickup location is set, draw a line and calculate price
                if (pickupLocation != null) {
                    drawLineAndCalculatePrice();
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e("OrderActivity", "Error selecting delivery location: " + status.getStatusMessage());
            }
        });
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Move the camera to Kathmandu Valley initially
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(KATHMANDU_CENTER, KATHMANDU_ZOOM));

        // Request location permissions and show current location
        getLocationPermission();
    }

    // Request permission to access location
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            showCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Show current location on map
    private void showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, KATHMANDU_ZOOM));
                }
            });
        }
    }

    // Draw line between pickup and delivery locations and calculate the price
    private void drawLineAndCalculatePrice() {
        // Draw a line (polyline) between pickup and delivery locations
        mMap.addPolyline(new PolylineOptions()
                .add(pickupLocation, deliveryLocation)
                .width(5)
                .color(ContextCompat.getColor(this, R.color.pure_courier_text)));  // Adjust color and width as needed

        // Calculate and display the price
        double price = calculateDeliveryPrice(pickupLocation, deliveryLocation);
        activityOrderBinding.labelPrice.setText(String.format("Price: Rs %.2f", price));  // Show price with 2 decimal places
    }

    // Method to calculate delivery price based on distance
    private double calculateDeliveryPrice(LatLng pickup, LatLng delivery) {
        // Use Haversine formula to calculate the distance
        double earthRadius = 6371; // Radius of Earth in kilometers
        double latDiff = Math.toRadians(delivery.latitude - pickup.latitude);
        double lonDiff = Math.toRadians(delivery.longitude - pickup.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(pickup.latitude)) * Math.cos(Math.toRadians(delivery.latitude))
                * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;  // Distance in kilometers

        // Format the distance to 2 decimal places
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String formattedDistance = decimalFormat.format(distance);

        // Show the formatted distance
        Log.d("OrderActivity", "Distance: " + formattedDistance + " km");

        // Assume Rs 20 per kilometer
        return distance * 20;
    }

    // Method to generate a unique tracking number
    private String generateTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Method to get the current date and time
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    // Save the order to Firebase with order creation time
    private void saveOrderToFirebase(LatLng pickupLocation, LatLng deliveryLocation, double price, String packageDetails, String recipientName, String recipientPhone, String trackingNumber, String orderCreationTime) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // Prepare the order data to be inserted
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("pickupLocation", pickupLocation);
        orderData.put("deliveryLocation", deliveryLocation);
        orderData.put("price", price);
        orderData.put("userUid", userUid);
        orderData.put("packageDetails", packageDetails);
        orderData.put("recipientName", recipientName);
        orderData.put("recipientPhone", recipientPhone);
        orderData.put("trackingNumber", trackingNumber);
        orderData.put("orderCreationTime", orderCreationTime);

        // Insert the order into Firebase
        ordersRef.push().setValue(orderData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(OrderActivity.this, "Order Created Successfully. Tracking Number: " + trackingNumber, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(OrderActivity.this, "Failed to create order", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
