package com.mysweetyphone.phone;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONObject;

public class Starting extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        int id = (PreferenceManager.getDefaultSharedPreferences(this)).getInt("id", -1);
        int regdate = (PreferenceManager.getDefaultSharedPreferences(this)).getInt("regdate", -1);
        String login = (PreferenceManager.getDefaultSharedPreferences(this)).getString("login", "");
        String name = (PreferenceManager.getDefaultSharedPreferences(this)).getString("name", "");

        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=Check&DeviceType=Phone&Login="+ login +"&RegDate="+ regdate +"&Id="+ id +"&Name="+ name, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                try {
                    if (responseBody.getInt("result") == 1) {
                        ChangeActivity(Main.class);
                    }else if (responseBody.getInt("result") == 2) {
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

    private void ChangeActivity(Class<?> cls){
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        finish();
    }
}
