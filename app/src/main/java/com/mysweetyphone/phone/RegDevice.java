package com.mysweetyphone.phone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class RegDevice extends AppCompatActivity {
    private int id;
    private String login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg_device);

    }

    public void onAddPhoneClick(View view){
        id = (PreferenceManager.getDefaultSharedPreferences(this)).getInt("id",-1);
        login  = (PreferenceManager.getDefaultSharedPreferences(this)).getString("login","");
        TextView PhoneName = findViewById(R.id.PhoneNameADDPHONE);
        TextView ErrorText = findViewById(R.id.ErrorADDPHONE);

        if(!Pattern.matches("\\w+",PhoneName.getText().toString())){
            ErrorText.setText(R.string.invalid_nameADDPHONE);
            ErrorText.setVisibility(View.VISIBLE);
            return;
        }
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=AddDevice&DeviceType=Phone&Id="+id+"&Login="+login+"&Name="+PhoneName.getText(), new JsonHttpResponseHandler() {
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

                            TextView Name = findViewById(R.id.PhoneNameADDPHONE);
                            editor.putString("name", Name.getText().toString());
                            editor.putInt("regdate", responseBody.getInt("regdate"));
                            editor.commit();
                            Intent intent;
                            if(getIntent().getStringExtra(Intent.EXTRA_TEXT) != null)
                                intent = new Intent(getApplicationContext(), ChooseWayToSend.class);
                            else intent = new Intent(getApplicationContext(), Main.class);
                            intent.putExtras(getIntent());
                            intent.setAction(getIntent().getAction());
                            startActivity(intent);
                            finish();
                            break;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
