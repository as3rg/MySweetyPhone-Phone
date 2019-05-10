package com.mysweetyphone.phone;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Calendar;

import Utils.Message;
import Utils.SessionClient;

public class MouseTracker extends Activity implements View.OnTouchListener {

    static public SessionClient sc;
    static final int MESSAGESIZE = 100;
    static String name;

//    public void mousePressed(MouseEvent e)
//    {
//        try {
//            JSONObject msg = new JSONObject();
//            msg.put("Type","mousePressed");
//            msg.put("Key",e.getButton().ordinal());
//            Send(msg.toJSONString().getBytes());
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    public void dragDropped(DragEvent e){
//        MouseEvent me = new MouseEvent(MouseEvent.MOUSE_RELEASED,0,0,0,0, MouseButton.PRIMARY,0,true,true,true,true,true,true,true,true,true,true,null);
//        mouseReleased(me);
//    }
//
//    public void mouseReleased(MouseEvent e)
//    {
//        try{
//            JSONObject msg = new JSONObject();
//            msg.put("Type","mouseReleased");
//            msg.put("Key",e.getButton().ordinal());
//            Send(msg.toJSONString().getBytes());
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    private void mouseMoved(MouseEvent t) {
//        try {
//            JSONObject msg = new JSONObject();
//            msg.put("Type", "mouseMoved");
//            msg.put("X", t.getX());
//            msg.put("Y", t.getY());
//            Send(msg.toJSONString().getBytes());
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    public void mouseWheelMoved(ScrollEvent e){
//        try{
//            JSONObject msg = new JSONObject();
//            msg.put("Type","mouseWheel");
//            double value = -e.getDeltaY()/10;
//            value = value > 0 ? Math.ceil(value) : -Math.ceil(-value);
//            msg.put("value",value);
//            Send(msg.toJSONString().getBytes());
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    public void keyPressed(KeyEvent e) {
//        try {
//            JSONObject msg = new JSONObject();
//            if(e.isAltDown() && e.getCode() == KeyCode.S) {
//                msg.put("Type", "swap");
//                Send(msg.toJSONString().getBytes());
//                sc.socket.close();
//                Thread.sleep(1000);
//                SessionServer ss = new SessionServer(sc.getType(), sc.getPort(), ()->{});
//                Session.sessions.add(ss);
//                Session.sessions.remove(this);
//                ss.Start();
//                s.close();
//            }else {
//                msg.put("Type", "keyPressed");
//                msg.put("value", e.getCode().getCode());
//                Send(msg.toJSONString().getBytes());
//            }
//        } catch (IOException | InterruptedException e1) {
//            e1.printStackTrace();
//        }
//    }
//
//    public void keyReleased(KeyEvent e) {
//        try {
//            JSONObject msg = new JSONObject();
//            msg.put("Type", "keyReleased");
//            msg.put("value", e.getCode().getCode());
//            Send(msg.toJSONString().getBytes());
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }

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

    long DownX,x,DownY,y;
    long lastUpTime = 0;
    long lastDownTime = 0;
    boolean LPressed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        name = PreferenceManager.getDefaultSharedPreferences(this).getString("name", "");
        View content = findViewById(android.R.id.content);
        JSONObject msg2 = new JSONObject();
        try {
            msg2.put("Type", "start");
            msg2.put("Name", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Send(msg2.toString().getBytes());
        content.setOnTouchListener((v,event)->{
            try {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = (int)event.getX();
                        y = (int)event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        JSONObject msg = new JSONObject();
                        msg.put("Type", "mouseMoved");
                        msg.put("Name", name);
                        msg.put("X", (int) event.getX() - x);
                        msg.put("Y", (int) event.getY() - y);
                        Send(msg.toString().getBytes());
                        x = (int)event.getX();
                        y = (int)event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        });
//        content.setOnClickListener(v -> {
//            try{
//                JSONObject msg = new JSONObject();
//                msg.put("Type", "mousePressed");
//                msg.put("Key", 1);
//                Send(msg.toString().getBytes());
//                msg = new JSONObject();
//                msg.put("Type", "mouseReleased");
//                msg.put("Key", 1);
//                Send(msg.toString().getBytes());
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        });
//        content.setOnLongClickListener(v -> {
//            try{
//                JSONObject msg = new JSONObject();
//                msg.put("Type", "mousePressed");
//                msg.put("Key", 3);
//                Send(msg.toString().getBytes());
//                msg = new JSONObject();
//                msg.put("Type", "mouseReleased");
//                msg.put("Key", 3);
//                Send(msg.toString().getBytes());
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            return false;
//        });

    }

    @Override
    public boolean onTouch(View v, final MotionEvent event) {
        new Thread(() -> {
            try {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = (int)event.getX();
                        y = (int)event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1) {
                            x = (int) event.getX() - x;
                            y = (int) event.getY() - y;
                            JSONObject msg = new JSONObject();
                            msg.put("Type", "mouseMoved");
                            msg.put("Name", name);
                            msg.put("X", x);
                            msg.put("Y", y);
                            Send(msg.toString().getBytes());
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }
}


//@Override
    /*protected void onCreate(){

    }

    /*public void Start(final View view){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress address = InetAddress.getByName("192.168.1.39");
                    Socket s = new Socket(address, 6666);
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
                    while (true) {
                        out.println(((Button) view).isPressed());
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t = new Thread(r);
        t.start();
        state.setText(Boolean.toString(t.isAlive()));
    }

    public void Stop(View v){
        t.interrupt();
        state.setText(Boolean.toString(t.isAlive()));
    }*/
