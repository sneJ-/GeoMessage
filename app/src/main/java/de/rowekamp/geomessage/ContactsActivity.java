package de.rowekamp.geomessage;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private List<Contact> contactList;
    private List<String> phoneBook;
    private ContactListAdapter contactListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);


        //ListView
        ListView listView = (ListView) findViewById(R.id.contactListView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ContactsActivity.this, SendMsgActivity.class);
                intent.putExtra("receipientName", contactListAdapter.getItem(position).getName());
                intent.putExtra("receipientNumber", contactListAdapter.getItem(position).getNumber());
                startActivity(intent);
            }
        });

        contactList = new ArrayList<Contact>();

        //fill contact list
        RequestQueue queue = Volley.newRequestQueue(this);
        String bookUrl = "https://geo.rowekamp.de/fetchBook.php";
        JsonArrayRequest hashRequest = new JsonArrayRequest(bookUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for(int i=0; i<response.length(); i++){
                    try{
                        String number = ("+"+response.getJSONObject(i).optString("number"));
                        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
                        Cursor cursor = ContactsActivity.this.getContentResolver().query(uri, new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, null, null, null);
                        if(cursor.moveToFirst()){
                            String contactName = cursor.getString(0);
                            cursor.close();
                            contactList.add(new Contact(contactName,number));
                        }
                    }catch(JSONException e){
                        e.printStackTrace();
                    }
                }
                contactListAdapter.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ContactsActivity.this, "Error: Book couldn't be fetched", Toast.LENGTH_SHORT).show();
            }
        });

        contactListAdapter = new ContactListAdapter(this, R.layout.contact_list_message, contactList);
        listView.setAdapter(contactListAdapter);
        queue.add(hashRequest);
    }


    public class Contact {
        private String number;
        private String name;

        public Contact(String name, String number) {
            this.number = number;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }
    }

    public class ContactListAdapter extends ArrayAdapter<Contact> {
        public ContactListAdapter(Context context, int resource, List<Contact> values) {
            super(context, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.contact_list_message, null);
            }

            Contact c = getItem(position);

            if (c != null) {
                TextView tt1 = (TextView) v.findViewById(R.id.contact_list_contact_name);
                TextView tt2 = (TextView) v.findViewById(R.id.contact_list_contact_number);
                tt1.setText(c.getName());
                tt2.setText(c.getNumber());
            }
            return v;
        }
    }
}