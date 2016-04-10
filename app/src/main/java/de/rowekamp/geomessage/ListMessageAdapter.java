package de.rowekamp.geomessage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Kiteflyer on 06.04.2016.
 */
public class ListMessageAdapter extends ArrayAdapter<ListMessage> {

    public ListMessageAdapter(Context context, int resource, List<ListMessage> values) {
        super(context, resource, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_message, null);
        }

        ListMessage m = getItem(position);

        if (m != null) {
            //TODO image
            TextView tt1 = (TextView) v.findViewById(R.id.contactName);
            TextView tt2 = (TextView) v.findViewById(R.id.nearestLocation);
            TextView tt3 = (TextView) v.findViewById(R.id.airLineDistance);
            TextView tt4 = (TextView) v.findViewById(R.id.expirationTime);

            if (tt1 != null) {
                if(m.getContactName() != null) tt1.setText(m.getContactName());
                else tt1.setText(m.getPhoneNumber());
            }

            if(tt2 != null){
                tt2.setText(m.getNearestLocation());
            }

            if (tt3 != null) {
                if(m.getCurrentLocation() != null){
                    float distance = m.getCurrentLocation().distanceTo(m.getTarget());
                    if (distance > 1000){
                        tt3.setText(distance/1000 + "km");
                    } else{
                        tt3.setText(distance + "m");
                    }
                }
                else{
                    tt3.setText("Distance couldn't be estimated. Please activate your GPS.");
                }
            }

            if (tt4 != null) {
                tt4.setText(m.getExpiryDateHR());
                tt4.setEnabled(true);
            }
        }

        return v;
    }
}
