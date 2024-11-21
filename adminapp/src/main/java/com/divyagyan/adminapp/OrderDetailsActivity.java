package com.divyagyan.adminapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.divyagyan.adminapp.databinding.ActivityOrderDetailsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.gms.common.api.Status;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    // Constants
    private static final LatLng KATHMANDU_BOUNDS_SW = new LatLng(27.605670, 85.206885);
    private static final LatLng KATHMANDU_BOUNDS_NE = new LatLng(27.815670, 85.375959);
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Binding and Map related fields
    private ActivityOrderDetailsBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    // Location and Firebase related fields
    private LatLng pickupLocation;
    private LatLng deliveryLocation;
    private String orderId;
    private DatabaseReference orderRef;
    private String currentStatus = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        allocateActivityTitle("Order Details");

        // Initialize Places API and Location Services
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY");
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get Order ID and Firebase Reference
        orderId = getIntent().getStringExtra("orderId");
        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_order_details);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup UI Components
        setupAutocompleteFragments();
        setupButtons();
        loadOrderDetails();
    }

    private void setupAutocompleteFragments() {
        setupAutocompletePickup();
        setupAutocompleteDelivery();
    }

    private void setupButtons() {
        binding.buttonPickupComplete.setOnClickListener(v ->
                showStatusConfirmationDialog("Pickup Complete"));
        binding.buttonSentForDelivery.setOnClickListener(v ->
                showStatusConfirmationDialog("Sent For Delivery"));
        binding.buttonDelivered.setOnClickListener(v ->
                showStatusConfirmationDialog("Delivered"));
        binding.updateButton.setOnClickListener(v ->
                saveFinalOrderDetails());
    }

    private void setupAutocompletePickup() {
        AutocompleteSupportFragment autocompletePickup = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_pickup_address);

        if (autocompletePickup != null) {
            autocompletePickup.setPlaceFields(Arrays.asList(
                    Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompletePickup.setLocationRestriction(
                    RectangularBounds.newInstance(KATHMANDU_BOUNDS_SW, KATHMANDU_BOUNDS_NE));

            autocompletePickup.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    pickupLocation = place.getLatLng();
                    showMarkersOnMap(pickupLocation, deliveryLocation);
                    drawLineAndCalculatePrice();
                    updateAutocompleteFields();
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e("OrderDetailsActivity",
                            "Pickup location selection error: " + status.getStatusMessage());
                }
            });
        }
    }

    private void setupAutocompleteDelivery() {
        AutocompleteSupportFragment autocompleteDelivery = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_delivery_address);

        if (autocompleteDelivery != null) {
            autocompleteDelivery.setPlaceFields(Arrays.asList(
                    Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteDelivery.setLocationRestriction(
                    RectangularBounds.newInstance(KATHMANDU_BOUNDS_SW, KATHMANDU_BOUNDS_NE));

            autocompleteDelivery.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    deliveryLocation = place.getLatLng();
                    showMarkersOnMap(pickupLocation, deliveryLocation);
                    drawLineAndCalculatePrice();
                    updateAutocompleteFields();
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e("OrderDetailsActivity",
                            "Delivery location selection error: " + status.getStatusMessage());
                }
            });
        }
    }

    private void updateAutocompleteFields() {
        if (pickupLocation != null) {
            String pickupAddress = getHumanReadableAddress(
                    pickupLocation.latitude, pickupLocation.longitude);
            AutocompleteSupportFragment autocompletePickup = (AutocompleteSupportFragment)
                    getSupportFragmentManager().findFragmentById(R.id.autocomplete_pickup_address);
            if (autocompletePickup != null) {
                autocompletePickup.setText(pickupAddress);
            }
        }

        if (deliveryLocation != null) {
            String deliveryAddress = getHumanReadableAddress(
                    deliveryLocation.latitude, deliveryLocation.longitude);
            AutocompleteSupportFragment autocompleteDelivery = (AutocompleteSupportFragment)
                    getSupportFragmentManager().findFragmentById(R.id.autocomplete_delivery_address);
            if (autocompleteDelivery != null) {
                autocompleteDelivery.setText(deliveryAddress);
            }
        }
    }

    private String getHumanReadableAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            Log.e("OrderDetailsActivity", "Geocoding failed", e);
        }
        return "Unable to determine address";
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        if (pickupLocation != null && deliveryLocation != null) {
            showMarkersOnMap(pickupLocation, deliveryLocation);
        }

        getLocationPermission();

        mMap.setOnMapClickListener(this::showLocationSelectionDialog);
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            showCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(
                            location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                }
            });
        }
    }

    private void showLocationSelectionDialog(LatLng latLng) {
        new AlertDialog.Builder(this)
                .setTitle("Select Location Type")
                .setItems(new String[]{"Pickup Location", "Delivery Location"},
                        (dialog, which) -> {
                            if (which == 0) {
                                pickupLocation = latLng;
                                Toast.makeText(this, "Pickup Location Set",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                deliveryLocation = latLng;
                                Toast.makeText(this, "Delivery Location Set",
                                        Toast.LENGTH_SHORT).show();
                            }
                            showMarkersOnMap(pickupLocation, deliveryLocation);
                            drawLineAndCalculatePrice();
                            updateAutocompleteFields();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMarkersOnMap(LatLng pickup, LatLng delivery) {
        if (mMap != null) {
            mMap.clear();

            if (pickup != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(pickup)
                        .title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_BLUE)));
            }

            if (delivery != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(delivery)
                        .title("Delivery Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN)));
            }

            if (pickup != null && delivery != null) {
                mMap.addPolyline(new PolylineOptions()
                        .add(pickup, delivery)
                        .width(5)
                        .color(ContextCompat.getColor(this, R.color.pure_courier_text)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, 12));
            }
        }
    }

    private void drawLineAndCalculatePrice() {
        if (pickupLocation != null && deliveryLocation != null) {
            double distance = calculateDistance(pickupLocation, deliveryLocation);
            double price = calculateDeliveryPrice(distance);

            binding.distanceText.setText(String.format(
                    Locale.getDefault(), "Distance: %.2f km", distance));
            binding.labelPrice.setText(String.format(
                    Locale.getDefault(), "Price: Rs %.2f", price));
        }
    }

    private double calculateDistance(LatLng start, LatLng end) {
        double earthRadius = 6371.0;
        double latDiff = Math.toRadians(end.latitude - start.latitude);
        double lonDiff = Math.toRadians(end.longitude - start.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(start.latitude)) *
                        Math.cos(Math.toRadians(end.latitude)) *
                        Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private double calculateDeliveryPrice(double distance) {
        double baseFare = 50;
        double costPerKm = 20;
        double minimumFare = 100;
        return Math.max(baseFare + (costPerKm * distance), minimumFare);
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String packageDetails = dataSnapshot.child("packageDetails").getValue(String.class);
                    String recipientName = dataSnapshot.child("recipientName").getValue(String.class);
                    String recipientPhone = dataSnapshot.child("recipientPhone").getValue(String.class);
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

                    updateAutocompleteFields(); // Update the autocomplete fields with the addresses

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

        double distance = 0.0;
        double price = 0.0;
        if (pickupLocation != null && deliveryLocation != null) {
            distance = calculateDistance(pickupLocation, deliveryLocation);
            price = calculateDeliveryPrice(distance);
        }

        String formattedDistance = String.format(Locale.getDefault(), "%.2f", distance);
        String formattedPrice = String.format(Locale.getDefault(), "%.2f", price);

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("packageDetails", updatedPackageDetails);
        updatedData.put("recipientName", updatedRecipientName);
        updatedData.put("recipientPhone", updatedRecipientPhone);
        updatedData.put("status", currentStatus);
        updatedData.put("distance", formattedDistance);
        updatedData.put("price", formattedPrice);

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

        DatabaseReference statusHistoryRef = orderRef.child("statusHistory");
        statusHistoryRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastStatus = "";
                for (DataSnapshot child : snapshot.getChildren()) {
                    lastStatus = child.child("status").getValue(String.class);
                }

                if (!currentStatus.equals(lastStatus)) {
                    Map<String, Object> statusUpdate = new HashMap<>();
                    statusUpdate.put("status", currentStatus);
                    statusUpdate.put("timestamp", timestamp);
                    statusHistoryRef.push().setValue(statusUpdate);
                }

                orderRef.updateChildren(updatedData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(OrderDetailsActivity.this, "Order details updated successfully", Toast.LENGTH_SHORT).show();
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
        if ("Delivered".equals(currentStatus)) {
            binding.buttonPickupComplete.setVisibility(View.GONE);
            binding.buttonSentForDelivery.setVisibility(View.GONE);
            binding.buttonDelivered.setVisibility(View.GONE);
            return;
        }

        binding.buttonPickupComplete.setVisibility(
                "Pickup Complete".equals(currentStatus) || "Sent For Delivery".equals(currentStatus) ? View.GONE : View.VISIBLE);

        binding.buttonSentForDelivery.setVisibility(
                "Sent For Delivery".equals(currentStatus) || "Delivered".equals(currentStatus) ? View.GONE : View.VISIBLE);

        binding.buttonDelivered.setVisibility(
                "Delivered".equals(currentStatus) ? View.GONE : View.VISIBLE);
    }
}
