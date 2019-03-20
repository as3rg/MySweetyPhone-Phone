package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;

public class Saved extends Fragment {

    private int regdate;
    private int id;
    private String name;
    private String login;
    private LinearLayout MessagesList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        id = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getInt("id", -1);
        regdate = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getInt("regdate", -1);
        login = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("login", "");
        name = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("name", "");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final TextView MessageText = getActivity().findViewById(R.id.TextFieldSAVED);
        final ImageButton sendButton = getActivity().findViewById(R.id.SendButtonSAVED);
        MessagesList = getActivity().findViewById(R.id.MessagesSAVED);
        final ImageButton chooseFileButton = getActivity().findViewById(R.id.ChooseFileSAVED);
        chooseFileButton.setOnClickListener(v -> {
            if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
            } else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, 43);
            }
        });

        sendButton.setOnClickListener(v -> {
            AsyncHttpClient client = new AsyncHttpClient();
            client.get("http://mysweetyphone.herokuapp.com/?Type=SendMessage&RegDate=" + regdate + "&MyName=" + name + "&Login=" + login + "&Id=" + id + "&MsgType=Text&Msg=" + MessageText.getText().toString().replace(" ", "%20").replace("\n", "\\n"), new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                    try {
                        int i = result.getInt("code");
                        if (i == 2) {
                            throw new Exception("Ошибка приложения!");
                        } else if (i == 1) {
                            throw new Exception("Неверные данные");
                        } else if (i == 0) {
                            Draw(MessageText.getText().toString(), result.getLong("time"), name, false, true);
                            MessageText.setText("");
                        } else if (i == 4) {
                            throw new Exception("Ваше устройство не зарегистрировано!");
                        } else {
                            throw new Exception("Ошибка приложения!");
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                        getActivity().finish();
                    }
                }
            });
        });
        LoadMore();
    }

    void LoadMore() {
        LoadMore(10);
    }

    void LoadMore(int Count) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=GetMessages&RegDate=" + regdate + "&MyName=" + name + "&Login=" + login + "&Id=" + id + "&From=" + MessagesList.getChildCount() + "&Count=" + Count, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                try {
                    int i = result.getInt("code");

                    if (i == 2) {
                        throw new Exception("Ошибка приложения!");
                    } else if (i == 1) {
                        throw new Exception("Неверные данные");
                    } else if (i == 0) {
                        JSONArray messages = (JSONArray) result.get("messages");
                        for (int j = Objects.requireNonNull(messages).length() - 1; j >= 0; j--) {
                            JSONObject message = (JSONObject) messages.get(j);
                            Draw((message.getString("msg")).replace("\\n", "\n"), message.getLong("date"), message.getString("sender"), (message.getString("type")).equals("File"), true);
                        }
                    } else if (i == 4) {
                        throw new Exception("Ваше устройство не зарегистрировано");
                    } else {
                        throw new Exception("Ошибка приложения!");
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    Objects.requireNonNull(getActivity()).finish();
                }
            }
        });
    }

    private void Draw (String text, Long date, String sender, boolean isFile, Boolean needsAnim) {
        if(isFile)
            DrawFile(text, date, sender, needsAnim);
        else
            DrawText(text, date, sender, needsAnim);
    }

    //отображение текста
    @SuppressLint("SetTextI18n")
    private void DrawText(String text, Long date, String sender, Boolean needsAnim) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.isClickable();
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));
        TextView textBox = new TextView(getActivity());
        if (text.length() == 0) {
            textBox.setText("Пустое сообщение");
            textBox.setTextColor(Color.parseColor("#cccccc"));
            textBox.setTypeface(null, Typeface.ITALIC);
        } else {
            textBox.setText(text);
        }
        textBox.setTextSize(20);
        layout.addView(textBox);
        TextView dateBox = new TextView(getActivity());
        Date Date = new java.util.Date(date * 1000L);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        dateBox.setText(format.format(Date) + ", " + sender);
        layout.addView(dateBox);
        layout.setPadding(35, 35, 35, 35);

        //удаление сообщения
        layout.setOnLongClickListener(v -> {
            final String[] actions ={"Удалить сообщение", "Копировать текст"};
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Message");
            //кнопка для закрытия диалога
            builder.setNeutralButton("Отмена",
                    (dialog, id) -> dialog.cancel());
            builder.setItems(actions, (dialog, item) -> {
                switch (actions[item]){
                    case "Удалить сообщение":
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get("https://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&Date="+date+"&Msg="+text.replace(" ","%20").replace("\n","\\n"), new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                                try {
                                    int i = result.getInt("code");

                                    if (i == 2) {
                                        throw new Exception("Ошибка приложения!");
                                    } else if (i == 1) {
                                        throw new Exception("Неверные данные");
                                    } else if (i == 0) {
                                        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.delete_anim);
                                        animation.setAnimationListener(new Animation.AnimationListener(){
                                            @Override
                                            public void onAnimationStart(Animation animation) { }
                                            @Override
                                            public void onAnimationRepeat(Animation animation) { }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {
                                                MessagesList.removeView(layout);
                                            }
                                        });
                                        layout.startAnimation(animation);
//                                        if(MessagesList.getChildCount() < 10)
//                                            LoadMore(10 - MessagesList.getChildCount());
                                        LoadMore();
                                    } else if (i == 4) {
                                        throw new Exception("Ваше устройство не зарегистрировано");
                                    } else {
                                        throw new Exception("Ошибка приложения!");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }); break;
                    case "Копировать текст":
                        Toast.makeText(getContext(),"Не работает", Toast.LENGTH_SHORT).show();
                        break;
                }
            });
            AlertDialog alert = builder.create();
            alert.setTitle("Message");
            alert.show();
            return false;
        });

        MessagesList.addView(layout, 0);
        if (needsAnim) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.send_anim);
            layout.startAnimation(anim);
        }
    }

    private void DrawFile(String text, Long date, String sender, Boolean needsAnim){
        if(getExtension(text.toLowerCase()).equals("jpg") || getExtension(text.toLowerCase()).equals("png") || getExtension(text.toLowerCase()).equals("jpeg") || getExtension(text.toLowerCase()).equals("bmp") || getExtension(text.toLowerCase()).equals("gif")){
            DrawImage(text, date, sender, needsAnim);
            return;
        }

        if(getExtension(text.toLowerCase()).equals("mp4")){
            DrawVideo(text, date, sender, needsAnim);
            return;
        }

        try{
            /*URL website = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + name + "&Login=" + login + "&Id=" + id + "&FileName=" + text + "&Date=" + date);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            if(getExtension(text).equals("jpg") || getExtension(text).equals("png")) {

            }

                URL website = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + name + "&Login=" + login + "&Id=" + id + "&FileName=" + text + "&Date=" + date);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                File out = new File("//storage//emulated//0//MySweetyPhone");
                out.mkdirs();

                ImageView image = new ImageView(getActivity());
                image.setImageURI(selectedImage);
                layout.addView(image);

                ImageView image1 = new ImageView(getActivity());
                image1.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_attack_file));
                layout.addView(image); */
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void DrawImage(String text, Long date, String sender, Boolean needsAnim){

    }

    private void DrawVideo(String text, Long date, String sender, Boolean needsAnim){

    }

    //выбор файла
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            File file = new File(ImageFilePath.getPath(getActivity(), data.getData()));
            System.out.println(ImageFilePath.getPath(getActivity(), data.getData()));
            //InputStream fin = getActivity().getContentResolver().openInputStream(data.getData());
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
                    HttpPost post = new HttpPost("http://mysweetyphone.herokuapp.com/?Type=UploadFile&RegDate=" + regdate + "&MyName=" + name + "&Login=" + login + "&Id=" + id);
                    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                    entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    entityBuilder.addTextBody("submit", "");
                    entityBuilder.addBinaryBody("fileToUpload", file);
                    HttpEntity entity = entityBuilder.build();
                    post.setEntity(entity);
                    HttpResponse response = client.execute(post);

                    JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity()));
                    int i = result.getInt("code");
                    if (i == 2) {
                        throw new Exception("Ошибка приложения!");
                    } else if (i == 1) {
                        throw new Exception("Неверные данные");
                    } else if (i == 0) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                try {
                                    Draw(file.getName(), result.getLong("time"), name, true, true);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } else if (i == 4) {
                        throw new Exception("Ваше устройство не зарегистрировано!");
                    } else if (i == 3) {
                        throw new Exception("Файл не отправлен!");
                    } else {
                        throw new Exception("Ошибка приложения!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    getActivity().finish();
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
    }

    //определение расширения файла
    private static String getExtension(String text) {
        // если в имени файла есть точка и она не является первым символом в названии файла
        if(text.lastIndexOf(".") != -1 && text.lastIndexOf(".") != 0)
            // то вырезаем все знаки после последней точки в названии файла, то есть ХХХХХ.txt -> txt
            return text.substring(text.lastIndexOf(".")+1);
            // в противном случае возвращаем заглушку, то есть расширение не найдено
        else return "";
    }

    @SuppressLint("ShowToast")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1001: {
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getActivity(), "Разрешения предоставлены", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Разрешения не предоставлены", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }
}