package com.mysweetyphone.phone;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import org.json.JSONException;

import java.io.IOException;

import Utils.Session;


public class SServer extends Fragment {

    Button NewSession;
    Spinner SessionType;
    Switch ServerMode;
    public static final String
            FILEVIEW = "Просмотр Файлов",
            SMSVIEW = "Просмотр SMS";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sserver, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SessionType = getActivity().findViewById(R.id.typeSSERVER);
        SessionType.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_item, new String[]{FILEVIEW, SMSVIEW}));
        NewSession = getActivity().findViewById(R.id.newSessionSSERVER);
        NewSession.setOnClickListener(this::OpenSession);
        ServerMode = getActivity().findViewById(R.id.serverModeSSERVER);

        ServerMode.setChecked(Utils.ServerMode.getState());
        NewSession.setEnabled(!ServerMode.isChecked());
        SessionType.setEnabled(!ServerMode.isChecked());
        ServerMode.setOnClickListener(this::SwitchServerMode);
    }



    public void OpenSession(View e){
        try{
            NewSession.setOnClickListener(this::CloseSession);
            NewSession.setText("Закрыть сессию");
            Utils.SessionServer s = new Utils.SessionServer(GetType(SessionType.getSelectedItem().toString()),0,()->{
                NewSession.setOnClickListener(this::OpenSession);
                NewSession.setText("Открыть сессию");
                SessionType.setEnabled(true);
            }, getActivity());
            s.Start();
            SessionType.setEnabled(false);
        } catch (IOException | JSONException err){
            err.printStackTrace();
        }
    }

    public int GetType(String s){
        switch (s){
            case SMSVIEW:
                return Session.SMSVIEWER;
            case FILEVIEW:
                return Session.FILEVIEW;
            default:
                return Session.NONE;
        }
    }

    public void CloseSession(View e) {
        try {
            NewSession.setOnClickListener(this::OpenSession);
            NewSession.setText("Открыть сессию");
            Session.sessions.pop().Stop();
            SessionType.setEnabled(true);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void SwitchServerMode(View v) {
        try {
            Switch s = (Switch) v;
            if(s.isChecked()){
                if(!Session.sessions.isEmpty()) CloseSession(null);
                Utils.ServerMode.Start();
            }else
                Utils.ServerMode.Stop();
            NewSession.setEnabled(!s.isChecked());
            SessionType.setEnabled(!s.isChecked());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
