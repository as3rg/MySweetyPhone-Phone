package Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.internal.platform.Platform;

public class SessionServer extends Session{
    Thread onStop;
    MessageParser messageParser;
    ServerSocket ss;

    public SessionServer(Type type, int Port, Runnable doOnStopSession, Activity thisActivity) throws IOException, JSONException {
        onStop = new Thread(doOnStopSession);
        messageParser = new MessageParser();
        JSONObject message = new JSONObject();
        switch (type){
            case FILEVIEW:
                ss = new ServerSocket(Port);
                port = ss.getLocalPort();
        }
        message.put("port", port);
        message.put("type", type.ordinal());
        byte[] buf2 = String.format("%-30s", message.toString()).getBytes();
        DatagramSocket s1 = new DatagramSocket();
        s1.setBroadcast(true);
        DatagramPacket packet = new DatagramPacket(buf2, buf2.length, Inet4Address.getByName("255.255.255.255"), BroadCastingPort);

        broadcasting = new Timer();
        TimerTask broadcastingTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    s1.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        broadcasting.schedule(broadcastingTask, 2000, 2000);

        switch (type) {
            case FILEVIEW:
                t = new Thread(()->{
                    try {
                        Ssocket = ss.accept();
                        broadcasting.cancel();
                        if(onStop != null){

                            thisActivity.runOnUiThread(onStop);
                            onStop = null;
                        }
                        PrintWriter writer = new PrintWriter(Ssocket.getOutputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(Ssocket.getInputStream()));
                        SimpleIntegerProperty gotAccess = new SimpleIntegerProperty(0);
                        while (true) {
                            String line = reader.readLine();
                            System.out.println(line);
                            JSONObject msg = new JSONObject(line);
                            if(gotAccess.get() == 0)
                                thisActivity.runOnUiThread(()-> {
                                    try {
                                        gotAccess.set(1);
                                        new AlertDialog.Builder(thisActivity)
                                                .setTitle("Выполнить действие?")
                                                .setMessage("Вы действительно хотите предоставить доступ к файлам \"" + msg.getString("Name") + "\"?")
                                                .setPositiveButton("Да", (dialog, which) -> gotAccess.set(2))
                                                .setNegativeButton("Нет",  (dialog, which) -> {
                                                    try {
                                                        Stop();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                })
                                                .show();

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                });

                            if(gotAccess.get() == 2){
                                JSONObject ans = new JSONObject();
                                if(msg.getString("Type").equals("showDir") && msg.getString("Dir").isEmpty())
                                    msg.put("Type", "start");
                                switch ((String)msg.get("Type")){
                                    case "back":
                                        if(msg.getString("Dir") != "/storage/emulated/0/") {
                                            String parentDir = new File((String) msg.get("Dir")).getParent();
                                            msg.put("Dir", parentDir);
                                        }
                                    case "start":
                                        if(msg.getString("Type").equals("start")) msg.put("Dir", "/storage/emulated/0/");
                                    case "showDir":
                                        File[] files;
                                        if(msg.getString("Dir").isEmpty()){
                                            files = File.listRoots();
                                            ans.put("Dir", "");
                                        }else{
                                            File dir = new File(msg.getString("Dir"));
                                            files = dir.listFiles();
                                            ans.put("Dir", dir.getPath());
                                        }
                                        JSONArray files2 = new JSONArray();
                                        ans.put("State", files != null ? 0 : 1);        //0 - без ошибок, 1 - нет доступа
                                        if(files != null) for(File f : files){
                                            JSONObject file = new JSONObject();
                                            file.put("Name", f.getName().isEmpty() ? f.getPath() : f.getName());
                                            file.put("Type", f.isDirectory() ? "Folder" : "File");
                                            files2.put(file);
                                        }
                                        ans.put("Inside", files2);
                                        ans.put("Type", "showDir");
                                        writer.println(ans.toString());
                                        writer.flush();
                                        break;
                                    case "newDir":
                                        ans.put("Type", "newDirAnswer");
                                        File file = new File(msg.getString("Dir"), msg.getString("DirName"));
                                        boolean state = file.mkdirs();
                                        ans.put("State", state ? 1 : 0);
                                        ans.put("Dir", msg.getString("Dir"));
                                        ans.put("DirName", msg.getString("DirName"));
                                        writer.println(ans.toString());
                                        writer.flush();
                                        break;
                                    case "uploadFile":
                                        new Thread(()->{
                                            try {
                                                Socket s = new Socket(((InetSocketAddress) Ssocket.getRemoteSocketAddress()).getAddress(), msg.getInt("FileSocketPort"));
                                                File getFile = new File(msg.getString("Dir"), msg.getString("FileName"));
                                                getFile.createNewFile();
                                                FileOutputStream fileout = new FileOutputStream(getFile);
                                                DataInputStream filein = new DataInputStream(s.getInputStream());
                                                IOUtils.copy(filein, fileout);
                                                s.close();
                                                fileout.close();
                                            } catch (IOException | JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }).start();
                                        break;
                                    case "downloadFile":
                                        new Thread(()->{
                                            try {
                                                Socket s = new Socket(((InetSocketAddress) Ssocket.getRemoteSocketAddress()).getAddress(), msg.getInt("FileSocketPort"));
                                                File getFile = new File(msg.getString("Dir"), msg.getString("FileName"));
                                                FileInputStream filein = new FileInputStream(getFile);
                                                DataOutputStream fileout = new DataOutputStream(s.getOutputStream());
                                                IOUtils.copy(filein, fileout);
                                                fileout.flush();
                                                s.close();
                                                filein.close();
                                            } catch (IOException | JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }).start();
                                        break;
                                }
                            }
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                });
                break;
            default:
                throw new RuntimeException("Неизвестный тип сессии");
        }
    }

    public boolean isServer(){
        return true;
    }

    @Override
    public void Stop() throws IOException {
        super.Stop();
        if(ss!=null)
            ss.close();
    }
}