package com.mysweetyphone.phone;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import Utils.ImageFilePath;

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

    void SendMessage(String text) {
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            client.get("http://mysweetyphone.herokuapp.com/?Type=SendMessage&RegDate=" + regdate + "&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&MsgType=Text&Msg=" + URLEncoder.encode(text, "UTF-8"), new JsonHttpResponseHandler() {
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
                        e.printStackTrace();
                        getActivity().finish();
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    void LoadMore() {
        LoadMore(10);
    }

    void LoadMore(int Count) {
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            client.get("http://mysweetyphone.herokuapp.com/?Type=GetMessages&RegDate=" + regdate + "&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&Count=" + (MessagesList.getChildCount() - 1 + Count), new JsonHttpResponseHandler() {
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
                        e.printStackTrace();
                        Objects.requireNonNull(getActivity()).finish();
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
            String[] actions;
            if(isURL(text))
                actions = new String[]{"Удалить сообщение", "Копировать текст", "Открыть ссылку"};
            else
                actions = new String[]{"Удалить сообщение", "Копировать текст"};
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            //кнопка для закрытия диалога
            //builder.setNeutralButton("Отмена",
            //        (dialog, id) -> dialog.cancel());
            builder.setItems(actions, (dialog, item) -> {
                try {
                    switch (actions[item]) {
                        case "Удалить сообщение":
                            AsyncHttpClient client = new AsyncHttpClient();
                            client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate=" + regdate + "&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&Date=" + date + "&Msg=" + URLEncoder.encode(text, "UTF-8"), new JsonHttpResponseHandler() {
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
                                            animation.setAnimationListener(new Animation.AnimationListener() {
                                                @Override
                                                public void onAnimationStart(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationEnd(Animation animation) {
                                                    Handler h = new Handler();
                                                    h.postAtTime(() -> MessagesList.removeView(layout), 100);
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
                        case "Открыть ссылку":
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(new URL(text).toURI().toString()));
                            startActivity(browserIntent);
                            break;
                    }
                } catch (MalformedURLException | URISyntaxException | NullPointerException | UnsupportedEncodingException e) {
                    e.printStackTrace();
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
                try {
                    switch (actions[item]) {
                        case "Удалить сообщение":
                            AsyncHttpClient client = new AsyncHttpClient();
                            client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate=" + regdate + "&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&Date=" + date + "&Msg=" + URLEncoder.encode(text, "UTF-8"), new JsonHttpResponseHandler() {
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
                                            animation.setAnimationListener(new Animation.AnimationListener() {
                                                @Override
                                                public void onAnimationStart(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationEnd(Animation animation) {
                                                    Handler h = new Handler();
                                                    h.postAtTime(() -> MessagesList.removeView(layout), 100);
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
                        case "Скачать файл":
                            Download(text, date);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
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

        ImageView imageView = new ImageView(getActivity());
        layout.addView(imageView);

        (new Thread(() -> {
            try {
                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&FileName=" + URLEncoder.encode(text, "UTF-8") + "&Date=" + date);
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
                String filebody = result.getString("filebody");

                getActivity().runOnUiThread(() -> {
                    try {
                        imageView.setImageBitmap(BitmapFactory.decodeStream(new ByteArrayInputStream(Hex.decodeHex(filebody.substring(2).toCharArray()))));
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
                try {
                    switch (actions[item]) {
                        case "Удалить сообщение":
                            AsyncHttpClient client = new AsyncHttpClient();
                            client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate=" + regdate + "&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&Date=" + date + "&Msg=" + URLEncoder.encode(text, "UTF-8"), new JsonHttpResponseHandler() {
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
                                            animation.setAnimationListener(new Animation.AnimationListener() {
                                                @Override
                                                public void onAnimationStart(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationEnd(Animation animation) {
                                                    Handler h = new Handler();
                                                    h.postAtTime(() -> MessagesList.removeView(layout), 100);
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
                        case "Скачать файл":
                            Download(text, date);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
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
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.isClickable();
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));

        SeekBar sb = new SeekBar(getActivity());
        sb.setClickable(false);
        sb.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        sb.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        sb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sb.setOnTouchListener((v, event) -> true);
        Button startButton = new Button(getActivity());
        startButton.setBackgroundResource(R.drawable.ic_saved_play);
        startButton.setWidth(startButton.getHeight());
        startButton.setHeight(startButton.getWidth());
        startButton.setLayoutParams(new LinearLayout.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT));
        VideoView videoView = new VideoView(getActivity());
        Timer timer = new Timer();
        videoView.setOnCompletionListener(mp -> {
            try {
                videoView.pause();
                videoView.seekTo(0);
                startButton.setBackgroundResource(R.drawable.ic_saved_play);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        LinearLayout bar = new LinearLayout(getActivity());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bar.addView(startButton);
        bar.addView(sb);
        layout.addView(bar);

        new Thread(() -> {
            try {
                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&FileName=" + URLEncoder.encode(text, "UTF-8") + "&Date=" + date);
                HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject result = new JSONObject(response.toString());
                String filebody = result.getString("filebody");

                File out = File.createTempFile(text, ".mp4");
                tempfiles.add(out);
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(Hex.decodeHex(filebody.substring(2).toCharArray()));
                fos.close();
                getActivity().runOnUiThread(() -> {
                    videoView.setVisibility(View.VISIBLE);
                    videoView.setMinimumHeight(100);
                    videoView.setMinimumWidth(100);
                    videoView.requestFocus(0);
                    videoView.setVideoURI(Uri.fromFile(out));
                    videoView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    videoView.setOnPreparedListener(mp ->{
                        videoView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        sb.setMax(videoView.getDuration());
                        View.OnClickListener onclick = v -> {
                            if(videoView.isPlaying()){
                                videoView.pause();
                                startButton.setBackgroundResource(R.drawable.ic_saved_play);
                            } else {
                                videoView.start();
                                startButton.setBackgroundResource(R.drawable.ic_saved_pause);
                            }
                        };
                        startButton.setOnClickListener(onclick);
                        videoView.setOnClickListener(onclick);
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                sb.setProgress(videoView.getCurrentPosition());
                            }},0,1);
                        //videoView.setOnPreparedListener((b)->{});
                    });
                    videoView.setOnErrorListener((mp, what, extra) -> {
                        System.err.println(what+" "+extra);
                        return false;
                    });
                    layout.addView(videoView, 0);
                });

            } catch (IOException | DecoderException | JSONException | NullPointerException e) {
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
                try {
                    switch (actions[item]) {
                        case "Удалить сообщение":
                            AsyncHttpClient client = new AsyncHttpClient();
                            client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate=" + regdate + "&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&Date=" + date + "&Msg=" + URLEncoder.encode(text, "UTF-8"), new JsonHttpResponseHandler() {
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
                                            animation.setAnimationListener(new Animation.AnimationListener() {
                                                @Override
                                                public void onAnimationStart(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationEnd(Animation animation) {
                                                    Handler h = new Handler();
                                                    h.postAtTime(() -> MessagesList.removeView(layout), 100);
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
                        case "Скачать файл":
                            Download(text, date);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
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
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.isClickable();
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.ic_saved_box));
        layout.setGravity(Gravity.CENTER);

        SeekBar sb = new SeekBar(getActivity());
        sb.setClickable(false);
        sb.setOnTouchListener((v, event) -> true);
        sb.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        sb.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        sb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        MediaPlayer mPlayer = new MediaPlayer();
        Button startButton = new Button(getActivity());
        startButton.setBackgroundResource(R.drawable.ic_saved_play);
        startButton.setWidth(startButton.getHeight());
        startButton.setHeight(startButton.getWidth());
        startButton.setLayoutParams(new LinearLayout.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        LinearLayout bar = new LinearLayout(getActivity());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bar.addView(startButton);
        bar.addView(sb);
        layout.addView(bar);

        new Thread(() -> {
            try {
                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&FileName=" + URLEncoder.encode(text, "UTF-8") + "&Date=" + date);
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
                String filebody = result.getString("filebody");

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
                                    startButton.setBackgroundResource(R.drawable.ic_saved_play);
                                } else{
                                    mPlayer.start();
                                    startButton.setBackgroundResource(R.drawable.ic_saved_pause);
                                }
                            });
                            timer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    sb.setProgress(mPlayer.getCurrentPosition());
                                }
                            },0,1);
                            mPlayer.setOnPreparedListener((b)->{});
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            } catch (IOException | DecoderException | JSONException | NullPointerException e) {
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
                try {
                    switch (actions[item]) {
                        case "Удалить сообщение":
                            AsyncHttpClient client = new AsyncHttpClient();
                            client.get("http://mysweetyphone.herokuapp.com/?Type=DelMessage&RegDate=" + regdate + "&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&Date=" + date + "&Msg=" + URLEncoder.encode(text, "UTF-8"), new JsonHttpResponseHandler() {
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
                                            animation.setAnimationListener(new Animation.AnimationListener() {
                                                @Override
                                                public void onAnimationStart(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animation animation) {
                                                }

                                                @Override
                                                public void onAnimationEnd(Animation animation) {
                                                    Handler h = new Handler();
                                                    h.postAtTime(() -> MessagesList.removeView(layout), 100);
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
                        case "Скачать файл":
                            Download(text, date);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
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

    boolean isURL(String s){
        try {
            URL uri = new URL(s);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
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
                HttpPost post = new HttpPost("http://mysweetyphone.herokuapp.com/?Type=UploadFile&RegDate=" + regdate + "&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id);
                MultipartEntity entity = new MultipartEntity();
                entity.addPart("fileToUpload", new FileBody(file));
                post.setEntity(entity);
                HttpResponse response = client.execute(post);

                String body = IOUtils.toString(response.getEntity().getContent());
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
            } catch (IOException | JSONException | NullPointerException e) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved, container, false);
    }

    private void Download(String text, Long date){
        Runnable r = () -> {
            try {
                URL obj = new URL("http://mysweetyphone.herokuapp.com/?Type=DownloadFile&RegDate="+regdate+"&MyName=" + URLEncoder.encode(name, "UTF-8") + "&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&FileName=" + URLEncoder.encode(text, "UTF-8") + "&Date=" + date);
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
                String filebody = result.getString("filebody");
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