package com.mysweetyphone.phone;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;

import Utils.Session;

public class IME extends InputMethodService{

    Button NewSession;

    @Override
    public View onCreateInputView() {
        View kv = getLayoutInflater().inflate(R.layout.keyboard_keyboard, null);
        NewSession = kv.findViewById(R.id.buttonKEYBOARD);
        NewSession.setText("Открыть сессию");
        NewSession.setOnClickListener(this::OpenSession);
        return kv;
    }

//    public void onKey(View v) {
//        InputConnection ic = getCurrentInputConnection();
//        ic.sendKeyEvent()
//        ic.commitText(String.valueOf(96),1);
//    }

    public void OpenSession(View e){
        try{
            NewSession.setOnClickListener(this::CloseSession);
            NewSession.setText("Закрыть сессию");
            Utils.SessionServer s = new Utils.SessionServer(Session.MOUSE,0,()->{
                NewSession.setOnClickListener(this::OpenSession);
                NewSession.setText("Открыть сессию");
            }, this);
            s.Start();
        } catch (IOException | JSONException err){
            err.printStackTrace();
        }
    }

    public void CloseSession(View e) {
        NewSession.setOnClickListener(this::OpenSession);
        NewSession.setText("Открыть сессию");
    }
}
