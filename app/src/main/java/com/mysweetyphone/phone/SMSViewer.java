package com.mysweetyphone.phone;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Utils.SessionClient;
import Utils.SimpleProperty;

public class SMSViewer extends AppCompatActivity {

    private String name;
    int code;
    Thread receiving;
    PrintWriter writer;
    BufferedReader reader;
    static public SessionClient sc;

    LinearLayout SendBar;
    Button SendButton1;
    Button SendButton2;
    LinearLayout Messages;
    ScrollView ScrollView;
    EditText MessageText;
    Spinner Contacts;
    ArrayList<String> ContactList;
    int newContact = -1;
    Map<String, String> NameToNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NameToNumber = new HashMap<>();
        setContentView(R.layout.activity_smsviewer);
        Toolbar toolbar = findViewById(R.id.toolbarSMSVIEWER);
        setSupportActionBar(toolbar);

        name = (PreferenceManager.getDefaultSharedPreferences(this)).getString("name", "");
        code = PreferenceManager.getDefaultSharedPreferences(this).getInt("code", 0);

        ContactList = new ArrayList<>();

        SendBar = findViewById(R.id.ButtonsBarSMSVIEWER);
        SendButton1 = findViewById(R.id.SimOneSMSVIEWER);
        SendButton2 = findViewById(R.id.SimTwoSMSVIEWER);
        Messages = findViewById(R.id.MessagesSMSVIEWER);
        ScrollView = findViewById(R.id.ScrollBarSMSVIEWER);
        MessageText = findViewById(R.id.TextFieldSMSVIEWER);
        Contacts = findViewById(R.id.contactSMSVIEWER);

        Contacts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (newContact != -1 && Messages.getChildCount() == 0) {
                    ContactList.remove(newContact);
                    Contacts.setAdapter(new ArrayAdapter<>(getBaseContext(), R.layout.spinner_item, ContactList));
                }
                if(position != newContact) newContact = -1;
                Contacts.setClickable(false);
                new Thread(()-> {
                    try {
                        JSONObject msg = new JSONObject();
                        msg.put("Type", "showSMSs");
                        msg.put("Contact", Contacts.getSelectedItem().toString());
                        msg.put("Name", name);
                        msg.put("Number", NameToNumber.get(Contacts.getSelectedItem()));
                        writer.println(msg.toString());
                        writer.flush();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        receiving = new Thread(()-> {
            try {
                writer = new PrintWriter(sc.getSocket().getOutputStream());
                reader = new BufferedReader(new InputStreamReader(sc.getSocket().getInputStream()));
                Timer t = new Timer();
                LinearLayout folders = this.findViewById(R.id.foldersFILEVIEWER);
                t.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            JSONObject msg2 = new JSONObject();
                            msg2.put("Type", "start");
                            msg2.put("Name", name);
                            if(sc.getMode() != 0) msg2.put("Code", code % sc.getMode());
                            writer.println(msg2.toString());
                            writer.flush();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, 0, 2000);
                SimpleProperty<String>  Sim1 = new SimpleProperty<>(""), Sim2 = new SimpleProperty<>("");
                while (true) {
                    String line = reader.readLine();
                    t.cancel();
                    if(line == null){
                        sc.Stop();
                        finish();
                        break;
                    }
                    JSONObject msg = new JSONObject(line);
                    switch ((String) msg.get("Type")) {
                        case "start":
                            if(msg.has("Sim1"))
                                Sim1.set(msg.getString("Sim1"));
                            else
                                Sim1.set("Sim1");
                            if(msg.has("Sim2"))
                                Sim2.set(msg.getString("Sim2"));
                            else
                                Sim2.set("Sim2");

                            runOnUiThread(()->{
                                if(!msg.has("Sim1") && !msg.has("Sim2")){
                                    SendBar.setVisibility(View.GONE);
                                }else if(!msg.has("Sim1")){
                                    SendButton1.setVisibility(View.GONE);
                                    SendButton2.setText(Sim2.get());
                                }else if(!msg.has("Sim2")){
                                    SendButton2.setVisibility(View.GONE);
                                    SendButton1.setText(Sim1.get());
                                }else {
                                    SendButton1.setText(Sim1.get());
                                    SendButton2.setText(Sim2.get());
                                }
                            });
                            break;
                        case "getContacts":
                            JSONArray values = (JSONArray) msg.get("Contacts");
                            for(int i = 0; i < values.length(); i++){
                                JSONObject contact = (JSONObject) values.get(i);
                                NameToNumber.put(contact.has("Name") ? contact.getString("Name") : contact.getString("Number"), contact.getString("Number"));
                                ContactList.add(contact.has("Name") ? contact.getString("Name") : contact.getString("Number"));
                            }
                            runOnUiThread(()->Contacts.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, ContactList)));
                            break;
                        case "showSMSs":
                            if(!msg.getString("Contact").equals(Contacts.getSelectedItem())) break;
                            values = msg.getJSONArray("SMS");
                            if(values.length() == 0) newContact = Contacts.getSelectedItemPosition();
                            runOnUiThread(() -> {
                                Contacts.setClickable(true);
                                SendBar.setVisibility(View.VISIBLE);
                                Messages.removeAllViews();
                                for (int i = 0; i < values.length(); i++) {
                                    try {
                                        JSONObject message = (JSONObject) values.get(i);
                                        DrawText(message.getString("text"), message.getLong("date"), false, message.getInt("type"), message.getInt("sim") == 1 ? Sim1.get() : Sim2.get());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                            });
                            break;
                        case "getContact":
                            runOnUiThread(() -> {
                                try {
                                    String name = (String) msg.get("Contact");
                                    if (!ContactList.contains(name)) {
                                        ContactList.add(name);
                                    }
                                    Contacts.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, ContactList));
                                    Contacts.setSelection(ContactList.indexOf(name));
                                    newContact = ContactList.indexOf(name);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                            break;
                        case "newSMSs":
                            values = (JSONArray) msg.get("SMS");
                            runOnUiThread(() -> {
                                for (int i = 0; i < values.length(); i++) {
                                    try {
                                        JSONObject message = (JSONObject) values.get(i);
                                        String number = message.getString("contact");
                                        if(number.equals(Contacts.getSelectedItem()))
                                            DrawText(message.getString("text"), message.getLong("date"), true, message.getInt("type"), message.getInt("sim") == 1 ? Sim1.get() : Sim2.get());
                                        else if(!ContactList.contains(number))
                                            ContactList.add(number);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if(ContactList.size() != Contacts.getCount())
                                    Contacts.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, ContactList));
                            });
                            break;
                        case "finish":
                            sc.Stop();
                            finish();
                            break;
                    }
                }
            } catch (ConnectException e){
                runOnUiThread(()->{
                    Toast toast = Toast.makeText(this,
                            "Сессия закрыта", Toast.LENGTH_LONG);
                    toast.show();
                });
            } catch (IOException | JSONException | NullPointerException e) {
                e.printStackTrace();
            }
        });
        receiving.start();
    }



    @SuppressLint("SetTextI18n")
    private void DrawText(String text, long date, Boolean needsAnim, int type, String sender) {
        LinearLayout hLayout = new LinearLayout(this);
        hLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        if(type == 1){
            layout.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_sms_viewer_in));
            hLayout.setGravity(Gravity.START);
        }else if(type == 2){
            layout.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_sms_viewer_out));
            hLayout.setGravity(Gravity.END);
        }else if(type == 5){
            layout.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_sms_viewer_fail));
            hLayout.setGravity(Gravity.END);
        }else return;
        TextView textBox = new TextView(this);
        if (text.length() == 0) {
            textBox.setText("Пустое сообщение");
            textBox.setTextColor(Color.parseColor("#cccccc"));
            textBox.setTypeface(null, Typeface.ITALIC);
        } else {
            textBox.setText(text);
        }
        textBox.setTextSize(20);
        textBox.setMaxWidth(400);
        layout.addView(textBox);
        layout.setWeightSum(0.6f);
        TextView dateBox = new TextView(this);
        Date Date = new java.util.Date(date * 1000L);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm dd.MM.yyyy");
        dateBox.setText(format.format(Date) + ", " + sender);
        layout.addView(dateBox);
        layout.setPadding(35, 35, 35, 35);

        layout.setOnLongClickListener(v -> {
            String[] actions;
            if(isURL(text))
                actions = new String[]{"Копировать текст", "Открыть ссылку"};
            else
                actions = new String[]{"Копировать текст"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setItems(actions, (dialog, item) -> {
                try {
                    switch (actions[item]) {
                        case "Копировать текст":
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("", text);
                            clipboard.setPrimaryClip(clip);
                            break;
                        case "Открыть ссылку":
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(new URL(text).toURI().toString()));
                            startActivity(browserIntent);
                            break;
                    }
                } catch (MalformedURLException | URISyntaxException | NullPointerException e) {
                    e.printStackTrace();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return false;
        });

        hLayout.addView(layout);
        Messages.addView(hLayout, Messages.getChildCount());
        if (needsAnim) {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.send_anim);
            hLayout.startAnimation(anim);
        }
        ScrollView.fullScroll(View.FOCUS_DOWN);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        receiving.interrupt();
        new Thread(() -> {
            try {
                JSONObject msg2 = new JSONObject();
                msg2.put("Type", "finish");
                msg2.put("Name", name);
                if(sc.getMode() != 0) msg2.put("Code", code % sc.getMode());
                writer.println(msg2.toString());
                writer.flush();
            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
            }
        }).start();
    }

    boolean isURL(String s){
        try {
            URL uri = new URL(s);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public void onSendClick1(View v){
        onSendClick(1);
    }

    public void onSendClick2(View v){
        onSendClick(2);
    }

    private void onSendClick(int i){
        new Thread(() -> {
            try {
                JSONObject msg2 = new JSONObject();
                msg2.put("Type", "sendSMS");
                msg2.put("Number", Contacts.getSelectedItem());
                msg2.put("Text", MessageText.getText());
                msg2.put("Name", name);
                if(sc.getMode() != 0) msg2.put("Code", code % sc.getMode());
                msg2.put("Sim", i);
                writer.println(msg2.toString());
                writer.flush();
                runOnUiThread(() -> MessageText.setText(""));
            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void Stop(View v){
        new Thread(()-> {
            try {
                JSONObject msg3 = new JSONObject();
                msg3.put("Type", "finish");
                msg3.put("Name", name);
                writer.println(msg3.toString());
                writer.flush();
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void onNewContact(View v){
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        android.support.v7.app.AlertDialog alert = new android.support.v7.app.AlertDialog.Builder(this)
                .setTitle("Введите номер телефона...")
                .setMessage("Введите номер телефона...")
                .setView(input)
                .setPositiveButton("Открыть", ((dialog, which) -> {
                    new Thread(() -> {
                        try {
                            JSONObject msg2 = new JSONObject();
                            msg2.put("Type", "getContact");
                            msg2.put("Number", input.getText().toString());
                            writer.println(msg2.toString());
                            writer.flush();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }))
                .setNegativeButton("Отмена", (dialog, which) -> {})
                .create();
        alert.show();
    }
}