package com.mysweetyphone.phone;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.json.JSONException;

import java.io.IOException;

import Utils.Session;


public class SServer extends Fragment {

    Button NewSession;
    Spinner SessionType;

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
        SessionType.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_item, new String[]{"Просмотр Файлов"}));
        NewSession = getActivity().findViewById(R.id.newSessionSSERVER);
        NewSession.setOnClickListener(this::OpenSession);
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

    public Session.Type GetType(String s){
        if(s.equals("Просмотр Файлов"))
            return Session.Type.FILEVIEW;
        return Session.Type.MOUSE;
    }

    public void CloseSession(View e) {
        try {
            NewSession.setOnClickListener(this::OpenSession);
            NewSession.setText("Открыть сессию");
            Session.sessions.get(Session.sessions.size() - 1).Stop();
            Session.sessions.remove(Session.sessions.size() - 1);
            SessionType.setEnabled(true);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
