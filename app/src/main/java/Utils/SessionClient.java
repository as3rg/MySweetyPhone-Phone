package Utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mysweetyphone.phone.FileViewer;
import com.mysweetyphone.phone.MouseTracker;

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
        Server(Button b){
            this.b = b;
            value = 5;
        }
    }

    static ArrayList<SessionClient> servers;
    static Map<String, Server> ips;
    static boolean isSearching;
    static Thread searching;
    static DatagramSocket s;

    static{
        isSearching = false;
    }

    public static void Search(LinearLayout v, Thread onFinishSearching, Activity activity) throws SocketException {
        v.removeAllViews();
        if(isSearching) {
            System.err.println("Поиск уже запущен");
            return;
        }
        servers = new ArrayList<>();
        ips = new TreeMap<>();
        isSearching = true;
        s = new DatagramSocket(BroadCastingPort);
        s.setBroadcast(true);
        s.setSoTimeout(60000);
        byte[] buf = new byte[30];
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
                    if (!ips.containsKey(p.getAddress().getHostAddress())) {
                        servers.add(new SessionClient(p.getAddress(),ans.getInt("port"), Type.values()[ans.getInt("type")], activity));
                        Server server = new Server(null);
                        ips.put(p.getAddress().getHostAddress(),server);
                        activity.runOnUiThread(() -> {
                            Button ip = new Button(activity);
                            ip.setText(p.getAddress().getHostAddress());
                            server.b = ip;
                            ip.setTextColor(Color.parseColor("#F0F0F0"));
                            ip.setOnClickListener(event->{
                                servers.get(v.indexOfChild(ip)).Start();
                                v.removeView(ip);
                            });
                            v.setEnabled(true);
                            v.addView(ip);
                        });
                    }else
                        ips.get(p.getAddress().getHostAddress()).value=5;
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
        searching.interrupt();
        isSearching=false;
        s.close();
    }

    public SessionClient(InetAddress address, int Port, Type type, Activity activity) throws IOException {
        this.address = address;
        this.port = Port;
        this.type = type;
        switch (type){
            case MOUSE:
                break;
            case FILEVIEW:

        }

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
            default:
                throw new RuntimeException("Неизвестный тип сессии");
        }
    }

    public boolean isServer(){
        return false;
    }
}