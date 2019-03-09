package com.mysweetyphone.phone;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
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
            if (file.length() > 1024 * 1024) {
                Toast toast = Toast.makeText(getContext(), "Размер файла превышает допустимые размеры", Toast.LENGTH_LONG);
                toast.show();
                return;
            }
            if (!Charset.forName("US-ASCII").newEncoder().canEncode(file.getName())) {
                Toast toast = Toast.makeText(getContext(), "Имя файла содержит недопустимые символы", Toast.LENGTH_LONG);
                toast.show();
                return;
            }

            Runnable r = () -> {
                try {
                    HttpClient client = new DefaultHttpClient();
                    HttpPost post = new HttpPost("http://mysweetyphone.herokuapp.com/?Type=UploadFile&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&MsgType=Text");

                    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                    entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    entityBuilder.addTextBody("fileToUpload", String.valueOf(new FileBody(file)));
                    entityBuilder.addTextBody("submit", "");

                    entityBuilder.addBinaryBody("fileToUpload",file);

                    HttpEntity entity = entityBuilder.build();
                    post.setEntity(entity);
                    HttpResponse response = client.execute(post);
                    HttpEntity httpEntity = response.getEntity();
                    String resultStr = EntityUtils.toString(httpEntity);
                    Log.v("result", resultStr);

                    JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
                    int i = result.getInt("code");
                    if (i == 2) {
                        throw new Exception("Ошибка приложения!");
                    } else if (i == 1) {
                        throw new Exception("Неверные данные");
                    } else if (i == 0) {
                        DrawMessage(file.getName(), (Long) result.getLong("time"), name,true,true);
                    } else if (i == 4) {
                        throw new Exception("Ваше устройство не зарегистрировано!");
                    } else {
                        throw new Exception("Ошибка приложения!");
                    }
                } catch (Exception e) {
                    Toast toast = Toast.makeText(getContext(), "Непредвиденная ошибка", Toast.LENGTH_LONG);
                    toast.show();
                    e.printStackTrace();
                    Objects.requireNonNull(getActivity()).finish();
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


    /*class SendFile extends AsyncTask<Void, Void, Void>{

        String resultString = null;

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                String myURL = "http://mysweetyphone.herokuapp.com/?Type=";
                String parammetrs = "UploadFile&RegDate=" + regdate + "&MyName=" + name + "&Login=" + login + "&Id=" + id + "&MsgType=Tex";
                byte[] Data = null;
                InputStream is = null;

                try {
                    URL url = new URL(myURL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Content-Length", "" + Integer.toString(parammetrs.getBytes().length));
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    // конвертируем передаваемую строку в UTF-8
                    Data = parammetrs.getBytes("UTF-8");

                    OutputStream os = conn.getOutputStream();
                    Toast toast = Toast.makeText(getContext(), "РАБОТАЕТ", Toast.LENGTH_LONG);
                    toast.show();

                    // передаем данные на сервер
                    os.write(Data);
                    os.flush();
                    os.close();
                    Data = null;
                    conn.connect();
                    int responseCode = conn.getResponseCode();

                    // передаем ответ сервера
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    if (responseCode == 2) {
                        conn.disconnect();
                        throw new Exception("Ошибка приложения!");
                    } else if (responseCode == 1) {
                        conn.disconnect();
                        throw new Exception("Неверные данные");
                    } else if (responseCode == 0) {
                        is = conn.getInputStream();

                        byte[] buffer = new byte[8192]; // размер буфера

                        // Далее так читаем ответ
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }

                        Data = baos.toByteArray();
                        resultString = new String(Data, "UTF-8");  // сохраняем в переменную ответ сервера, у нас "OK"
                    } else if (responseCode == 4) {
                        conn.disconnect();
                        throw new Exception("Ваше устройство не зарегистрировано!");
                    } else {
                        conn.disconnect();
                        throw new Exception("Ошибка приложения!");
                    }
                } catch (MalformedURLException e) {
                    resultString = "MalformedURLException:" + e.getMessage();
                } catch (IOException e) {
                    resultString = "IOException:" + e.getMessage();
                } catch (Exception e) {
                    resultString = "Exception:" + e.getMessage();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            Toast toast = Toast.makeText(getContext(), "Данные переданы!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }*/
}

