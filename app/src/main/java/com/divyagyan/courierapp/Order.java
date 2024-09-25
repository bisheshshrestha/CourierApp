public class Order {
    public String trackingNumber;
    public String userUid;
    public String packageDetails;
    public String recipientName;
    public String recipientPhone;
    public double pickupLat;
    public double pickupLng;
    public double deliveryLat;
    public double deliveryLng;
    public double price;
    public String orderCreationTime;

    public Order() {}

    public Order(String trackingNumber, String userUid, String packageDetails, String recipientName,
                 String recipientPhone, double pickupLat, double pickupLng,
                 double deliveryLat, double deliveryLng, double price, String orderCreationTime) {
        this.trackingNumber = trackingNumber;
        this.userUid = userUid;
        this.packageDetails = packageDetails;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.deliveryLat = deliveryLat;
        this.deliveryLng = deliveryLng;
        this.price = price;
        this.orderCreationTime = orderCreationTime;
    }
}
