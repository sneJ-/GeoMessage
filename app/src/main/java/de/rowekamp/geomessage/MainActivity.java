package de.rowekamp.geomessage;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private String provider;
    private LocationManager locationManager;
    private List<ListMessage> values;
    private ListMessageAdapter adapter;
    private long hash = 0L;
    private String phoneNumber;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //extract phone number
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        phoneNumber = tMgr.getLine1Number();

        //check if mobile has registered itself before on the server
        queue = Volley.newRequestQueue(this);
        SharedPreferences mPrefs = getSharedPreferences("label", 0);
        boolean registered = mPrefs.getBoolean("registered", false);
        if (!registered) {
            registerOnServer();
        }

        //Location Manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location currentLocation = locationManager.getLastKnownLocation(provider);

        //ListView
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("latitude", adapter.getItem(position).getTarget().getLatitude());
                intent.putExtra("longitude", adapter.getItem(position).getTarget().getLongitude());
                intent.putExtra("contactName", adapter.getItem(position).getContactName());
                intent.putExtra("radius", adapter.getItem(position).getRadius());
                intent.putExtra("phoneNumber", adapter.getItem(position).getPhoneNumber());
                intent.putExtra("messageId", adapter.getItem(position).getId());
                startActivity(intent);
            }
        });

        values = new ArrayList<ListMessage>();

        //Gets and adds all messages to the list
        getMessages(currentLocation);

        adapter = new ListMessageAdapter(this, R.layout.list_message, values);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        Location currentLocation = locationManager.getLastKnownLocation(provider);
        getMessages(currentLocation);
        for (ListMessage m : values) {
            m.setCurrentLocation(currentLocation);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location currentLocation) {
        for (ListMessage m : values) {
            m.setCurrentLocation(currentLocation);
        }
        adapter.notifyDataSetChanged();
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

    /**
     * Registers the phone on the server
     */
    private void registerOnServer() {
        String url = "https://geo.rowekamp.de/register.php?nr=" + phoneNumber.substring(1);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                SharedPreferences mPrefs = getSharedPreferences("label", 0);
                SharedPreferences.Editor mEditor = mPrefs.edit();
                mEditor.putBoolean("registered", true).commit();
                Toast.makeText(MainActivity.this, "Number " + phoneNumber + " successfully registered", Toast.LENGTH_SHORT).show();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error: Number " + phoneNumber + " not registered", Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(stringRequest);
    }

    /**
     * Gets and adds all messages to the list
     */
    private void getMessages(final Location currentLocation) {
        //Get hash from sever
        String checkUrl = "https://geo.rowekamp.de/checkMsgList.php?nr=" + phoneNumber.substring(1);
        String reqUrl = "https://geo.rowekamp.de/fetchMsgList.php?nr=" + phoneNumber.substring(1);

        //check the message opened flag and reset the hash if true to trigger msgList reload
        SharedPreferences mPrefs = getSharedPreferences("opened", 1);
        boolean opened = mPrefs.getBoolean("opened", false);
        if(opened){
            hash = 0L;
            SharedPreferences.Editor mEditor = mPrefs.edit();
            mEditor.putBoolean("opened", false).commit();
        }

        //msgListRequest
        final JsonArrayRequest msgListRequest = new JsonArrayRequest(reqUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                if(response.length()>0){
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject jo = response.getJSONObject(i);
                            Location target = new Location("");
                            target.setLatitude(jo.getDouble("latitude"));
                            target.setLongitude(jo.getDouble("longitude"));
                            Long expiryDate = jo.getLong("expiryDate");
                            if(expiryDate == -1){
                                expiryDate = null;
                            }
                            ListMessage msg = new ListMessage(jo.getLong("id"),"+"+jo.getString("sender"),
                                    jo.getString("description"),expiryDate,target,
                                    jo.getInt("radius"),currentLocation,MainActivity.this);
                            values.add(msg);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        hash = -1;
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error: Number msgList not found.", Toast.LENGTH_SHORT).show();
            }
        });

        //hash request
        JsonArrayRequest hashRequest = new JsonArrayRequest(checkUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                if(response.length()>0){
                    try {
                        long serverHash = response.getJSONObject(0).optLong("hash");
                        if(hash != serverHash){
                            hash = serverHash;
                            values.clear();
                            queue.add(msgListRequest);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        hash = -1;
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error: Number hash not found.", Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(hashRequest);
    }

    /**
     * ACTION BAR
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_toolbar, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
        startActivity(intent);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
