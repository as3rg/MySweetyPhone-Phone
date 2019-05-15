package com.mysweetyphone.phone;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

import Utils.Message;
import Utils.MessageParser;
import Utils.SessionClient;
import Utils.SessionServer;

public class FileViewer extends AppCompatActivity {

    static public SessionClient sc;
    Thread receiving;
    DatagramSocket receiver;
    static final int MESSAGESIZE = 100;
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer);
        TextView path = findViewById(R.id.pathFILEVIEWER);

        name = PreferenceManager.getDefaultSharedPreferences(this).getString("name", "");
        receiving = new Thread(()-> {
            try {
                receiver = new DatagramSocket();
                receiver.setBroadcast(true);
                MessageParser messageParser = new MessageParser();
                DatagramPacket p;
                Timer t = new Timer();
                LinearLayout folders = this.findViewById(R.id.foldersFILEVIEWER);
                t.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        JSONObject msg2 = new JSONObject();
                        try {
                            msg2.put("Type", "start");
                            msg2.put("Name", name);
                            msg2.put("BackChatPort", receiver.getLocalPort());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Send(msg2.toString().getBytes());
                    }
                }, 0, 2000);
                while (!receiver.isClosed()) {
                    Message m = null;
                    int head = -1;
                    p = null;
                    do {
                        byte[] buf = new byte[Message.getMessageSize(MESSAGESIZE)];
                        p = new DatagramPacket(buf, buf.length);
                        try {
                            receiver.receive(p);
                            m = new Message(p.getData());
                            messageParser.messageMap.put(m.getId(), m);
                            if (head == -1)
                                head = m.getId();
                        } catch (SocketException ignored) {
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } while (!receiver.isClosed() && (m == null || m.getNext() != -1));
                    if (messageParser.messageMap.get(head) == null) continue;
                    t.cancel();
                    String msgString = new String(messageParser.parse(head));
                    JSONObject msg = new JSONObject(msgString);
                    JSONArray values = msg.getJSONArray("Inside");
                    this.runOnUiThread(()-> {
                        try {
                            folders.removeAllViews();
                            path.setText(msg.getString("Dir"));
                            findViewById(R.id.backFILEVIEWER).setVisibility(path.getText().toString().isEmpty() ? View.INVISIBLE : View.VISIBLE);
                            for (int i = 0; i < values.length(); i++) {
                                TextView folder = new TextView(this);
                                folder.setText(values.getJSONObject(i).getString("Name"));
                                folder.setPadding(20,20,20,20);
                                folder.setTextSize(20);
                                folder.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                                Drawable d = getDrawable(values.getJSONObject(i).getString("Type").equals("Folder") ? R.drawable.ic_file_viewer_folder  : R.drawable.ic_file_viewer_file);
                                d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                                folder.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
                                folders.addView(folder);
                                final int i2 = i;
                                folder.setOnClickListener(v-> {
                                    try {
                                        if (values.getJSONObject(i2).getString("Type").equals("Folder"))
                                            new Thread(() -> {
                                                JSONObject msg2 = new JSONObject();
                                                try {
                                                    msg2.put("Type", "showDir");
                                                    msg2.put("Name", name);
                                                    msg2.put("BackChatPort", receiver.getLocalPort());
                                                    msg2.put("Dir", new File(msg.getString("Dir"), values.getJSONObject(i2).getString("Name")).toPath());

                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                Send(msg2.toString().getBytes());
                                            }).start();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (SocketException | JSONException e) {
                e.printStackTrace();
            }
        });
        receiving.start();
        View content = findViewById(android.R.id.content);
    }

    public void Send(byte[] b) {
        new Thread(()-> {
            try {
                Message[] messages = Message.getMessages(b, MESSAGESIZE);
                for (Message m : messages) {
                    sc.getSocket().send(new DatagramPacket(m.getArr(), m.getArr().length, sc.getAddress(), sc.getPort()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        receiver.close();
        receiving.interrupt();
        try {
            JSONObject msg = new JSONObject();
            msg.put("Type", "finish");
            msg.put("Name", name);
            Send(msg.toString().getBytes());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String getFolderName(String filePath) {

        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }

        int filePosi = filePath.lastIndexOf(File.separator);
        return (filePosi == -1) ? "" : filePath.substring(0, filePosi);
    }

    public void back(View v){
        new Thread(() -> {
            JSONObject msg2 = new JSONObject();
            try {
                msg2.put("Type", "showDir");
                msg2.put("Name", name);
                msg2.put("BackChatPort", receiver.getLocalPort());
                msg2.put("Dir", getFolderName(((TextView)findViewById(R.id.pathFILEVIEWER)).getText().toString()));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            Send(msg2.toString().getBytes());
        }).start();
    }

}
