package com.mysweetyphone.phone;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

public class DevicesList extends Fragment {
    int regdate;
    int id;
    String name;
    String login;
    Set<String> names = new HashSet();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_devices_list, container, false);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        try {
            regdate = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("regdate", -1);
            id = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("id", -1);
            login = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("login", "");
            name = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("name", "");

            TableLayout layout = getActivity().findViewById(R.id.tableDEVICESLIST);

            AsyncHttpClient client = new AsyncHttpClient();
            client.get("http://mysweetyphone.herokuapp.com/?Type=ShowDevices&RegDate=" + regdate + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&MyName=" + URLEncoder.encode(name, "UTF-8"), new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                    try {
                        if (responseBody.getInt("code") == 4) {
                            Toast toast = Toast.makeText(getActivity(),
                                    "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                            toast.show();
                            getActivity().finish();
                        } else {
                            names.clear();
                            printDevices(responseBody.getJSONArray("PCs"), false, printDevices(responseBody.getJSONArray("Phones"), true, 0, layout), layout);
                        }
                    } catch (JSONException | NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private int printDevices(JSONArray arr, boolean isPhone, int odd, TableLayout table) throws JSONException {
        if (table == null) return 0;
        int i = 0;
        for (; i < arr.length(); i++){
            if(names.contains(arr.getString(i))) continue;
            names.add(arr.getString(i));
            final TableRow row = new TableRow(getActivity());
            final TextView DeviceName = new TextView(getActivity());
            final Button RemoveButton = new Button(getActivity());
            ImageView Icon = new ImageView(getActivity());
            Icon.setImageResource(isPhone ? R.drawable.ic_devices_list_phone : R.drawable.ic_devices_list_pc);
            Icon.setPadding(150,30,0,0);
            Icon.setImageTintList(ColorStateList.valueOf(0xFFFFFFFF));
            DeviceName.setText(arr.getString(i));
            DeviceName.setPadding(20,0,0,0);
            DeviceName.setTextSize(22);
            RemoveButton.setText("Удалить");
            if (arr.getString(i).equals(name)) {
                RemoveButton.setBackground(ContextCompat.getDrawable(getActivity(),R.drawable.ic_devices_list_this_phone));
                    Shader textShader = new LinearGradient(0, 0, RemoveButton.getPaint().measureText(RemoveButton.getText().toString()),RemoveButton.getTextSize(),
                        new int[]{
                                Color.parseColor("#fc354c"),
                                Color.parseColor("#0ABFBC"),
                        }, null, Shader.TileMode.CLAMP);
                RemoveButton.getPaint().setShader(textShader);
            }else {
                RemoveButton.setTextColor(Color.WHITE);
                RemoveButton.setBackground(ContextCompat.getDrawable(getActivity(),R.drawable.ic_style_button_background));
            }
            RemoveButton.setOnClickListener(v -> {
                try {
                    final TableRow parent = (TableRow) v.getParent();
                    final TextView DeviceName1 = (TextView) parent.getChildAt(2);
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.get("http://mysweetyphone.herokuapp.com/?Type=RemoveDevice&RegDate=" + regdate + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&Name=" + URLEncoder.encode(DeviceName1.getText().toString(), "UTF-8") + "&MyName=" + URLEncoder.encode(name, "UTF-8"), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                            try {
                                if (responseBody.getInt("code") == 4 || DeviceName1.getText().equals(name)) {
                                    Toast toast = Toast.makeText(getActivity(),
                                            "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                                    toast.show();
                                    getActivity().finish();
                                }
                                table.removeView(parent);
                                for (int i1 = 0; i1 < table.getChildCount(); i1++)
                                    table.getChildAt(i1).setBackgroundColor((i1 % 2 == 0) ? 0xFF252525 : 0xFF202020);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            });
            row.addView(RemoveButton);
            row.addView(Icon);
            row.addView(DeviceName);
            if ((odd+i) % 2 == 0)
                row.setBackgroundColor(0xFF252525);
            table.addView(row);
            TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams(row.getLayoutParams());
            tableRowParams.setMargins(0, 0, 0, 10);
            row.setLayoutParams(tableRowParams);
        }
        return ((odd+i) % 2 == 0) ? 0 : 1 ;
    }
}