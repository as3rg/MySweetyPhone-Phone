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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class Saved extends Fragment{

    private int regdate;
    private int id;
    private String name;
    private String login;
    private LinearLayout MessagesList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        id = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getInt("id",-1);
        regdate = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getInt("regdate",-1);
        login  = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("login","");
        name = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("name","");
    }

    void LoadMore(){
        LoadMore(10);
    }

    void LoadMore(int Count){
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=GetMessages&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&From="+MessagesList.getChildCount()+"&Count="+Count, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                try {
                    int i = result.getInt("code");

                    if(i==2){
                        throw new Exception("Ошибка приложения!");
                    }else if(i==1){
                        throw new Exception("Неверные данные");
                    }else if(i==0){
                        JSONArray messages = (JSONArray)result.get("messages");
                        for(int j = Objects.requireNonNull(messages).length() - 1; j >= 0; j--){
                            JSONObject message = (JSONObject)messages.get(j);
                            DrawMessage((message.getString("msg")).replace("\\n","\n"),message.getLong("date"),message.getString("sender"), (message.getString("type")).equals("File"));
                        }
                    }else if(i==4){
                        throw new Exception("Ваше устройство не зарегистрировано");
                    }else{
                        throw new Exception("Ошибка приложения!");
                    }
                }catch (Exception e){
                    Toast toast = Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG);
                    toast.show();
                    e.printStackTrace();
                    Objects.requireNonNull(getActivity()).finish();
                }
            }
        });
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
        MessagesList = getActivity().findViewById(R.id.MessagesSAVED);
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
            AsyncHttpClient client = new AsyncHttpClient();
            client.get("http://mysweetyphone.herokuapp.com/?Type=SendMessage&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&MsgType=Text&Msg="+MessageText.getText().toString().replace(" ","%20").replace("\n","\\n"), new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                    try {
                        int i = result.getInt("code");
                        if(i == 2){
                            throw new Exception("Ошибка приложения!");
                        }else if(i == 1){
                            throw new Exception("Неверные данные");
                        }else if(i == 0){
                            DrawMessage(MessageText.getText().toString(), result.getLong("time"), name, false, true);
                            MessageText.setText("");
                        }else if(i == 4){
                            throw new Exception("Ваше устройство не зарегистрировано!");
                        }else{
                            throw new Exception("Ошибка приложения!");
                        }
                    }catch (Exception e){
                        Toast toast = Toast.makeText(getContext(), "Непредвиденная ошибка", Toast.LENGTH_LONG);
                        toast.show();
                        e.printStackTrace();
                        getActivity().finish();
                    }
                }
            });
        });
        LoadMore();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            File file = new File(Objects.requireNonNull(data.getData()).getPath());
            if(file.length() > 1024*1024){
                Toast toast = Toast.makeText(getContext(), "Размер файла превышает допустимые размеры", Toast.LENGTH_LONG);
                toast.show();
                return;
            }
            if(!Charset.forName("US-ASCII").newEncoder().canEncode(file.getName())){
                Toast toast = Toast.makeText(getContext(), "Имя файла содержит недопустимые символы", Toast.LENGTH_LONG);
                toast.show();
                return;
            }

            Runnable r = () -> {
                try {
                    String url = "http://mysweetyphone.herokuapp.com";
                    String urlParameters = "?Type=UploadFile&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&MsgType=Text";
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                    con.setRequestMethod("POST");
                    con.setDoOutput(true);
                    con.setDoInput(true);
                    byte[] Data;

                    con.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
          /*ошибка*/DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.writeBytes(urlParameters);
                    wr.flush();
                    wr.close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    Toast toast = Toast.makeText(getContext(), "пока все хорошо", Toast.LENGTH_SHORT);
                    toast.show();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    /*OutputStream os = con.getOutputStream();
                    Data = urlParameters.getBytes("UTF-8");
                    os.write(Data);
                    con.connect();*/



                    JSONObject result = new JSONObject(response.toString());
                    int i = result.getInt("code");
                    if(i == 2){
                        throw new Exception("Ошибка приложения!");
                    }else if(i == 1){
                        throw new Exception("Неверные данные");
                    }else if(i == 0){
                        DrawMessage(file.getName(), result.getLong("time"), name, true, true);
                    }else if(i == 4){
                        throw new Exception("Ваше устройство не зарегистрировано!");
                    }else{
                        throw new Exception("Ошибка приложения!");
                    }
                }catch (Exception e){
                    Toast toast = Toast.makeText(getContext(), "Непредвиденная ошибка", Toast.LENGTH_LONG);
                    toast.show();
                    e.printStackTrace();
                    getActivity().finish();
                }
            };
            Thread t = new Thread(r);
            t.run();
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

    private void DrawMessage(String text, Long date, String sender, Boolean isFile) {
        DrawMessage(text, date, sender, isFile, false);
    }

    @SuppressLint("SetTextI18n")
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
        if(text.length() == 0) {
            textBox.setText("Пустое сообщение");
            textBox.setTextColor(Color.parseColor("#cccccc"));
            textBox.setTypeface(null, Typeface.ITALIC);
        }else {
            textBox.setText(text);
        }
        textBox.setTextSize(20);
        layout.addView(textBox);
        TextView dateBox = new TextView(getActivity());
        Date Date = new java.util.Date(date * 1000L);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        dateBox.setText(format.format(Date) + ", " +  sender);
        layout.addView(dateBox);
        layout.setPadding(35,35,35,35);
        MessagesList.addView(layout,0);
        if(needsAnim){
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.send_anim);
            layout.startAnimation(anim);
        }
    }
}
