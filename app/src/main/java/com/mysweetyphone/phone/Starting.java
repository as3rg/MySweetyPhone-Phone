package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Starting extends AppCompatActivity {
    int id;
    int regdate;
    private static boolean isOnline = true;
    String login;
    String name;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.starting_activity);
        id = (PreferenceManager.getDefaultSharedPreferences(this)).getInt("id", -1);
        regdate = (PreferenceManager.getDefaultSharedPreferences(this)).getInt("regdate", -1);
        login = (PreferenceManager.getDefaultSharedPreferences(this)).getString("login", "");
        name = (PreferenceManager.getDefaultSharedPreferences(this)).getString("name", "");
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE}, 124);
    }

    private void Request() throws UnsupportedEncodingException {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=Check&DeviceType=Phone&Login=" + URLEncoder.encode(login, "UTF-8") + "&RegDate=" + regdate + "&Id=" + id + "&Name=" + URLEncoder.encode(name, "UTF-8"), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                try {
                    if (responseBody.getInt("result") == 1 && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND)) {
                            if (getIntent().getParcelableExtra(Intent.EXTRA_STREAM) != null)
                                ChangeActivity(Main.class);
                            else if (getIntent().getStringExtra(Intent.EXTRA_TEXT) != null)
                            ChangeActivity(ChooseWayToSend.class);
                    } else if (responseBody.getInt("result") == 1) {
                        ChangeActivity(Main.class);
                    } else if (responseBody.getInt("result") == 2) {
                        ChangeActivity(RegDevice.class);
                    } else {
                        ChangeActivity(Login.class);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ChangeActivity(Login.class);
                }
            }

            public void onFailure(){
                try {
                    if(isOnline) Request();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onFailure();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                onFailure();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                onFailure();
            }
        });
    }

    private void ChangeActivity(Class<?> cls){
        Intent intent = new Intent(this, cls);
        intent.putExtras(getIntent());
        intent.setAction(getIntent().getAction());
        startActivity(intent);
        finish();
    }

    @SuppressLint("ShowToast")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            if (requestCode == 124 && grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(!name.isEmpty() && login.isEmpty())
                    if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND) && getIntent().getStringExtra(Intent.EXTRA_TEXT) != null) {
                        ChangeActivity(ChooseWayToSend.class);
                    } else
                        ChangeActivity(Main.class);
                else
                    Request();
            } else {
                finish();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void Offline(View v){
        isOnline = false;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("id");
        editor.remove("name");
        editor.remove("login");
        editor.apply();
        ChangeActivity(RegDevice.class);
    }
}
