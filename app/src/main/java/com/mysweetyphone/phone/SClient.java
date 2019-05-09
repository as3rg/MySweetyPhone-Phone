package com.mysweetyphone.phone;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.net.SocketException;

import Utils.SessionClient;


public class SClient extends Fragment {

    Button SearchSessions;
    LinearLayout ConnectToSession;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sclient, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SearchSessions = getActivity().findViewById(R.id.searchSCLIENT);
        SearchSessions.setOnClickListener(this::Search);
        ConnectToSession = getActivity().findViewById(R.id.ConnectToSessionSCLIENT);
    }

    public void Search(View v) {
        try {
            SearchSessions.setOnClickListener(this::StopSearching);
            SearchSessions.setText(R.string.stop_searchingSCLIENT);
            SessionClient.Search(ConnectToSession, new Thread(() -> {
                SearchSessions.setOnClickListener(this::Search);
                SearchSessions.setText("Поиск...");
            }), getActivity());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void StopSearching(View v) {
        SearchSessions.setText(R.string.searchSCLIENT);
        SearchSessions.setOnClickListener(this::Search);
        SessionClient.StopSearching();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SessionClient.StopSearching();
    }
}
