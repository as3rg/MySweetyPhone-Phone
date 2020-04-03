package com.mysweetyphone.phone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.regex.Pattern;

public class RegDevice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg_device);
        Toolbar toolbar = findViewById(R.id.toolbarADDPHONE);
        setSupportActionBar(toolbar);
        TextView title = findViewById(R.id.titleADDPHONE);
        Shader textShader = new LinearGradient(0, 0, title.getMeasuredWidth(),title.getLineHeight(),
                new int[]{
                        Color.parseColor("#d53369"),
                        Color.parseColor("#cbad6d"),
                }, null, Shader.TileMode.CLAMP);
        title.getPaint().setShader(textShader);
    }

    public void onAddPhoneClick(View view){
        TextView PhoneName = findViewById(R.id.PhoneNameADDPHONE);
        TextView ErrorText = findViewById(R.id.ErrorADDPHONE);

        if (!Pattern.matches("\\w+", PhoneName.getText().toString())) {
            ErrorText.setText(R.string.invalid_name);
            ErrorText.setVisibility(View.VISIBLE);
            return;
        }
        ErrorText.setVisibility(View.INVISIBLE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("name", PhoneName.getText().toString());
        editor.putInt("code", (int)(System.currentTimeMillis() % 1000000));
        editor.apply();
        Intent intent;
        if (getIntent().getStringExtra(Intent.EXTRA_TEXT) != null)
            intent = new Intent(getApplicationContext(), ChooseWayToSend.class);
        else intent = new Intent(getApplicationContext(), Main.class);
        intent.putExtras(getIntent());
        intent.setAction(getIntent().getAction());
        startActivity(intent);
        finish();


    }
}
