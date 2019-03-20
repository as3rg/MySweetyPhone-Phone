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


public class Login extends AppCompatActivity {

    private boolean RegOrLogin = false;     //Reg == true, Login == false
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
                        setTheme(R.style.AppTheme);
                        setContentView(R.layout.activity_login);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    setTheme(R.style.AppTheme);
                    setContentView(R.layout.activity_login);
                }
            }
        });
    }

    public void onLoginClick(View view){
        RegOrLogin = view == findViewById(R.id.RegLOGIN);

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
                            startActivity(intent);
                            finish();
                            break;
                    }
                }catch (Exception e){
                    Toast toast = Toast.makeText(getBaseContext(),
                            e.getMessage(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    //Должно вызываться после получения результата асинхронного Get-Запроса, но этот код не может выполняться внутри анонимного класса
    private void ChangeActivity(Class<?> cls){
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        finish();
    }
}
