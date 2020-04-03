package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
    private static boolean isOnline = true;
    String name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.starting_activity);
        name = (PreferenceManager.getDefaultSharedPreferences(this)).getString("name", "");
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE}, 124);
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
        if (requestCode == 124 && grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if(!name.isEmpty())
                if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND) && getIntent().getStringExtra(Intent.EXTRA_TEXT) != null) {
                    ChangeActivity(ChooseWayToSend.class);
                } else
                    ChangeActivity(Main.class);
            else
                ChangeActivity(RegDevice.class);
        } else {
            finish();
        }
    }

    public void Offline(View v){
        isOnline = false;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("name");
        editor.apply();
        ChangeActivity(RegDevice.class);
    }
}
