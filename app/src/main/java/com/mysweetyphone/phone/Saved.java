package com.mysweetyphone.phone;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Saved extends Fragment {

    //Button sendButton;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        final LinearLayout mainLayout = getActivity().findViewById(R.id.mainLayoutSAVED);
        final TextView msg =  getActivity().findViewById(R.id.textFieldSAVED);
        final Button sendButton = getActivity().findViewById(R.id.sendButtonSAVED);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                TextView sendText = new TextView(getActivity());
                sendText.setTextColor(Color.BLACK);
                sendText.setTextSize(21);
                sendText.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.saved_box));
                sendText.setPadding(40,40,40,40);
                sendText.setText(msg.getText());
                msg.setText("");
                mainLayout.addView(sendText);
            }
        });
    }
}
