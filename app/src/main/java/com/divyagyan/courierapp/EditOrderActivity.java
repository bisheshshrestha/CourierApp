package com.divyagyan.courierapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.divyagyan.courierapp.databinding.ActivityEditOrderBinding;
import com.google.android.gms.common.api.Status;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EditOrderActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    private ActivityEditOrderBinding activityEditOrderBinding;
    private EditText edittextPackageDetails, edittextRecipientName, edittextRecipientPhone;
    private TextView labelPrice;
    private Button updateOrderButton;
    private LatLng pickupLocation, deliveryLocation;
    private String orderId;
    private DatabaseReference orderRef;
    private GoogleMap mMap;

    private static final double PRICE_PER_KM = 80.0; // Price per kilometer
    private static final LatLngBounds KATHMANDU_BOUNDS = new LatLngBounds(
            new LatLng(27.605670, 85.206885),  // Southwest corner
            new LatLng(27.815670, 85.375959)   // Northeast corner
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityEditOrderBinding = ActivityEditOrderBinding.inflate(getLayoutInflater());
        setContentView(activityEditOrderBinding.getRoot());
        allocateActivityTitle("Edit Order");

        orderId = getIntent().getStringExtra("orderId");

        // Initialize views
        edittextPackageDetails = findViewById(R.id.edittext_package_details);
        edittextRecipientName = findViewById(R.id.edittext_recipient_name);
        edittextRecipientPhone = findViewById(R.id.edittext_recipient_phone);
        labelPrice = findViewById(R.id.label_price);
        updateOrderButton = findViewById(R.id.button_update_order);

        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadOrderDetails();
        setupAutocompletePickup();
        setupAutocompleteDelivery();

        updateOrderButton.setOnClickListener(v -> updateOrder());
    }

    private void loadOrderDetails() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> orderData = (Map<String, Object>) dataSnapshot.getValue();
                    if (orderData != null) {
                        edittextPackageDetails.setText((String) orderData.get("packageDetails"));
                        edittextRecipientName.setText((String) orderData.get("recipientName"));
                        edittextRecipientPhone.setText((String) orderData.get("recipientPhone"));

                        // Retrieve and display the actual price from the database if it exists
                        Object priceObj = orderData.get("price");
                        double price = 0.0;
                        if (priceObj instanceof String) {
                            try {
                                price = Double.parseDouble((String) priceObj);
                            } catch (NumberFormatException e) {
                                Log.e("EditOrderActivity", "Price parsing error: " + e.getMessage());
                            }
                        } else if (priceObj instanceof Double) {
                            price = (Double) priceObj;
                        }
                        labelPrice.setText(String.format("Price: Rs %.2f", price)); // Display the actual price retrieved

                        // Retrieve pickup and delivery locations
                        Map<String, Object> pickup = (Map<String, Object>) orderData.get("pickupLocation");
                        Map<String, Object> delivery = (Map<String, Object>) orderData.get("deliveryLocation");

                        if (pickup != null && delivery != null) {
                            double pickupLat = (double) pickup.get("latitude");
                            double pickupLng = (double) pickup.get("longitude");
                            pickupLocation = new LatLng(pickupLat, pickupLng);

                            double deliveryLat = (double) delivery.get("latitude");
                            double deliveryLng = (double) delivery.get("longitude");
                            deliveryLocation = new LatLng(deliveryLat, deliveryLng);

                            // Show markers and paths on the map
                            drawMarkers();
                        }
                    }
                } else {
                    Toast.makeText(EditOrderActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("EditOrderActivity", "Failed to load order data", databaseError.toException());
            }
        });
    }

    private void setupAutocompletePickup() {
        AutocompleteSupportFragment autocompletePickup = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_pickup_address);
        autocompletePickup.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompletePickup.setLocationRestriction(RectangularBounds.newInstance(KATHMANDU_BOUNDS.southwest, KATHMANDU_BOUNDS.northeast));

        autocompletePickup.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                pickupLocation = place.getLatLng();
                drawMarkers();
                calculateAndDisplayPrice();
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e("EditOrderActivity", "Error selecting pickup location: " + status.getStatusMessage());
            }
        });
    }

    private void setupAutocompleteDelivery() {
        AutocompleteSupportFragment autocompleteDelivery = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_delivery_address);
        autocompleteDelivery.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteDelivery.setLocationRestriction(RectangularBounds.newInstance(KATHMANDU_BOUNDS.southwest, KATHMANDU_BOUNDS.northeast));

        autocompleteDelivery.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                deliveryLocation = place.getLatLng();
                drawMarkers();
                calculateAndDisplayPrice();
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e("EditOrderActivity", "Error selecting delivery location: " + status.getStatusMessage());
            }
        });
    }

    private void calculateAndDisplayPrice() {
        if (pickupLocation != null && deliveryLocation != null) {
            double price = calculateDeliveryPrice(pickupLocation, deliveryLocation);
            String formattedPrice = String.format("%.2f", price); // Display only two decimal places without rounding
            Log.d("EditOrderActivity", "Calculated Price: " + formattedPrice);
            labelPrice.setText("Price: Rs " + formattedPrice); // Set the truncated price
        }
    }

    private double calculateDeliveryPrice(LatLng pickup, LatLng delivery) {
        double earthRadius = 6371; // Earth radius in kilometers
        double latDiff = Math.toRadians(delivery.latitude - pickup.latitude);
        double lonDiff = Math.toRadians(delivery.longitude - pickup.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(pickup.latitude)) * Math.cos(Math.toRadians(delivery.latitude))
                * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;
        Log.d("EditOrderActivity", "Calculated Distance: " + distance + " km");  // Log distance to check accuracy

        return distance * PRICE_PER_KM;
    }

    private void updateOrder() {
        String updatedPackageDetails = edittextPackageDetails.getText().toString().trim();
        String updatedRecipientName = edittextRecipientName.getText().toString().trim();
        String updatedRecipientPhone = edittextRecipientPhone.getText().toString().trim();

        if (validateInputs(updatedPackageDetails, updatedRecipientName, updatedRecipientPhone)) {
            double price = calculateDeliveryPrice(pickupLocation, deliveryLocation);
            String truncatedPrice = String.format("%.2f", price); // Convert to two decimal places without rounding

            Map<String, Object> updatedOrderData = new HashMap<>();
            updatedOrderData.put("packageDetails", updatedPackageDetails);
            updatedOrderData.put("recipientName", updatedRecipientName);
            updatedOrderData.put("recipientPhone", updatedRecipientPhone);
            updatedOrderData.put("pickupLocation", LatLngToMap(pickupLocation));
            updatedOrderData.put("deliveryLocation", LatLngToMap(deliveryLocation));
            updatedOrderData.put("price", truncatedPrice); // Store truncated price as a string

            orderRef.updateChildren(updatedOrderData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(EditOrderActivity.this, "Order Updated Successfully", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(EditOrderActivity.this, "Failed to update order", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean validateInputs(String packageDetails, String recipientName, String recipientPhone) {
        if (packageDetails.isEmpty()) {
            Toast.makeText(this, "Package details cannot be empty.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (recipientName.isEmpty()) {
            Toast.makeText(this, "Recipient name cannot be empty.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (recipientPhone.isEmpty() || !recipientPhone.matches("\\d{10}")) {
            Toast.makeText(this, "Invalid phone number.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private Map<String, Object> LatLngToMap(LatLng latLng) {
        Map<String, Object> map = new HashMap<>();
        map.put("latitude", latLng.latitude);
        map.put("longitude", latLng.longitude);
        return map;
    }

    private void drawMarkers() {
        if (mMap != null) {
            mMap.clear();
            if (pickupLocation != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(pickupLocation)
                        .title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))); // Blue marker for pickup
            }
            if (deliveryLocation != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(deliveryLocation)
                        .title("Delivery Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))); // Green marker for delivery
            }

            if (pickupLocation != null && deliveryLocation != null) {
                mMap.addPolyline(new PolylineOptions()
                        .add(pickupLocation, deliveryLocation)
                        .width(5)
                        .color(ContextCompat.getColor(this, R.color.pure_courier_text)));
                LatLng midPoint = new LatLng(
                        (pickupLocation.latitude + deliveryLocation.latitude) / 2,
                        (pickupLocation.longitude + deliveryLocation.longitude) / 2
                );
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(midPoint, 11));
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true); // Enable zoom controls
        mMap.getUiSettings().setZoomGesturesEnabled(true); // Enable zoom gestures
        drawMarkers();
    }
}
