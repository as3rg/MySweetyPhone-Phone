package com.mysweetyphone.phone;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class Saved extends Fragment{

    private int regdate;
    private int id;
    private String name;
    private String login;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Runnable r = () -> {
            try {
                regdate = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("regdate", -1);
                id = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("id", -1);
                login = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("login", "");
                name = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("name","");

                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=GetMessages&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id);

                HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject result = (JSONObject) JSONValue.parse(response.toString());
                Long i = (Long) result.getOrDefault("code", 2);
                if(i.equals(2L)){
                    throw new Exception("Ошибка приложения!");
                }else if(i.equals(1L)){
                    throw new Exception("Неверные данные");
                }else if(i.equals(0L)){
                    JSONArray messagesArray = new JSONArray(result.get("messages"));
                    for(int j = 0; j < messagesArray.length(); j++){
                        Object message = new Object();
                        DrawMessage(((String)((JSONObject)message).get("msg")).replace("\\n","\n"),
                                (Long)((JSONObject)message).get("date"),
                                (String)((JSONObject)message).get("sender"),
                                (String)((JSONObject)message).get("type"));
                    }
                }else if(i.equals(4L)){
                    throw new Exception("Ваше устройство не зарегистрировано");
                }else{
                    throw new Exception("Ошибка приложения!");
                }
            } catch (Exception e){
                Toast toast = Toast.makeText(getContext(),
                        "Ошибка", Toast.LENGTH_LONG);
                toast.show();
                e.printStackTrace();
                getActivity().finish();
            }
        };
        Thread t = new Thread(r);
        t.run();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        final TextView MessageText =  getActivity().findViewById(R.id.TextFieldSAVED);
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
            Runnable r = () -> {
                try {
                    URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=SendMessage&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&MsgType=Text&Msg="+MessageText.toString().replace(" ","%20").replace("\n","\\n"));

                    HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject result = new JSONObject(response.toString());
                    Long i = (Long) result.getLong("code");
                    if(i.equals(2L)){
                        throw new Exception("Ошибка приложения!");
                    }else if(i.equals(1L)){
                        throw new Exception("Неверные данные");
                    }else if(i.equals(0L)){
                        DrawMessage(MessageText.getText(), (Long) result.getOrDefault("time", 2), name, "Text", true);
                    }else if(i.equals(4L)){
                       throw new Exception("Ваше устройство не зарегистрировано!");
                    }else{
                        throw new Exception("Ошибка приложения!");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            Thread t = new Thread(r);
            t.run();
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            File file = new File(Objects.requireNonNull(data.getData()).getPath());
            DrawMessage(file.getName(), 0L, name, "File", true);
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

    private void DrawMessage(String text, Long date, String sender, String type) {
        DrawMessage(text, date, sender, type, false);
    }

    @SuppressLint("SetTextI18n")
    private void DrawMessage(String text, Long date, String sender, String type, Boolean needsAnim) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));
        if (type.equals("File")) {
            ImageView image = new ImageView(getActivity());
            image.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_attack_file));
            layout.addView(image);
        }
        TextView textBox = new TextView(getActivity());
        if(text.length() == 0) {
            textBox.setText("Пустое сообщение");
            textBox.setTextColor(Color.parseColor("#cccccc"));
            textBox.setTypeface(null, Typeface.ITALIC);
        }else
            textBox.setText(text);
        textBox.setTextSize(20);
        layout.addView(textBox);
        TextView dateBox = new TextView(getActivity());
        dateBox.setText(DateFormat.format("HH:mm dd.MM.yyyy",  date) + ", " +  sender);
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
