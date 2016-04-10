package de.rowekamp.geomessage;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SendMsgActivity extends AppCompatActivity implements LocationListener {
    private String receipientName;
    private String receipientNumber;
    private String phoneNumber;
    private String provider;
    private Bitmap attachedImage;
    private LocationManager locationManager;
    private Location currentLocation;
    private int radius = 2;
    private long expiryDate = -1;
    private ImageView imageView;
    private EditText descriptionEditText;
    private EditText messageEditText;
    private RequestQueue queue;
    private String imageEncoded = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_msg);

        //Get data from Contacts Activity
        Intent intent = getIntent();
        receipientNumber = intent.getStringExtra("receipientNumber");
        receipientName = intent.getStringExtra("receipientName");
        this.setTitle("Send to "+receipientName);

        //extract phone number
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        phoneNumber = tMgr.getLine1Number();

        //Location Manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location currentLocation = locationManager.getLastKnownLocation(provider);

        imageView = (ImageView) findViewById(R.id.attachedImageView);
        descriptionEditText = (EditText) findViewById(R.id.descriptionEditText);
        messageEditText = (EditText) findViewById(R.id.messageEditText);

        queue = Volley.newRequestQueue(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_toolbar, menu);
        return true;
    }

    /**
     * IconBar
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getTitle().equals("attach image")) attachImage();
        if(item.getTitle().equals("send message")) sendMessage();
        if(item.getTitle().equals("set timeout")) setTimeout();
        if(item.getTitle().equals("set radius")) setRadius();
        return true;
    }

    private void attachImage(){
        //Image attach options
        final CharSequence[] options = { "Take Photo", "Choose from Library", "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, 0);
                } else if (options[item].equals("Choose from Library")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(
                            Intent.createChooser(intent, "Select File"), 1);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 0) { //request camera
                attachedImage = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == 1) { //request file
                Uri selectedImageUri = data.getData();
                String[] projection = {MediaStore.MediaColumns.DATA};
                CursorLoader cursorLoader = new CursorLoader(this, selectedImageUri, projection, null, null,
                        null);
                Cursor cursor = cursorLoader.loadInBackground();
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                String selectedImagePath = cursor.getString(column_index);
                attachedImage = BitmapFactory.decodeFile(selectedImagePath);
            }
            imageView.setImageBitmap(attachedImage);
        }
    }

    private void sendMessage() {
        if(currentLocation != null) {
            String url = "https://geo.rowekamp.de/sendMsg.php";

            //encode image with Base64
            if (attachedImage != null) {
                ByteArrayOutputStream ba = new ByteArrayOutputStream();
                attachedImage.compress(Bitmap.CompressFormat.JPEG, 100, ba);
                byte[] b = ba.toByteArray();
                imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);
            }

            StringRequest sendMsg = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(response.substring(0,2).equals("OK")){
                        Toast.makeText(SendMsgActivity.this, "message sent", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(SendMsgActivity.this,MainActivity.class);
                        startActivity(i);
                    }
                    else{
                        Toast.makeText(SendMsgActivity.this, "Error: message couldn't be sent", Toast.LENGTH_SHORT).show();
                    }
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(SendMsgActivity.this, "Error: message coudln't be sent", Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("sender", phoneNumber.substring(1));
                    params.put("receipient", receipientNumber.substring(1));
                    params.put("latitude", ""+currentLocation.getLatitude());
                    params.put("longitude", ""+currentLocation.getLongitude());
                    params.put("description",descriptionEditText.getText().toString());
                    params.put("message",messageEditText.getText().toString());
                    params.put("image",imageEncoded);
                    params.put("expiryDate",expiryDate+"");
                    params.put("radius",radius+"");
                    return params;
                }
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("Content-Type","application/x-www-form-urlencoded");
                    return params;
                }
            };
            queue.add(sendMsg);
        }
        else{
            Toast.makeText(this, "Sending failed. Please activate your GPS.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setRadius(){
        Toast.makeText(this, "radius currently not supported", Toast.LENGTH_SHORT).show();
    }

    private void setTimeout(){
        Toast.makeText(this, "expiry date currently not supported", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        currentLocation = locationManager.getLastKnownLocation(provider);
    }
}
