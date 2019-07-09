package com.mysweetyphone.phone;

import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethod;
import android.widget.Button;
import android.widget.ScrollView;

import org.json.JSONException;

import java.io.IOException;

import Utils.Session;

public class IME extends InputMethodService{

    Button NewSession;
    public View layout;
    private ScrollView scrollView;

    @Override
    public View onCreateInputView() {
        View kv = getLayoutInflater().inflate(R.layout.keyboard_keyboard, null);
        Button Open = kv.findViewById(R.id.openKEYBOARD);
        Open.setOnClickListener(this::OpenList);
        layout = kv.findViewById(R.id.alertsKEYBOARD);
        scrollView = kv.findViewById(R.id.scrollViewKEYBOARD);
        NewSession = kv.findViewById(R.id.buttonKEYBOARD);
        NewSession.setText("Открыть сессию");
        NewSession.setOnClickListener(this::OpenSession);
        NewSession.setBackgroundResource(R.drawable.ic_style_button_background);
        return kv;
    }

    public void OpenSession(View e){
        try{
            if(PreferenceManager.getDefaultSharedPreferences(this).getString("name", "").isEmpty()){
                startActivity(new Intent(this, Starting.class));
                return;
            }
            NewSession.setOnClickListener(this::CloseSession);
            NewSession.setText("Закрыть сессию");
            Utils.SessionServer s = new Utils.SessionServer(Session.KEYBOARD,0,()->{
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(()-> {
                    NewSession.setOnClickListener(this::OpenSession);
                    NewSession.setText("Открыть сессию");
                });
            }, this);
            s.Start();
        } catch (IOException | JSONException err){
            err.printStackTrace();
        }
    }

    public void CloseSession(View e) {Handler mainHandler = new Handler(Looper.getMainLooper());
        if(PreferenceManager.getDefaultSharedPreferences(this).getString("name", "").isEmpty()){
            startActivity(new Intent(this, Starting.class));
            return;
        }
        mainHandler.post(()-> {
            NewSession.setOnClickListener(this::OpenSession);
            NewSession.setText("Открыть сессию");
        });
    }

    public void OpenList(View v){
        if(scrollView.getVisibility() == View.GONE){
            scrollView.setVisibility(View.VISIBLE);
            ((Button) v).setText("▼");
        }else {
            scrollView.setVisibility(View.GONE);
            ((Button) v).setText("▲");
        }
    }
}
