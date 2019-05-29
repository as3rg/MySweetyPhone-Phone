package Utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mysweetyphone.phone.FileViewer;
import com.mysweetyphone.phone.MouseTracker;
import com.mysweetyphone.phone.SMSViewer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class SessionClient extends Session{

    static private class Server{
        public int value;
        public Button b;
        public SessionClient sc;
        Server(Button b, SessionClient sc){
            this.b = b;
            this.sc = sc;
            value = 5;
        }
    }

    static Map<String, Server> ips;
    static boolean isSearching;
    static Thread searching;
    static DatagramSocket s;
    String os;

    public String getOS(){
        return os;
    }

    static{
        isSearching = false;
    }

    public static void Search(LinearLayout v, Thread onFinishSearching, Activity activity) throws SocketException {
        v.removeAllViews();
        if(isSearching) {
            StopSearching();
        }
        ips = new TreeMap<>();
        isSearching = true;
        s = new DatagramSocket(BroadCastingPort);
        s.setBroadcast(true);
        s.setSoTimeout(60000);
        byte[] buf = new byte[100];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        long time = System.currentTimeMillis();
        Timer t = new Timer();
        searching = new Thread(() -> {
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    for (String item : ips.keySet()) {
                        if (ips.get(item).value == 1) {
                            Button b = ips.get(item).b;
                            activity.runOnUiThread(() -> v.removeView(b));
                            ips.remove(item);
                        }else
                            ips.get(item).value--;
                    }
                }
            }, 0, 2000);
            try {
                while (System.currentTimeMillis() - time <= 60000) {
                    s.receive(p);
                    JSONObject ans = new JSONObject(new String(p.getData()));
                    String name = ans.get("name") + "(" + p.getAddress().getHostAddress() + "): " + decodeType((ans.getInt("type")));
                    if (!ips.containsKey(name)) {
                        Server server = new Server(null, new SessionClient(p.getAddress(),ans.getInt("port"), ans.getInt("type"), ans.has("os") ? ans.getString("os") : "", activity));
                        ips.put(name,server);
                        activity.runOnUiThread(() -> {
                            Button ip = new Button(activity);
                            ip.setText(name);
                            server.b = ip;
                            ip.setTextColor(Color.parseColor("#F0F0F0"));
                            ip.setOnClickListener(event->{
                                server.sc.Start();
                                v.removeView(ip);
                            });
                            v.setEnabled(true);
                            v.addView(ip);
                        });
                    }else
                        ips.get(name).value=5;
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            isSearching = false;
            s.close();
            t.cancel();
            activity.runOnUiThread(onFinishSearching);
        });
        searching.start();
    }

    public static void StopSearching() {
        try {
            searching.interrupt();
        }catch (NullPointerException ignored){}
        isSearching=false;
        try{
            s.close();
        }catch (NullPointerException ignored){}
    }

    public SessionClient(InetAddress address, int Port, int type, String os, Activity activity) throws IOException {
        this.address = address;
        this.port = Port;
        this.type = type;
        this.os = os;

        switch (type) {
            case MOUSE:
                t = new Thread(()->{
                    try {
                        Dsocket = new DatagramSocket();
                        Dsocket.setBroadcast(true);
                        if (searching != null) StopSearching();
                        activity.runOnUiThread(() -> {
                            MouseTracker.sc = this;
                            Intent intent = new Intent(activity, MouseTracker.class);
                            activity.startActivity(intent);
                        });
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                });
                break;
            case FILEVIEW:
                t = new Thread(()->{
                    if(searching != null) StopSearching();
                    activity.runOnUiThread(()->{
                        FileViewer.sc = this;
                        Intent intent = new Intent(activity, FileViewer.class);
                        activity.startActivity(intent);
                    });
                });
                break;
            case SMSVIEWER:
                t = new Thread(()->{
                    try {
                        if (searching != null) StopSearching();
                        Ssocket = new Socket(address, port);
                        activity.runOnUiThread(() -> {
                            SMSViewer.sc = this;
                            Intent intent = new Intent(activity, SMSViewer.class);
                            activity.startActivity(intent);
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                break;
            default:
                throw new RuntimeException("Неизвестный тип сессии");
        }
    }

    public boolean isServer(){
        return false;
    }
}