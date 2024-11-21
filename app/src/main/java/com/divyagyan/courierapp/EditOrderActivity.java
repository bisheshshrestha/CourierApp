package com.divyagyan.courierapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.divyagyan.courierapp.databinding.ActivityEditOrderBinding;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditOrderActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    // Constants
    private static final LatLng KATHMANDU_BOUNDS_SW = new LatLng(27.605670, 85.206885);
    private static final LatLng KATHMANDU_BOUNDS_NE = new LatLng(27.815670, 85.375959);

    // Fields
    private GoogleMap mMap;
    private LatLng pickupLocation;
    private LatLng deliveryLocation;
    private String userUid;
    private ActivityEditOrderBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference orderRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        allocateActivityTitle("Edit Order");

        // User Preferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userUid = sharedPreferences.getString("user_uid", null);

        if (userUid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            return;
        }

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY"); // Replace with your API key
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(getIntent().getStringExtra("orderId"));

        // Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Setup Autocomplete
        setupAutocompletePickup();
        setupAutocompleteDelivery();

        // Load Order Details
        loadOrderDetails();

        // Update Order
        binding.buttonUpdateOrder.setOnClickListener(v -> updateOrder());
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> orderData = (Map<String, Object>) snapshot.getValue();
                    binding.edittextPackageDetails.setText((String) orderData.get("packageDetails"));
                    binding.edittextRecipientName.setText((String) orderData.get("recipientName"));
                    binding.edittextRecipientPhone.setText((String) orderData.get("recipientPhone"));

                    pickupLocation = getLatLngFromMap((Map<String, Object>) orderData.get("pickupLocation"));
                    deliveryLocation = getLatLngFromMap((Map<String, Object>) orderData.get("deliveryLocation"));

                    // Set the autocomplete text fields
                    updateAutocompleteFields();

                    drawMarkers();
                    calculateAndDisplayPrice(); // Calculate initial price and distance
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

    private void updateAutocompleteFields() {
        if (pickupLocation != null) {
            String pickupAddress = getHumanReadableAddress(pickupLocation.latitude, pickupLocation.longitude);
            AutocompleteSupportFragment autocompletePickup = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_pickup_address);
            if (autocompletePickup != null) {
                autocompletePickup.setText(pickupAddress);
            }
        }

        if (deliveryLocation != null) {
            String deliveryAddress = getHumanReadableAddress(deliveryLocation.latitude, deliveryLocation.longitude);
            AutocompleteSupportFragment autocompleteDelivery = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_delivery_address);
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
            } else {
                return "Unknown Location";
            }
        } catch (Exception e) {
            Log.e("EditOrderActivity", "Geocoder failed", e);
            return "Unable to determine address";
        }
    }

    private void setupAutocompletePickup() {
        AutocompleteSupportFragment autocompletePickup = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_pickup_address);
        autocompletePickup.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompletePickup.setLocationRestriction(RectangularBounds.newInstance(KATHMANDU_BOUNDS_SW, KATHMANDU_BOUNDS_NE));

        autocompletePickup.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                pickupLocation = place.getLatLng();
                drawMarkers();
                calculateAndDisplayPrice();
                updateAutocompleteFields(); // Update autocomplete text when selected
            }

            @Override
            public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                Log.e("EditOrderActivity", "Error selecting pickup location: " + status.getStatusMessage());
            }
        });
    }

    private void setupAutocompleteDelivery() {
        AutocompleteSupportFragment autocompleteDelivery = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_delivery_address);
        autocompleteDelivery.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteDelivery.setLocationRestriction(RectangularBounds.newInstance(KATHMANDU_BOUNDS_SW, KATHMANDU_BOUNDS_NE));

        autocompleteDelivery.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                deliveryLocation = place.getLatLng();
                drawMarkers();
                calculateAndDisplayPrice();
                updateAutocompleteFields(); // Update autocomplete text when selected
            }

            @Override
            public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                Log.e("EditOrderActivity", "Error selecting delivery location: " + status.getStatusMessage());
            }
        });
    }

    private void drawMarkers() {
        if (mMap != null) {
            mMap.clear();
            if (pickupLocation != null) {
                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
            if (deliveryLocation != null) {
                mMap.addMarker(new MarkerOptions().position(deliveryLocation).title("Delivery Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }
            // Draw polyline if both locations are set
            if (pickupLocation != null && deliveryLocation != null) {
                drawPolyline();
            }
        }
    }

    private void drawPolyline() {
        if (mMap != null && pickupLocation != null && deliveryLocation != null) {
            mMap.addPolyline(new PolylineOptions()
                    .add(pickupLocation, deliveryLocation)
                    .width(5)
                    .color(ContextCompat.getColor(this, R.color.pure_courier_text))); // Use your desired color
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, 12));
        }
    }

    private void calculateAndDisplayPrice() {
        if (pickupLocation != null && deliveryLocation != null) {
            double distance = calculateDistance(pickupLocation, deliveryLocation);
            double price = calculateDeliveryPrice(distance);

            Log.d("EditOrderActivity", "Distance: " + distance);
            Log.d("EditOrderActivity", "Price: " + price);

            binding.labelPrice.setText(String.format("Price: Rs %.2f", price));
            binding.distanceText.setText(String.format("Distance: %.2f km", distance));
        } else {
            binding.labelPrice.setText("Price: Rs 0.00");
            binding.distanceText.setText("Distance: 0 km");
        }
    }

    private double calculateDistance(LatLng start, LatLng end) {
        // Haversine formula to calculate distance
        double earthRadius = 6371.0;
        double latDiff = Math.toRadians(end.latitude - start.latitude);
        double lonDiff = Math.toRadians(end.longitude - start.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude)) *
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

    private void updateOrder() {
        String details = binding.edittextPackageDetails.getText().toString().trim();
        String name = binding.edittextRecipientName.getText().toString().trim();
        String phone = binding.edittextRecipientPhone.getText().toString().trim();

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
        getLocationPermission();

        // Set up a click listener for the map
        mMap.setOnMapClickListener(latLng -> showConfirmationDialog(latLng));
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

    private void showConfirmationDialog(LatLng latLng) {
        new AlertDialog.Builder(this)
                .setTitle("Update Location")
                .setMessage("Do you want to update the location?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Ask the user to specify if it's a Pickup or Delivery location
                    new AlertDialog.Builder(this)
                            .setTitle("Select Location Type")
                            .setMessage("Is this location for Pickup or Delivery?")
                            .setPositiveButton("Pickup", (pickupDialog, whichPickup) -> {
                                pickupLocation = latLng;
                                Toast.makeText(this, "Pickup Location Set", Toast.LENGTH_SHORT).show();
                                drawMarkers();
                                calculateAndDisplayPrice(); // Calculate price after setting location
                                updateAutocompleteFields(); // Update fields after setting locations
                            })
                            .setNegativeButton("Delivery", (deliveryDialog, whichDelivery) -> {
                                deliveryLocation = latLng;
                                Toast.makeText(this, "Delivery Location Set", Toast.LENGTH_SHORT).show();
                                drawMarkers();
                                calculateAndDisplayPrice(); // Calculate price after setting location
                                updateAutocompleteFields(); // Update fields after setting locations
                            })
                            .setNeutralButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("No", null)
                .show();
    }

}
