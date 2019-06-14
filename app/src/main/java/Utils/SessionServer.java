package Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.Dimension;
import android.support.design.widget.Snackbar;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.InputType;
import android.text.Layout;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mysweetyphone.phone.IME;
import com.mysweetyphone.phone.Main;
import com.mysweetyphone.phone.MouseTracker;
import com.mysweetyphone.phone.R;

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
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class SessionServer extends Session{
    private Thread onStop;
    private ServerSocket ss;
    private SimpleProperty<Long> lastSync = new SimpleProperty<>(0L);
    private SimpleProperty<String> currentNumber = new SimpleProperty<>("");

    public SessionServer(int type, int Port, Runnable doOnStopSession, Context thisContext) throws IOException, JSONException {
        onStop = new Thread(doOnStopSession);
        MessageParser messageParser = new MessageParser();
        String name = (PreferenceManager.getDefaultSharedPreferences(thisContext)).getString("name", "");
        JSONObject message = new JSONObject();
        switch (type){
            case MOUSE:
                Dsocket = new DatagramSocket(Port);
                Dsocket.setBroadcast(true);
                port = Dsocket.getLocalPort();
                break;
            case SMSVIEWER:
            case FILEVIEW:
                ss = new ServerSocket(Port);
                port = ss.getLocalPort();
        }
        message.put("port", port);
        message.put("type", type);
        message.put("name", name);
        message.put("subtype", "Phone");
        byte[] buf2 = String.format("%-100s", message.toString()).getBytes();
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
            case MOUSE:
                t = new Thread(() -> {
                    try {
                        Dsocket.setBroadcast(true);
                        DatagramPacket p;
                        SimpleProperty gotAccess = new SimpleProperty(0);
                        LinearLayout layout = (LinearLayout) ((IME) thisContext).layout;
                        while (!Dsocket.isClosed()) {
                            if(gotAccess.get().equals(1))
                                continue;
                            Message m = null;
                            int head = -1;
                            p = null;
                            do{
                                byte[] buf = new byte[Message.getMessageSize(MouseTracker.MESSAGESIZE)];
                                p = new DatagramPacket(buf, buf.length);
                                try {
                                    Dsocket.receive(p);
                                    broadcasting.cancel();
                                    if(onStop != null){
                                        onStop.start();
                                        onStop = null;
                                    }
                                    m = new Message(p.getData());
                                    messageParser.messageMap.put(m.getId(), m);
                                    if (head == -1)
                                        head = m.getId();
                                } catch (SocketException ignored){
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }while (!Dsocket.isClosed() && (m == null || m.getNext() != -1));
                            if(messageParser.messageMap.get(head) == null) continue;
                            String msgString = new String(messageParser.parse(head));
                            JSONObject msg = new JSONObject(msgString);
                            if(gotAccess.get().equals(0)) {
                                gotAccess.set(1);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                params.gravity = Gravity.CENTER;

                                LinearLayout notification = new LinearLayout(thisContext);
                                notification.setOrientation(LinearLayout.HORIZONTAL);

                                Button yes = new Button(thisContext);
                                yes.setText("Да");
                                yes.setLayoutParams(params);
                                yes.setOnClickListener((v -> {
                                    gotAccess.set(2);
                                    layout.removeView(notification);
                                }));
                                yes.setMinimumWidth(200);
                                notification.addView(yes);

                                Button no = new Button(thisContext);
                                no.setText("Нет");
                                no.setLayoutParams(params);
                                no.setOnClickListener((v -> {
                                    try {
                                        Stop();
                                        layout.removeView(notification);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }));
                                no.setMinimumWidth(200);
                                notification.addView(no);

                                TextView text = new TextView(thisContext);
                                text.setText("Вы действительно хотите предоставить доступ к клавиатуре \"" + msg.getString("Name") + "\"?");
                                text.setTextColor(Color.WHITE);
                                text.setLayoutParams(params);
                                notification.addView(text);

                                Handler mainHandler = new Handler(thisContext.getMainLooper());
                                mainHandler.post(()-> {
                                    layout.addView(notification);
                                });
                            }

                            if(gotAccess.get().equals(2))
                                try {
                                    switch (msg.getString("Type")) {
                                        case "keyReleased":
                                            ((IME)thisContext).getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, MouseTracker.AwtToAndroid(msg.getInt("value"))));
                                            break;
                                        case "keyPressed":
                                            ((IME)thisContext).getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MouseTracker.AwtToAndroid(msg.getInt("value"))));
                                            break;
                                        case "keyClicked":
                                            ((IME)thisContext).getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MouseTracker.AwtToAndroid(msg.getInt("value"))));
                                            ((IME)thisContext).getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, MouseTracker.AwtToAndroid(msg.getInt("value"))));
                                            break;
                                        case "keysTyped":
                                            ((IME)thisContext).getCurrentInputConnection().commitText(msg.getString("value"),1);
                                            break;
                                        case "start":
                                            break;
                                        default:
                                            System.err.println(msgString);
                                    }
                                }catch (IllegalArgumentException ignored){}
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                });
                break;
            case FILEVIEW:
                t = new Thread(()->{
                    try {
                        Ssocket = ss.accept();
                        PrintWriter writer = new PrintWriter(Ssocket.getOutputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(Ssocket.getInputStream()));
                        SimpleProperty gotAccess = new SimpleProperty(0);
                        while (true) {
                            String line = reader.readLine();
                            broadcasting.cancel();
                            if(onStop != null){
                                ((Activity)thisContext).runOnUiThread(onStop);
                                onStop = null;
                            }
                            if(line == null){
                                Stop();
                                break;
                            }
                            JSONObject msg = new JSONObject(line);
                            if(gotAccess.get().equals(0)) {
                                gotAccess.set(1);
                                ((Activity)thisContext).runOnUiThread(() -> {
                                    try {
                                        new AlertDialog.Builder(thisContext)
                                                .setTitle("Выполнить действие?")
                                                .setMessage("Вы действительно хотите предоставить доступ к файлам \"" + msg.getString("Name") + "\"?")
                                                .setPositiveButton("Да", (dialog, which) -> gotAccess.set(2))
                                                .setNegativeButton("Нет", (dialog, which) -> {
                                                    new Thread(() -> {
                                                        try {
                                                            JSONObject ans = new JSONObject();
                                                            ans.put("Type", "finish");
                                                            writer.println(ans.toString());
                                                            writer.flush();
                                                            Stop();
                                                        } catch (JSONException | IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }).start();
                                                })
                                                .show();

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                            if(gotAccess.get().equals(2)){
                                JSONObject ans = new JSONObject();
                                if(msg.getString("Type").equals("showDir") && msg.getString("Dir").isEmpty())
                                    msg.put("Type", "start");
                                switch (msg.getString("Type")){
                                    case "finish":
                                        Ssocket.close();
                                        Stop();
                                        break;
                                    case "back":
                                        if(msg.getString("Dir") != "/storage/emulated/0/") {
                                            String parentDir = new File(msg.getString("Dir")).getParent();
                                            msg.put("Dir", parentDir);
                                        }
                                    case "start":
                                        if(msg.getString("Type").equals("start")) msg.put("Dir", "/storage/emulated/0/");
                                    case "showDir":
                                        File[] files;
                                        if(msg.getString("Dir").isEmpty() && (!msg.has("DirName") || msg.getString("DirName").isEmpty())){
                                            files = File.listRoots();
                                            ans.put("Dir", "");
                                        }else{
                                            File dir = new File(msg.getString("Dir"), msg.has("DirName") ? msg.getString("DirName") : "");
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
                                    case "deleteFile":
                                        file = new File((String)msg.get("Dir"), (String)msg.get("FileName"));
                                        boolean result = file.delete();
                                        ans.put("State", result ? 0 : 1);        //0 - без ошибок, 1 - нет доступа
                                        ans.put("Type", "deleteFile");
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
                    } catch (IOException | JSONException | NullPointerException e) {
                        e.printStackTrace();
                    }
                });
                break;
            case SMSVIEWER:
                t = new Thread(()->{
                    try {
                        Ssocket = ss.accept();
                        PrintWriter writer = new PrintWriter(Ssocket.getOutputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(Ssocket.getInputStream()));
                        SimpleProperty<Integer> gotAccess = new SimpleProperty<>(0);
                        while (true) {
                            String line = reader.readLine();
                            broadcasting.cancel();
                            if(onStop != null){
                                ((Activity)thisContext).runOnUiThread(onStop);
                                onStop = null;
                            }
                            if(line == null) {
                                Stop();
                                break;
                            }
                            JSONObject msg = new JSONObject(line);
                            if(gotAccess.get() == 0) {
                                gotAccess.set(1);
                                ((Activity)thisContext).runOnUiThread(() -> {
                                    try {
                                        new AlertDialog.Builder(thisContext)
                                                .setTitle("Выполнить действие?")
                                                .setMessage("Вы действительно хотите предоставить доступ к сообщениям \"" + msg.getString("Name") + "\"?")
                                                .setPositiveButton("Да", (dialog, which) -> {
                                                    gotAccess.set(2);
                                                    new Thread(() -> {
                                                        try {
                                                            JSONObject msg2 = new JSONObject();
                                                            msg2.put("Type", "accepted");
                                                            writer.println(msg2.toString());
                                                            writer.flush();
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }).start();
                                                })
                                                .setNegativeButton("Нет", (dialog, which) -> {
                                                    new Thread(() -> {
                                                        try {
                                                            JSONObject ans = new JSONObject();
                                                            ans.put("Type", "finish");
                                                            writer.println(ans.toString());
                                                            writer.flush();
                                                            Stop();
                                                        } catch (JSONException | IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }).start();
                                                })
                                                .show();

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                            if(gotAccess.get() == 2){
                                Timer t = new Timer();
                                JSONObject ans = new JSONObject();
                                switch (msg.getString("Type")){
                                    case "finish":
                                        Ssocket.close();
                                        Stop();
                                    case "start":
                                        SubscriptionManager subscriptionManager = (SubscriptionManager) thisContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                                        SubscriptionInfo sim = subscriptionManager.getActiveSubscriptionInfo(1);
                                        if(sim != null) ans.put("Sim1", sim.getDisplayName());
                                        sim = subscriptionManager.getActiveSubscriptionInfo(2);
                                        if(sim != null) ans.put("Sim2", sim.getDisplayName());
                                        ans.put("Type", "start");
                                        writer.println(ans.toString());
                                        writer.flush();
                                        ans = new JSONObject();
                                        t.scheduleAtFixedRate(new TimerTask() {
                                            @Override
                                            public void run() {
                                                try {
                                                    if((System.currentTimeMillis()/1000 - lastSync.get()) < 60 || currentNumber.get().isEmpty()) return;
                                                    JSONObject ans = new JSONObject();
                                                    JSONArray sms = new JSONArray();
                                                    @SuppressLint("Recycle") Cursor cur = thisContext.getContentResolver().query(Uri.parse("content://sms"), new String[]{Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.SUBSCRIPTION_ID}, "CAST(" + Telephony.Sms.DATE + " AS INTEGER)/1000 >= "+ lastSync.get(), null, Telephony.Sms.DATE + " ASC");
                                                    while (cur != null && cur.moveToNext()) {
                                                        String number = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                                                        JSONObject a = new JSONObject();
                                                        a.put("text", cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.BODY)));
                                                        a.put("date", Long.parseLong(cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.DATE))) / 1000);
                                                        a.put("type", cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                                                        a.put("sim", cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID)));

                                                        if(number.isEmpty()) continue;
                                                        Cursor cur2 = thisContext
                                                                .getContentResolver()
                                                                .query(
                                                                        Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                                                                Uri.encode(number)),
                                                                        new String[]{ContactsContract.Data.DISPLAY_NAME},
                                                                        null,
                                                                        null,
                                                                        null
                                                                );
                                                        cur2.moveToFirst();
                                                        if(number.replaceAll("[ \\-()]","").matches("\\+\\d{7,13}"))
                                                            number = number.replaceAll("[ \\-()]","");
                                                        if(cur2.getCount() > 0) a.put("contact", cur2.getString(0)+"("+number+")");
                                                        else a.put("contact", number);
                                                        sms.put(a);
                                                    }
                                                    if(sms.length() == 0) return;
                                                    ans.put("SMS", sms);
                                                    ans.put("Type", "newSMSs");
                                                    writer.println(ans.toString());
                                                    writer.flush();
                                                    lastSync.set(System.currentTimeMillis() / 1000);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }, 0, 45000);
                                    case "getContacts":
                                        Set<String> contacts = new HashSet<>();
                                        Cursor cur = thisContext.getContentResolver().query(Uri.parse("content://sms"), new String[]{Telephony.Sms.ADDRESS}, null, null, null);
                                        while (cur != null && cur.moveToNext()) {
                                            String number = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                                            if(number == null || number.isEmpty()) continue;
                                            Cursor cur2 = thisContext
                                                    .getContentResolver()
                                                    .query(
                                                            Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                                                    Uri.encode(number)),
                                                            new String[]{ContactsContract.Data.DISPLAY_NAME},
                                                            null,
                                                            null,
                                                            null
                                                    );
                                            cur2.moveToFirst();
                                            if(number.replaceAll("[ \\-()]","").matches("\\+\\d{7,13}"))
                                                number = number.replaceAll("[ \\-()]","");
                                            if(cur2.getCount() > 0) contacts.add(cur2.getString(0)+"("+number+")");
                                            else contacts.add(number);
                                        }
                                        ans.put("Type", "getContacts");
                                        ans.put("Contacts", new JSONArray(contacts));
                                        writer.println(ans.toString());
                                        writer.flush();
                                        break;
                                    case "getContact":
                                        String number = msg.getString("Number");
                                        Cursor cur2 = thisContext
                                            .getContentResolver()
                                            .query(
                                                    Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                                            Uri.encode(number)),
                                                    new String[]{ContactsContract.Data.DISPLAY_NAME},
                                                    null,
                                                    null,
                                                    null
                                            );
                                        cur2.moveToFirst();
                                        if(number.replaceAll("[ \\-()]","").matches("\\+\\d{7,13}"))
                                            number = number.replaceAll("[ \\-()]","");
                                        if(cur2.getCount() > 0) ans.put("Contact", cur2.getString(0)+"("+number+")");
                                        else ans.put("Contact", number);
                                        ans.put("Type", "getContact");
                                        writer.println(ans.toString());
                                        writer.flush();
                                        break;
                                    case "showSMSs":
                                        currentNumber.set(msg.getString("Number"));
                                        JSONArray sms = new JSONArray();
                                        cur = thisContext.getContentResolver().query(Uri.parse("content://sms"), new String[]{Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.SUBSCRIPTION_ID}, null, null, Telephony.Sms.DATE + " ASC");
                                        while (cur != null && cur.moveToNext()) {
                                            number = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                                            if(number == null || !(number.replaceAll("[ \\-()]","").equals(msg.getString("Number")) || number.equals(msg.getString("Number")))
                                            ) {
                                                continue;
                                            }
                                            JSONObject a = new JSONObject();
                                            a.put("text", cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.BODY)));
                                            a.put("date", Long.parseLong(cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.DATE)))/1000);
                                            a.put("type", cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                                            a.put("sim", cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID)));
                                            sms.put(a);
                                        }
                                        ans.put("SMS", sms);
                                        ans.put("Contact", msg.getString("Contact"));
                                        ans.put("Type", "showSMSs");
                                        writer.println(ans.toString());
                                        writer.flush();
                                        lastSync.set(System.currentTimeMillis()/1000);
                                        break;
                                    case "sendSMS":
                                        SmsManager smgr = SmsManager.getSmsManagerForSubscriptionId(msg.getInt("Sim"));
                                        smgr.sendTextMessage(msg.getString("Number"), null, msg.getString("Text"), null, null);
                                        Thread.sleep(2000);
                                        ans = new JSONObject();
                                        sms = new JSONArray();
                                        cur = thisContext.getContentResolver().query(Uri.parse("content://sms"), new String[]{Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.SUBSCRIPTION_ID}, "CAST(" + Telephony.Sms.DATE + " AS INTEGER)/1000 >= "+ lastSync.get(), null, Telephony.Sms.DATE + " ASC");
                                        while (cur != null && cur.moveToNext()) {
                                            number = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                                            if (number == null || !(number.replaceAll("[ \\-()]", "").equals(currentNumber.get()) || number.equals(currentNumber.get()))
                                            ) {
                                                continue;
                                            }
                                            JSONObject a = new JSONObject();
                                            a.put("text", cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.BODY)));
                                            a.put("date", Long.parseLong(cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.DATE))) / 1000);
                                            a.put("type", cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                                            a.put("sim", cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID)));
                                            if(number.isEmpty()) continue;
                                            Cursor cur3 = thisContext
                                                    .getContentResolver()
                                                    .query(
                                                            Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                                                    Uri.encode(number)),
                                                            new String[]{ContactsContract.Data.DISPLAY_NAME},
                                                            null,
                                                            null,
                                                            null
                                                    );
                                            cur3.moveToFirst();
                                            if(number.replaceAll("[ \\-()]","").matches("\\+\\d{7,13}"))
                                                number = number.replaceAll("[ \\-()]","");
                                            if(cur3.getCount() > 0) a.put("contact", cur3.getString(0)+"("+number+")");
                                            else a.put("contact", number);
                                            sms.put(a);
                                        }
                                        if(sms.length() == 0) break;
                                        ans.put("SMS", sms);
                                        ans.put("Type", "newSMSs");
                                        writer.println(ans.toString());
                                        writer.flush();
                                        lastSync.set(System.currentTimeMillis() / 1000);
                                        break;
                                }
                            }
                        }
                    } catch (IOException | JSONException | NullPointerException | InterruptedException e) {
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