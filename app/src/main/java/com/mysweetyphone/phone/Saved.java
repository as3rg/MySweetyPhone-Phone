package com.mysweetyphone.phone;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import static android.app.Activity.RESULT_OK;

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
        final ImageButton sendButton = getActivity().findViewById(R.id.sendButtonSAVED);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                TextView sendText = new TextView(getActivity());
                sendText.setTextColor(Color.BLACK);
                sendText.setTextSize(21);
                sendText.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.saved_box));
                sendText.setPadding(40,40,40,40);
                sendText.setText(msg.getText().toString());
                msg.setText("");
                mainLayout.addView(sendText);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);

            Toast toast = Toast.makeText(getContext(),
                    filePath, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @SuppressLint("ShowToast")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case  1001: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getActivity(), "Permission granted.", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getActivity(), "Permission NOT granted.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
