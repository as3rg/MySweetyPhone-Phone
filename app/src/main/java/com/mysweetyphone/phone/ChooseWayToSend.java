package com.mysweetyphone.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.regex.Pattern;

import okhttp3.internal.platform.Platform;

public class ChooseWayToSend extends AppCompatActivity {

    static private class Server{
        public int value;
        public Button b;
        public int port;
        Server(Button b, int p){
            this.b = b;
            value = 5;
            port = p;
        }
    }
    static Map<String, Server> ips;
    private static final int BROADCASTINGSIZE = 100;
    DatagramSocket socket;

    private static final int BroadCastingPort = 9500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_way_to_send);
        RadioGroup rg = findViewById(R.id.wayToSendCHOOSEWAY);
        if(!Pattern.matches("https?://([0-9a-zA-Z]+\\.){1,2}[0-9a-zA-Z]+(/.*)*", getIntent().getStringExtra(Intent.EXTRA_TEXT))){
            RadioButton rb = findViewById(R.id.openSiteCHOOSEWAY);
            rg.removeView(rb);
        }
        ips = new TreeMap<>();
    }


    public void Send(View v) throws SocketException {
        RadioGroup rg = findViewById(R.id.wayToSendCHOOSEWAY);
        if(rg.getCheckedRadioButtonId() == R.id.sendToSavedCHOOSEWAY) {
            ChangeActivity(Main.class);
        }
        else {
            long time = System.currentTimeMillis();
            socket = new DatagramSocket(BroadCastingPort);
            Dialog d = new Dialog(this);
            LinearLayout ll = new LinearLayout(this);
            TextView label = new TextView(this);
            label.setText("Выберите получателя:");
            label.setTextSize(20);
            label.setTextColor(Color.BLACK);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.addView(label);
            ll.setPadding(10,10,10,10);
            d.setContentView(ll);
            Thread searching = new Thread(() -> {
                try{
                    byte[] buf = new byte[BROADCASTINGSIZE];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    while (System.currentTimeMillis() - time <= 60000) {
                        socket.receive(p);
                        JSONObject ans = new JSONObject(new String(p.getData()));
                        Button b = new Button(this);
                        b.setBackgroundColor(Color.WHITE);
                        b.setText(p.getAddress().getHostAddress());
                        Server s = new Server(b,ans.getInt("port"));
                        Activity thisActivity = this;
                        b.setOnClickListener(button-> new Thread(()-> {
                            try(DatagramSocket sendsocket = new DatagramSocket()) {
                                runOnUiThread(()->{
                                    FrameLayout fl = new FrameLayout(thisActivity);
                                    TextView waiting = new TextView(thisActivity);
                                    fl.addView(waiting);
                                    waiting.setGravity(Gravity.CENTER);
                                    waiting.setText("Ожидание завершения действия...");
                                    waiting.setTextColor(Color.WHITE);
                                    waiting.setTextSize(20);
                                    thisActivity.setContentView(fl);
                                    d.setOnCancelListener(dialog -> {});
                                    d.cancel();
                                });
                                sendsocket.setBroadcast(true);
                                JSONObject messages = new JSONObject();
                                if (rg.getCheckedRadioButtonId() == R.id.openSiteCHOOSEWAY) {
                                    messages.put("type", "openSite");
                                    messages.put("site", getIntent().getStringExtra(Intent.EXTRA_TEXT));
                                } else {
                                    messages.put("type", "copy");
                                    messages.put("value", getIntent().getStringExtra(Intent.EXTRA_TEXT));
                                }
                                for (Message m : Message.getMessages(messages.toString().getBytes(), Message.BODYMAXIMUM/10)) {
                                    sendsocket.send(new DatagramPacket(m.getArr(), m.getArr().length, p.getAddress(),ans.getInt("port")));
                                }
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                            socket.close();
                            thisActivity.finish();
                        }).start());
                        if (!ips.containsKey(p.getAddress().getHostAddress())) {
                            ips.put(p.getAddress().getHostAddress(), s);
                            this.runOnUiThread(()->{
                                ll.addView(b);
                            });
                        }else
                            ips.get(p.getAddress().getHostAddress()).value++;
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            });
            searching.start();
            d.setOnCancelListener(dialog -> {
                socket.close();
                searching.interrupt();
            });
            d.show();
        }
    }

    private void ChangeActivity(Class<?> cls){
        Intent intent = new Intent(this, cls);
        intent.putExtras(getIntent());
        intent.setAction(getIntent().getAction());
        startActivity(intent);
        finish();
    }

}
