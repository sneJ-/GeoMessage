package de.rowekamp.geomessage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

public class ViewActivity extends AppCompatActivity {

    private String sender;
    private RequestQueue queue;
    private String description;
    private String message;
    private Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        //extract phone number
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = tMgr.getLine1Number();

        //Get data from mapsActivity
        Intent intent = getIntent();
        long messageId = intent.getLongExtra("messageId",0);
        double latitude = intent.getDoubleExtra("latitude",0);
        double longitude = intent.getDoubleExtra("longitude",0);
        sender = intent.getStringExtra("sender");

        //fetch message from server
        queue = Volley.newRequestQueue(this);
        String url = "https://geo.rowekamp.de/fetchMsg.php?nr=" + phoneNumber.substring(1)+"&lat="+latitude
                +"&long="+longitude+"&id="+messageId;

        JsonArrayRequest messageRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                if(response.length()>0){
                    try {
                        description = response.getJSONObject(0).optString("description");
                        message = response.getJSONObject(0).optString("message");
                        byte[] decodedStringImage = Base64.decode(response.getJSONObject(0).optString("image"), Base64.DEFAULT);
                        image = BitmapFactory.decodeByteArray(decodedStringImage, 0, decodedStringImage.length);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    TextView contactTextView = (TextView) findViewById(R.id.contactTextView);
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    TextView descriptionTextView = (TextView) findViewById(R.id.descriptionTextView);
                    TextView messageTextView = (TextView) findViewById(R.id.messageTextView);

                    if(sender != null) contactTextView.setText(sender);
                    if(image != null) imageView.setImageBitmap(image);
                    if(description != null) descriptionTextView.setText(description);
                    if(message != null) messageTextView.setText(message);

                    //set message opened flag true to trigger reload
                    SharedPreferences mPrefs = getSharedPreferences("opened", 1);
                    SharedPreferences.Editor mEditor = mPrefs.edit();
                    mEditor.putBoolean("opened", true).commit();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ViewActivity.this, "Error: Message not found.", Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(messageRequest);
    }
}
