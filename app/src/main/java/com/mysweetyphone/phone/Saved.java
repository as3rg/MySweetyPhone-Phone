package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.util.Date;

import static android.app.Activity.RESULT_OK;

public class Saved extends Fragment {

    boolean isMsgWillBeHereDeleted = false;
    private long timeMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        final LinearLayout mainLayout = getActivity().findViewById(R.id.MainLayoutSAVED);
        final TextView msg =  getActivity().findViewById(R.id.TextFieldSAVED);
        final ImageButton sendButton = getActivity().findViewById(R.id.SendButtonSAVED);
        final TextView msgWillBeHere = getActivity().findViewById(R.id.MessagesWillBeHereSAVED);
        final ListView listMessages = getActivity().findViewById(R.id.ListMessagesSAVED);

        final ImageButton chooseFileButton = getActivity().findViewById(R.id.ChooseFileSAVED);
        chooseFileButton.setOnClickListener(new View.OnClickListener() {    //выбор файла
            @Override
            public void onClick(View v) {
                new MaterialFilePicker()
                        .withActivity(getActivity())
                        .withRequestCode(1000)
                        .withHiddenFiles(false) // Показывает скрытые файлы и папки
                        .start();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                if(!isMsgWillBeHereDeleted){
                    mainLayout.removeView(msgWillBeHere);
                    isMsgWillBeHereDeleted = true;
                }

                final String[] messages = new String[]{"s", "dfe", "s", "dfe","s", "dfe","s", "dfe","s", "dfe","s", "dfe","s", "dfe","s", "dfe",};
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        R.layout.list_messages_saved, messages);

                listMessages.setAdapter(adapter);

                /*TextView sendText = new TextView(getActivity());
                sendText.setTextSize(21);
                sendText.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.saved_box));
                sendText.setPadding(40, 40, 40, 40);

                if(msg.getText().length() != 0) {
                    sendText.setTextColor(Color.BLACK);
                    sendText.setText(msg.getText().toString());
                    msg.setText("");
                    mainLayout.addView(sendText);
                }
                else{
                    sendText.setTextColor(Color.parseColor("#494949"));
                    sendText.setTypeface(null, Typeface.ITALIC);
                    sendText.setText("Пустое сообщение");
                    msg.setText("");
                    mainLayout.addView(sendText);
                }*/
            }
        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);

            Toast.makeText(getActivity(), filePath, Toast.LENGTH_SHORT).show();

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
