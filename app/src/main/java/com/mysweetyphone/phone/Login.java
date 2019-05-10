package com.mysweetyphone.phone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONObject;

public class Login extends AppCompatActivity {

    private boolean RegOrLogin = false;     //Reg == true, Login == false
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_login);
    }

    public void onModeChanged(View view){
        Button LoginButton = findViewById(R.id.LoginLOGIN);
        switch (view.getId()){
            case R.id.RegRatioLOGIN:
                LoginButton.setText(R.string.RegLOGIN);
                break;
            case R.id.LoginRatioLOGIN:
                LoginButton.setText(R.string.log_inLOGIN);
        }
    }

    public void onLoginClick(View view){
        RegOrLogin = ((RadioButton)findViewById(R.id.RegRatioLOGIN)).isChecked();

        TextView Nick = findViewById(R.id.NickLOGIN);
        TextView Pass = findViewById(R.id.PasswordLOGIN);
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=" + (RegOrLogin ? "Reg" : "Login") + "&Login="+Nick.getText()+"&Pass="+Pass.getText(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                try {
                    TextView ErrorText = findViewById(R.id.ErrorLOGIN);
                    switch (responseBody.getInt("code")) {
                        case 3:
                            ErrorText.setText(R.string.FillNameAndPassLOGIN);
                            ErrorText.setVisibility(View.VISIBLE);
                            break;
                        case 2:
                            ErrorText.setText(R.string.Exception);
                            ErrorText.setVisibility(View.VISIBLE);
                            break;
                        case 1:
                            ErrorText.setText(RegOrLogin ? R.string.ErrorRegingLOGIN : R.string.ErrorLoggingInLOGIN);
                            ErrorText.setVisibility(View.VISIBLE);
                            break;
                        case 0:
                            ErrorText.setVisibility(View.INVISIBLE);
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = sharedPreferences.edit();

                            editor.putInt("id", responseBody.getInt("id"));

                            TextView Nick = findViewById(R.id.NickLOGIN);
                            editor.putString("login", Nick.getText().toString());
                            editor.commit();
                            Intent intent = new Intent(getApplicationContext(), RegDevice.class);
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
