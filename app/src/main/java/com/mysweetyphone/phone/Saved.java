package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import com.squareup.picasso.Picasso;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.internal.platform.Platform;

import static android.app.Activity.RESULT_OK;

public class Saved extends Fragment {

    private int regdate;
    private int id;
    private String name;
    private String login;
    private LinearLayout MessagesList;
    private ScrollView scrollView;

    ArrayList<File> tempfiles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        id = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getInt("id", -1);
        regdate = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getInt("regdate", -1);
        login = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("login", "");
        name = (PreferenceManager.getDefaultSharedPreferences(getActivity())).getString("name", "");
        tempfiles = new ArrayList<>();
        if (PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
        }
        Intent intent = getActivity().getIntent();
        if(intent.getAction() == Intent.ACTION_SEND){
            if(intent.getParcelableExtra(Intent.EXTRA_STREAM) != null)
                SendFile(intent.getParcelableExtra(Intent.EXTRA_STREAM));
            else if(intent.getStringExtra(Intent.EXTRA_TEXT) != null)
                SendMessage(intent.getStringExtra(Intent.EXTRA_TEXT));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Button LoadMoreButton= getActivity().findViewById(R.id.LoadMoreSAVED);
        LoadMoreButton.setOnClickListener(v -> {
            LoadMore(10);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
        final TextView MessageText = getActivity().findViewById(R.id.TextFieldSAVED);
        final ImageButton sendButton = getActivity().findViewById(R.id.SendButtonSAVED);
        MessagesList = getActivity().findViewById(R.id.MessagesSAVED);
        final ImageButton chooseFileButton = getActivity().findViewById(R.id.ChooseFileSAVED);
        scrollView = getActivity().findViewById(R.id.ScrollBarSAVED);
        chooseFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, 43);
        });

        sendButton.setOnClickListener(v -> {
            SendMessage(MessageText.getText().toString());
            MessageText.setText("");
        });
        LoadMore();
    }

    void SendMessage(String text){
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=SendMessage&RegDate=" + regdate + "&MyName=" + name + "&Login=" + login + "&Id=" + id + "&MsgType=Text&Msg=" + text.replace(" ", "%20").replace("\n", "\\n"), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                try {
                    int i = result.getInt("code");
                    if (i == 2) {
                        throw new Exception("Ошибка приложения!");
                    } else if (i == 1) {
                        throw new Exception("Неверные данные");
                    } else if (i == 0) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                Draw(text, result.getLong("time"), name, false, true);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    } else if (i == 4) {
                        Toast toast = Toast.makeText(getActivity(),
                                "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        throw new Exception("Ошибка приложения!");
                    }
                } catch (Exception e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    getActivity().finish();
                }
            }
        });
    }

    void LoadMore() {
        LoadMore(10);
    }

    void LoadMore(int Count) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("http://mysweetyphone.herokuapp.com/?Type=GetMessages&RegDate=" + regdate + "&MyName=" + name + "&Login=" + login + "&Id=" + id  + "&Count=" + (MessagesList.getChildCount()-1+Count), new JsonHttpResponseHandler() {
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
                        MessagesList.removeAllViews();
                        for (int j = Objects.requireNonNull(messages).length() - 1; j >= 0; j--) {
                            JSONObject message = (JSONObject) messages.get(j);
                            getActivity().runOnUiThread(() -> {
                                try {
                                    Draw((message.getString("msg")).replace("\\n", "\n"), message.getLong("date"), message.getString("sender"), (message.getString("type")).equals("File"), false);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    } else if (i == 4) {
                        Toast toast = Toast.makeText(getActivity(),
                                "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        throw new Exception("Ошибка приложения!");
                    }
                } catch (Exception e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
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

    @SuppressLint("SetTextI18n")
    private void DrawText(String text, Long date, String sender, Boolean needsAnim) {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
            //кнопка для закрытия диалога
            //builder.setNeutralButton("Отмена",
            //        (dialog, id) -> dialog.cancel());
            builder.setItems(actions, (dialog, item) -> {
                switch (actions[item]){
                    case "Удалить сообщение":
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&Date="+date+"&Msg="+text.replace(" ","%20").replace("\n","\\n"), new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                                try {
                                    int i = result.getInt("code");

                                    if (i == 2) {
                                        throw new Exception("Ошибка приложения!");
                                    } else if (i == 1) {
                                        throw new Exception("Неверные данные");
                                    } else if (i == 0) {
                                        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.delete_anim);
                                        animation.setAnimationListener(new Animation.AnimationListener(){
                                            @Override
                                            public void onAnimationStart(Animation animation) { }
                                            @Override
                                            public void onAnimationRepeat(Animation animation) { }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {
                                                Handler h = new Handler();
                                                h.postAtTime(()->MessagesList.removeView(layout), 100);
                                            }
                                        });
                                        layout.startAnimation(animation);
                                    } else if (i == 4) {
                                        Toast toast = Toast.makeText(getActivity(),
                                "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                        toast.show();
                                    } else {
                                        throw new Exception("Ошибка приложения!");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        break;
                    case "Копировать текст":
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("", text);
                        clipboard.setPrimaryClip(clip);
                        break;
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return false;
        });

        MessagesList.addView(layout, MessagesList.getChildCount());
        if (needsAnim) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.send_anim);
            layout.startAnimation(anim);
        }
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    @SuppressLint("SetTextI18n")
    private void DrawFile(String text, Long date, String sender, Boolean needsAnim){
        if(getExtension(text.toLowerCase()).equals("jpg") || getExtension(text.toLowerCase()).equals("png") || getExtension(text.toLowerCase()).equals("jpeg") || getExtension(text.toLowerCase()).equals("bmp") || getExtension(text.toLowerCase()).equals("gif")){
            DrawImage(text, date, sender, needsAnim);
            return;
        }

        if(getExtension(text.toLowerCase()).equals("mp4") || getExtension(text.toLowerCase()).equals("flv")){
            DrawVideo(text, date, sender, needsAnim);
            return;
        }

        if(getExtension(text.toLowerCase()).equals("mp3") || getExtension(text.toLowerCase()).equals("wav")){
            DrawAudio(text, date, sender, needsAnim);
            return;
        }

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.isClickable();
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));
        layout.setGravity(Gravity.CENTER);

        ImageView fileImg = new ImageView(getActivity());
        fileImg.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_attack_file));
        layout.addView(fileImg);

        TextView textBox = new TextView(getActivity());
        textBox.setText(text);
        textBox.setTextSize(20);
        layout.addView(textBox);
        TextView dateBox = new TextView(getActivity());
        Date Date = new java.util.Date(date * 1000L);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        dateBox.setText(format.format(Date) + ", " + sender);
        layout.addView(dateBox);
        layout.setPadding(35, 35, 35, 35);

        layout.setOnLongClickListener(v -> {
            final String[] actions ={"Удалить сообщение", "Скачать файл", "Копировать текст"};
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            //кнопка для закрытия диалога
            //builder.setNeutralButton("Отмена",
            //        (dialog, id) -> dialog.cancel());
            builder.setItems(actions, (dialog, item) -> {
                switch (actions[item]){
                    case "Удалить сообщение":
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&Date="+date+"&Msg="+text.replace(" ","%20").replace("\n","\\n"), new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                                try {
                                    int i = result.getInt("code");

                                    if (i == 2) {
                                        throw new Exception("Ошибка приложения!");
                                    } else if (i == 1) {
                                        throw new Exception("Неверные данные");
                                    } else if (i == 0) {
                                        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.delete_anim);
                                        animation.setAnimationListener(new Animation.AnimationListener(){
                                            @Override
                                            public void onAnimationStart(Animation animation) { }
                                            @Override
                                            public void onAnimationRepeat(Animation animation) { }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {
                                                Handler h = new Handler();
                                                h.postAtTime(()->MessagesList.removeView(layout), 100);
                                            }
                                        });
                                        layout.startAnimation(animation);
                                    } else if (i == 4) {
                                        Toast toast = Toast.makeText(getActivity(),
                                "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                        toast.show();
                                    } else {
                                        throw new Exception("Ошибка приложения!");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }); break;
                    case "Копировать текст":
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("", text);
                        clipboard.setPrimaryClip(clip);
                        break;
                    case "Скачать файл":
                        Download(text,date);
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return false;
        });
        MessagesList.addView(layout);
        if (needsAnim) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.send_anim);
            layout.startAnimation(anim);
        }
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void DrawImage(String text, Long date, String sender, Boolean needsAnim){
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.isClickable();
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));
        layout.setGravity(Gravity.CENTER);

        ImageView Image = new ImageView(getActivity());
        layout.addView(Image);

        (new Thread(() -> {
            try {
                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + name + "&Login=" + login + "&Id=" + id + "&FileName=" + text.replace(" ","%20") + "&Date=" + date);
                HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject result = (JSONObject) new JSONObject(response.toString());
                String filebody = (String)result.get("filebody");

                getActivity().runOnUiThread(() -> {
                    try {
                        Image.setImageBitmap(BitmapFactory.decodeStream(new ByteArrayInputStream(Hex.decodeHex(filebody.substring(2).toCharArray()))));
                    } catch (DecoderException e) {
                        e.printStackTrace();
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        })).start();


        TextView textBox = new TextView(getActivity());
        textBox.setText(text);
        textBox.setTextSize(20);
        layout.addView(textBox);
        TextView dateBox = new TextView(getActivity());
        Date Date = new java.util.Date(date * 1000L);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        dateBox.setText(format.format(Date) + ", " + sender);
        layout.addView(dateBox);
        layout.setPadding(35, 35, 35, 35);

        layout.setOnLongClickListener(v -> {
            final String[] actions ={"Удалить сообщение", "Скачать файл", "Копировать текст"};
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(actions, (dialog, item) -> {
                switch (actions[item]){
                    case "Удалить сообщение":
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&Date="+date+"&Msg="+text.replace(" ","%20").replace("\n","\\n"), new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                                try {
                                    int i = result.getInt("code");

                                    if (i == 2) {
                                        throw new Exception("Ошибка приложения!");
                                    } else if (i == 1) {
                                        throw new Exception("Неверные данные");
                                    } else if (i == 0) {
                                        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.delete_anim);
                                        animation.setAnimationListener(new Animation.AnimationListener(){
                                            @Override
                                            public void onAnimationStart(Animation animation) { }
                                            @Override
                                            public void onAnimationRepeat(Animation animation) { }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {
                                                Handler h = new Handler();
                                                h.postAtTime(()->MessagesList.removeView(layout), 100);
                                            }
                                        });
                                        layout.startAnimation(animation);
                                    } else if (i == 4) {
                                        Toast toast = Toast.makeText(getActivity(),
                                "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                        toast.show();
                                    } else {
                                        throw new Exception("Ошибка приложения!");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }); break;
                    case "Копировать текст":
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("", text);
                        clipboard.setPrimaryClip(clip);
                        break;
                    case "Скачать файл":
                        Download(text,date);
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return false;
        });
        MessagesList.addView(layout);
        if (needsAnim) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.send_anim);
            layout.startAnimation(anim);
        }
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void DrawVideo(String text, Long date, String sender, Boolean needsAnim){
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.isClickable();
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));

        new Thread(() -> {
            try {
                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + name + "&Login=" + login + "&Id=" + id + "&FileName=" + text.replace(" ","%20") + "&Date=" + date);
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
                String filebody = (String)result.get("filebody");

                File out = File.createTempFile(text, ".mp4");
                tempfiles.add(out);
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(Hex.decodeHex(filebody.substring(2).toCharArray()));
                fos.close();
                getActivity().runOnUiThread(() -> {
                    VideoView videoView = new VideoView(getActivity());
                    MediaController mediaController = new MediaController(getActivity());
                    mediaController.setAnchorView(videoView);
                    mediaController.setMediaPlayer(videoView);
                    videoView.setMediaController(mediaController);
                    videoView.setVisibility(View.VISIBLE);
                    videoView.setMinimumHeight(100);
                    videoView.setMinimumWidth(100);
                    videoView.requestFocus(0);
                    videoView.setVideoURI(Uri.fromFile(out));
                    videoView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                            ActionBar.LayoutParams.FILL_PARENT));
                    videoView.start();
                    videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            System.err.println(what+" "+extra);
                            return false;
                        }
                    });
                    layout.addView(videoView);

                });

            } catch (IOException | DecoderException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();

        TextView textBox = new TextView(getActivity());
        textBox.setText(text);
        textBox.setTextSize(20);
        layout.addView(textBox);
        TextView dateBox = new TextView(getActivity());
        Date Date = new java.util.Date(date * 1000L);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        dateBox.setText(format.format(Date) + ", " + sender);
        layout.addView(dateBox);
        layout.setPadding(35, 35, 35, 35);

        layout.setOnLongClickListener(v -> {
            final String[] actions ={"Удалить сообщение", "Скачать файл", "Копировать текст"};
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(actions, (dialog, item) -> {
                switch (actions[item]){
                    case "Удалить сообщение":
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&Date="+date+"&Msg="+text.replace(" ","%20").replace("\n","\\n"), new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                                try {
                                    int i = result.getInt("code");

                                    if (i == 2) {
                                        throw new Exception("Ошибка приложения!");
                                    } else if (i == 1) {
                                        throw new Exception("Неверные данные");
                                    } else if (i == 0) {
                                        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.delete_anim);
                                        animation.setAnimationListener(new Animation.AnimationListener(){
                                            @Override
                                            public void onAnimationStart(Animation animation) { }
                                            @Override
                                            public void onAnimationRepeat(Animation animation) { }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {
                                                Handler h = new Handler();
                                                h.postAtTime(()->MessagesList.removeView(layout), 100);
                                            }
                                        });
                                        layout.startAnimation(animation);
                                    } else if (i == 4) {
                                        Toast toast = Toast.makeText(getActivity(),
                                "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                        toast.show();
                                    } else {
                                        throw new Exception("Ошибка приложения!");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }); break;
                    case "Копировать текст":
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("", text);
                        clipboard.setPrimaryClip(clip);
                        break;
                    case "Скачать файл":
                        Download(text,date);
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return false;
        });
        MessagesList.addView(layout);
        if (needsAnim) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.send_anim);
            layout.startAnimation(anim);
        }
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void DrawAudio(String text, Long date, String sender, Boolean needsAnim){
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.isClickable();
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));
        layout.setGravity(Gravity.CENTER);

        SeekBar sb = new SeekBar(getActivity());
        sb.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        sb.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        MediaPlayer mPlayer = new MediaPlayer();
        Button startButton = new Button(getActivity());
        startButton.setBackgroundResource(R.drawable.ic_saved_play);
        startButton.setHeight(startButton.getWidth());
        Timer timer = new Timer();
        mPlayer.setOnCompletionListener(mp -> {
            try {
                mPlayer.stop();
                mPlayer.prepare();
                mPlayer.seekTo(0);
                startButton.setBackgroundResource(R.drawable.ic_saved_play);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        layout.addView(startButton);
        layout.addView(sb);

        new Thread(() -> {
            try {
                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + name + "&Login=" + login + "&Id=" + id + "&FileName=" + text.replace(" ","%20") + "&Date=" + date);
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
                String filebody = (String)result.get("filebody");

                File out = File.createTempFile(text, ".tmp");
                tempfiles.add(out);
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(Hex.decodeHex(filebody.substring(2).toCharArray()));
                fos.close();
                getActivity().runOnUiThread(() -> {
                    try {
                        mPlayer.setDataSource(out.getPath());
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mPlayer.prepare();
                        sb.setMax(mPlayer.getDuration());
                        mPlayer.setOnPreparedListener((a)->{
                            startButton.setOnClickListener(v -> {
                                if(mPlayer.isPlaying()){
                                    mPlayer.pause();
                                }
                                else{
                                    mPlayer.start();
                                }
                                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                        if(sb.isPressed())
                                            mPlayer.seekTo(i);
                                    }

                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) {

                                    }

                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) {

                                    }
                                });
                                startButton.setBackgroundResource(mPlayer.isPlaying() ? R.drawable.ic_saved_pause : R.drawable.ic_saved_play);
                            });
                            timer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    if(!sb.isPressed())
                                        sb.setProgress(mPlayer.getCurrentPosition());
                                }
                            },0,100);
                            mPlayer.setOnPreparedListener((b)->{});
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            } catch (IOException | DecoderException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();

        TextView textBox = new TextView(getActivity());
        textBox.setText(text);
        textBox.setTextSize(20);
        layout.addView(textBox);
        TextView dateBox = new TextView(getActivity());
        Date Date = new java.util.Date(date * 1000L);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        dateBox.setText(format.format(Date) + ", " + sender);
        layout.addView(dateBox);
        layout.setPadding(35, 35, 35, 35);

        layout.setOnLongClickListener(v -> {
            final String[] actions ={"Удалить сообщение", "Скачать файл", "Копировать текст"};
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(actions, (dialog, item) -> {
                switch (actions[item]){
                    case "Удалить сообщение":
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate="+regdate+"&MyName="+name+"&Login="+login+"&Id="+id+"&Date="+date+"&Msg="+text.replace(" ","%20").replace("\n","\\n"), new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
                                try {
                                    int i = result.getInt("code");

                                    if (i == 2) {
                                        throw new Exception("Ошибка приложения!");
                                    } else if (i == 1) {
                                        throw new Exception("Неверные данные");
                                    } else if (i == 0) {
                                        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.delete_anim);
                                        animation.setAnimationListener(new Animation.AnimationListener(){
                                            @Override
                                            public void onAnimationStart(Animation animation) { }
                                            @Override
                                            public void onAnimationRepeat(Animation animation) { }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {
                                                Handler h = new Handler();
                                                h.postAtTime(()->MessagesList.removeView(layout), 100);
                                            }
                                        });
                                        layout.startAnimation(animation);
                                    } else if (i == 4) {
                                        Toast toast = Toast.makeText(getActivity(),
                                "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                        toast.show();
                                    } else {
                                        throw new Exception("Ошибка приложения!");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }); break;
                    case "Копировать текст":
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("", text);
                        clipboard.setPrimaryClip(clip);
                        break;
                    case "Скачать файл":
                        Download(text,date);
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return false;
        });
        MessagesList.addView(layout);
        if (needsAnim) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.send_anim);
            layout.startAnimation(anim);
        }
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for(File f : tempfiles)
            f.deleteOnExit();
    }

    private void SendFile(Uri uri){
        File file = new File(ImageFilePath.getPath(getActivity(), uri));
        if (file.length() > 1024 * 1024) {
            Toast toast = Toast.makeText(getActivity(), "Размер файла превышает допустимые размеры", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if (!Charset.forName("US-ASCII").newEncoder().canEncode(file.getName())) {
            Toast toast = Toast.makeText(getActivity(), "Имя файла содержит недопустимые символы", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        Runnable r = () -> {
            try {
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost("http://mysweetyphone.herokuapp.com/?Type=UploadFile&RegDate=" + regdate + "&MyName=" + name + "&Login=" + login + "&Id=" + id);
                MultipartEntity entity = new MultipartEntity();
                entity.addPart("fileToUpload", new FileBody(file));
                post.setEntity(entity);
                HttpResponse response = client.execute(post);

                String body = IOUtils.toString(response.getEntity().getContent());
                System.out.println(body);
                JSONObject result = new JSONObject(body);


                int i = result.getInt("code");
                if (i == 2) {
                    throw new RuntimeException("Ошибка приложения!");
                } else if (i == 1) {
                    throw new RuntimeException("Неверные данные");
                } else if (i == 0) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            Draw(file.getName(), result.getLong("time"), name, true, true);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } else if (i == 4) {
                    Toast toast = Toast.makeText(getActivity(),
                            "Ваше устройство не зарегистрировано!", Toast.LENGTH_LONG);
                    toast.show();
                } else if (i == 3) {
                    throw new RuntimeException("Файл не отправлен!");
                } else {
                    throw new RuntimeException("Ошибка приложения!");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    //выбор файла
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println(requestCode+" "+resultCode);
        if (resultCode == RESULT_OK) {
            File file = new File(ImageFilePath.getPath(getActivity(), data.getData()));
            SendFile(data.getData());
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
                    getActivity().finish();
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }

    public interface FileLoadingListener {
        void onBegin();
        void onSuccess();
        void onFailure(Throwable cause);
        void onEnd();
    }

    private void Download(String text, Long date){
        Runnable r = () -> {
            try {
                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + name + "&Login=" + login + "&Id=" + id + "&FileName=" + text.replace(" ","%20") + "&Date=" + date);
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
                String filebody = (String)result.get("filebody");
                File out2 = new File(Environment.getExternalStorageDirectory() + "/MySweetyPhone");
                out2.mkdirs();
                FileOutputStream fos = new FileOutputStream(new File(out2, text));
                fos.write(Hex.decodeHex(filebody.substring(2).toCharArray()));
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DecoderException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }
}