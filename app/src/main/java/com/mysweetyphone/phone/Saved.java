package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.text.SimpleDateFormat;

import static android.app.Activity.RESULT_OK;

public class Saved extends Fragment{

    int regdate;
    int id;
    String name;
    String login;

    boolean isMsgWillBeHereDeleted = false;
    private long timeMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        regdate = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("regdate", -1);
        id = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("id", -1);
        login = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("login", "");
        name = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("name","");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        final TextView msg =  getActivity().findViewById(R.id.TextFieldSAVED);
        final ImageButton sendButton = getActivity().findViewById(R.id.SendButtonSAVED);

        final ImageButton chooseFileButton = getActivity().findViewById(R.id.ChooseFileSAVED);
        chooseFileButton.setOnClickListener(v->{
            /*if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
            }*/
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent,43);
        });

        sendButton.setOnClickListener(v -> {
            DrawMessage(msg.getText().toString(),(Long)(System.currentTimeMillis()/1000),name, false,true);
            msg.setText("");
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            File file = new File(data.getData().getPath());
            DrawMessage(file.getName(), 0L, name, true, true);
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

    private void DrawMessage(String text, Long date, String sender, Boolean isFile, Boolean needsAnim) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));
        if (isFile) {
            ImageView image = new ImageView(getActivity());
            image.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_attack_file));
            layout.addView(image);
        }
        TextView textBox = new TextView(getActivity());
        textBox.setText(text);
        textBox.setTextSize(20);
        layout.addView(textBox);
        TextView dateBox = new TextView(getActivity());
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        dateBox.setText(sdf.format(date) + ", " + sender);
        layout.addView(dateBox);
        layout.setPadding(35,35,35,35);
        LinearLayout messages = getActivity().findViewById(R.id.MessagesSAVED);
        messages.addView(layout,0);
        if(needsAnim){
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.send_anim);
            layout.startAnimation(anim);
        }
    }
}
