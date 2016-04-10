package de.rowekamp.geomessage;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.provider.ContactsContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Kiteflyer on 06.04.2016.
 */
public class ListMessage {

    private String phoneNumber;
    private String nearestLocation;
    private Long expiryDate; //time in ms since Unix clock start
    private Location target;
    private Location currentLocation;
    private Context context;
    private final long id;
    private int radius; //access radius around target location in meter

    public ListMessage(long id, String phoneNumber, String nearestLocation, Long expiryDate, Location target, int radius, Location currentLocation, Context context) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.nearestLocation = nearestLocation;
        this.expiryDate = expiryDate;
        this.target = target;
        this.radius = radius;
        this.currentLocation = currentLocation;
        this.context = context;
    }

    public long getId(){
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getNearestLocation(){
        return nearestLocation;
    }

    public Long getExpiryDate() {
        return expiryDate;
    }

    public Location getCurrentLocation(){
        return currentLocation;
    }

    public int getRadius(){
        return radius;
    }

    public void setCurrentLocation(Location currentLocation){
        this.currentLocation = currentLocation;
    }

    public Location getTarget() {
        return target;
    }

    /**
     * Returns the contact name assigned to the phone number in the telephone book.
     * If there is no assigned name, returns null.
     * @return contact name assigned to phone number
     */
    public String getContactName(){
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = context.getContentResolver().query(uri, new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, null, null, null);
        if(cursor.moveToFirst()){
            String contactName = cursor.getString(0);
            cursor.close();
            return contactName;
        }
        else return null;
    }

    /**
     * Returns the expiry date in a human readable scale.
     * Returns null if the message has no expiry date.
     * @return expiry date
     */
    public String getExpiryDateHR() {
        if (expiryDate != null) {
            Date timeout = new Date(expiryDate);
            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
            return dateFormat.format(timeout);
        } else return null;
    }
}
