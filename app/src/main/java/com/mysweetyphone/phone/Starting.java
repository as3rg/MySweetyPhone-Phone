package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.apache.http.Header;
import org.json.JSONObject;

public class Starting extends AppCompatActivity {
    int id;
    int regdate;
    String login;
    String name;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        id = (PreferenceManager.getDefaultSharedPreferences(this)).getInt("id", -1);
        regdate = (PreferenceManager.getDefaultSharedPreferences(this)).getInt("regdate", -1);
        login = (PreferenceManager.getDefaultSharedPreferences(this)).getString("login", "");
        name = (PreferenceManager.getDefaultSharedPreferences(this)).getString("name", "");
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                || PermissionChecker.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                || PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS}, 124);
        }else{
            AsyncHttpClient client = new AsyncHttpClient();
            client.get("http://mysweetyphone.herokuapp.com/?Type=Check&DeviceType=Phone&Login="+ login +"&RegDate="+ regdate +"&Id="+ id +"&Name="+ name, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                    try {
                        if(responseBody.getInt("result") == 1 && getIntent().getAction() == Intent.ACTION_SEND) {
                            if(getIntent().getParcelableExtra(Intent.EXTRA_STREAM) != null)
                                ChangeActivity(Main.class);
                            else if(getIntent().getStringExtra(Intent.EXTRA_TEXT) != null)
                                ChangeActivity(ChooseWayToSend.class);
                        }else if (responseBody.getInt("result") == 1) {
                            ChangeActivity(Main.class);
                        } else if (responseBody.getInt("result") == 2) {
                            ChangeActivity(RegDevice.class);
                        }else{
                            ChangeActivity(Login.class);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        ChangeActivity(Login.class);
                    }
                }
            });
        }
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
        switch (requestCode) {
            case 124: {
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    AsyncHttpClient client = new AsyncHttpClient();
                    client.get("http://mysweetyphone.herokuapp.com/?Type=Check&DeviceType=Phone&Login="+ login +"&RegDate="+ regdate +"&Id="+ id +"&Name="+ name, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                            try {
                                if(responseBody.getInt("result") == 1 && getIntent().getAction() == Intent.ACTION_SEND) {
                                    if(getIntent().getParcelableExtra(Intent.EXTRA_STREAM) != null)
                                        ChangeActivity(Main.class);
                                    else if(getIntent().getStringExtra(Intent.EXTRA_TEXT) != null)
                                        ChangeActivity(ChooseWayToSend.class);
                                }else if (responseBody.getInt("result") == 1) {
                                    ChangeActivity(Main.class);
                                } else if (responseBody.getInt("result") == 2) {
                                    ChangeActivity(RegDevice.class);
                                }else{
                                    ChangeActivity(Login.class);
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                                ChangeActivity(Login.class);
                            }
                        }
                    });
                } else {
                    finish();
                }
            }
        }
    }
}
