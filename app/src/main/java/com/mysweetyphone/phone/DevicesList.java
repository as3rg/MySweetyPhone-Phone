package com.mysweetyphone.phone;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

public class DevicesList extends Fragment {
    int regdate;
    int id;
    String name;
    String login;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_devices_list, container, false);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        regdate = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("regdate", -1);
        id = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("id", -1);
        login = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("login", "");
        name = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("name","");

        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=ShowDevices&RegDate="+regdate+"&Login=" + login + "&Id=" + id + "&MyName=" + name, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                try {
                    if (responseBody.getInt("code") == 4){
                        Toast toast = Toast.makeText(getContext(),
                                "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                        toast.show();
                        getActivity().finish();
                    }else
                        printDevices(responseBody.getJSONArray("PCs"),false, printDevices(responseBody.getJSONArray("Phones"),true, 0 ) );
                }catch (Exception e){
                    Toast toast = Toast.makeText(getContext(),
                            e.getMessage(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private int printDevices(JSONArray arr, boolean isPhone, int odd) throws Exception {
        final TableLayout table = getView().findViewById(R.id.tableDEVICESLIST);
        int i = 0;
        for (; i < arr.length(); i++){
            final TableRow row = new TableRow(getContext());
            final TextView DeviceName = new TextView(getContext());
            final Button RemoveButton = new Button(getContext());
            ImageView Icon = new ImageView(getContext());
            Icon.setImageResource(isPhone ? R.drawable.ic_devices_list_phone : R.drawable.ic_devices_list_pc);
            Icon.setPadding(150,30,0,0);
            DeviceName.setText(arr.getString(i));
            DeviceName.setPadding(20,0,0,0);
            DeviceName.setTextSize(22);
            if (arr.getString(i).equals(name)) DeviceName.setTextColor(Color.RED);
            RemoveButton.setTextColor(Color.RED);
            RemoveButton.setText("Удалить");
            RemoveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final TableRow parent = (TableRow) v.getParent();
                    final TextView DeviceName = (TextView)parent.getChildAt(2);
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.get("http://mysweetyphone.herokuapp.com/?Type=RemoveDevice&RegDate="+regdate+"&Login=" + login + "&Id=" + id + "&Name=" + DeviceName.getText() + "&MyName=" + name, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                            try {
                                if (responseBody.getInt("code") == 4 || DeviceName.getText().equals(name)){
                                    Toast toast = Toast.makeText(getContext(),
                                            "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                                    toast.show();
                                    getActivity().finish();
                                }
                                table.removeView(parent);
                                for (int i = 0; i < table.getChildCount(); i++)
                                    table.getChildAt(i).setBackgroundColor((i % 2 == 0) ? 0xFFE6E6E6 : Color.WHITE);

                            }catch (Exception e){
                                Toast toast = Toast.makeText(getContext(),
                                        e.getMessage(), Toast.LENGTH_LONG);
                                toast.show();
                            }

                        }
                    });
                }
            });
            row.addView(RemoveButton);
            row.addView(Icon);
            row.addView(DeviceName);
            if ((odd+i) % 2 == 0)
                row.setBackgroundColor(0xFFE6E6E6);
            table.addView(row);
        }
        return ((odd+i) % 2 == 0) ? 0 : 1 ;
    }
}