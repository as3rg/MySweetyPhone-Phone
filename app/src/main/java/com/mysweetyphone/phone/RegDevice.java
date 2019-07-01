package com.mysweetyphone.phone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class RegDevice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg_device);

    }

    public void onAddPhoneClick(View view){
        try {
            int id = (PreferenceManager.getDefaultSharedPreferences(this)).getInt("id", -1);
            String login = PreferenceManager.getDefaultSharedPreferences(this).getString("login", "");
            TextView PhoneName = findViewById(R.id.PhoneNameADDPHONE);
            TextView ErrorText = findViewById(R.id.ErrorADDPHONE);
            
            if (!Pattern.matches("\\w+", PhoneName.getText().toString())) {
                ErrorText.setText(R.string.invalid_name);
                ErrorText.setVisibility(View.VISIBLE);
                return;
            }
            if (login.isEmpty()){
                ErrorText.setVisibility(View.INVISIBLE);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putString("name", PhoneName.getText().toString());
                editor.putInt("regdate", (int)System.currentTimeMillis()/1000);
                editor.apply();
                Intent intent;
                if (getIntent().getStringExtra(Intent.EXTRA_TEXT) != null)
                    intent = new Intent(getApplicationContext(), ChooseWayToSend.class);
                else intent = new Intent(getApplicationContext(), Main.class);
                intent.putExtras(getIntent());
                intent.setAction(getIntent().getAction());
                startActivity(intent);
                finish();
                return;
            }

            AsyncHttpClient client = new AsyncHttpClient();
            client.get("http://mysweetyphone.herokuapp.com/?Type=AddDevice&DeviceType=Phone&Id=" + id + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Name=" + URLEncoder.encode(PhoneName.getText().toString(), "UTF-8"), new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                    try {
                        switch (responseBody.getInt("code")) {
                            case 3:
                                ErrorText.setText(R.string.FillNameADDPHONE);
                                ErrorText.setVisibility(View.VISIBLE);
                                break;
                            case 2:
                                ErrorText.setText(R.string.Exception);
                                ErrorText.setVisibility(View.VISIBLE);
                                break;
                            case 1:
                                ErrorText.setText(R.string.NameExistADDPHONE);
                                ErrorText.setVisibility(View.VISIBLE);
                                break;
                            case 0:
                                ErrorText.setVisibility(View.INVISIBLE);
                                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = sharedPreferences.edit();

                                editor.putString("name", PhoneName.getText().toString());
                                editor.putInt("regdate", responseBody.getInt("regdate"));
                                editor.apply();
                                Intent intent;
                                if (getIntent().getStringExtra(Intent.EXTRA_TEXT) != null)
                                    intent = new Intent(getApplicationContext(), ChooseWayToSend.class);
                                else intent = new Intent(getApplicationContext(), Main.class);
                                intent.putExtras(getIntent());
                                intent.setAction(getIntent().getAction());
                                startActivity(intent);
                                finish();
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
