package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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
            if(PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
            }else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, 43);
            }
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
                        Toast toast = Toast.makeText(getContext(),
                                e.getMessage(), Toast.LENGTH_LONG);
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
                File file = new File(ImageFilePath.getPath(getActivity(),data.getData()));
                System.out.println(ImageFilePath.getPath(getActivity(),data.getData()));
                //InputStream fin = getActivity().getContentResolver().openInputStream(data.getData());
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
                        HttpClient client = new DefaultHttpClient();
                        HttpPost post = new HttpPost("http://mysweetyphone.herokuapp.com/?Type=UploadFile&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id);
                        MultipartEntity entity = new MultipartEntity();
                        entity.addPart("fileToUpload", new FileBody(file));
                        entity.addPart("submit", new StringBody(""));
                        post.setEntity(entity);
                        HttpResponse response = client.execute(post);

                        JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity()));
                        int i = result.getInt("code");
                        if(i == 2){
                            throw new Exception("Ошибка приложения!");
                        }else if(i == 1){
                            throw new Exception("Неверные данные");
                        }else if(i == 0){
                            getActivity().runOnUiThread(()->{
                                try {
                                    DrawMessage(file.getName(), result.getLong("time"), name, true, true);
                                }catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                        }else if(i == 4) {
                            throw new Exception("Ваше устройство не зарегистрировано!");
                        }else if(i == 3){
                            throw new Exception("Файл не отправлен!");
                        }else{
                            throw new Exception("Ошибка приложения!");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        getActivity().finish();
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
    }

    @SuppressLint("ShowToast")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case  1001: {
                if(grantResults.length!=0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getActivity(), "Разрешения предоставлены", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getActivity(), "Разрешения не предоставлены", Toast.LENGTH_SHORT).show();
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
