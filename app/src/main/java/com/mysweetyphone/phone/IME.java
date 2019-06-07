package com.mysweetyphone.phone;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;

import java.io.IOException;

import Utils.Session;

public class IME extends InputMethodService{

    Button NewSession;
    public View layout;

    @Override
    public View onCreateInputView() {
        View kv = getLayoutInflater().inflate(R.layout.keyboard_keyboard, null);
        layout = kv.findViewById(R.id.keyboardLayoutKEYBOARD);
        NewSession = kv.findViewById(R.id.buttonKEYBOARD);
        NewSession.setText("Открыть сессию");
        NewSession.setOnClickListener(this::OpenSession);
        NewSession.setBackgroundResource(R.drawable.ic_style_button_background);
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
