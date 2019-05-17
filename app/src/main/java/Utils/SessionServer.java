package Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.TimerTask;

public class SessionServer extends Session{
    Thread onStop;
    MessageParser messageParser;
    ServerSocket ss;

    public SessionServer(Type type, int Port, Runnable doOnStopSession) throws IOException, JSONException {
        onStop = new Thread(doOnStopSession);
        messageParser = new MessageParser();
        JSONObject message = new JSONObject();
        switch (type){
            case MOUSE:
                Dsocket = new DatagramSocket(Port);
                Dsocket.setBroadcast(true);
                port = Dsocket.getLocalPort();
                break;
            case FILEVIEW:
                ss = new ServerSocket(Port);
                port = ss.getLocalPort();
        }
        message.put("port", port);
        message.put("type", type.ordinal());
        byte[] buf2 = String.format("%-30s", message.toString()).getBytes();
        DatagramSocket s = new DatagramSocket();
        s.setBroadcast(true);
        DatagramPacket packet = new DatagramPacket(buf2, buf2.length, Inet4Address.getByName("255.255.255.255"), BroadCastingPort);

        broadcasting = new Timer();
        TimerTask broadcastingTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    s.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        broadcasting.schedule(broadcastingTask, 2000, 2000);

        switch (type) {
//            case MOUSE:
//                t = new Thread(() -> {
//                    try {
//                        socket.setBroadcast(true);
//                        Robot r = new Robot();
//                        DatagramPacket p;
//                        while (!socket.isClosed()) {
//                            Message m = null;
//                            int head = -1;
//                            p = null;
//                            do{
//                                byte[] buf = new byte[Message.getMessageSize(MouseTracker.MESSAGESIZE)];
//                                p = new DatagramPacket(buf, buf.length);
//                                try {
//                                    socket.receive(p);
//                                    if(onStop != null){
//                                        Platform.runLater(onStop);
//                                        onStop = null;
//                                    }
//                                    m = new Message(p.getData());
//                                    messageParser.messageMap.put(m.getId(), m);
//                                    if (head == -1)
//                                        head = m.getId();
//                                } catch (SocketException ignored){
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }while (!socket.isClosed() && (m == null || m.getNext() != -1));
//                            if(messageParser.messageMap.get(head) == null) continue;
//                            String msgString = new String(messageParser.parse(head));
//                            JSONObject msg = (JSONObject) JSONValue.parse(msgString);
//                            if(msg!=null)
//                                switch ((String)msg.get("Type")){
//                                    case "mouseMoved":
//                                        r.mouseMove(((Double) msg.get("X")).intValue(), ((Double) msg.get("Y")).intValue());
//                                        break;
//                                    case "mouseReleased":
//                                        r.mouseRelease(InputEvent.getMaskForButton(((Long) msg.get("Key")).intValue()));
//                                        break;
//                                    case "mousePressed":
//                                        r.mousePress(InputEvent.getMaskForButton(((Long) msg.get("Key")).intValue()));
//                                        break;
//                                    case "mouseWheel":
//                                        r.mouseWheel(((Double)msg.get("value")).intValue());
//                                        break;
//                                    case "keyReleased":
//                                        r.keyRelease(((Long)msg.get("value")).intValue());
//                                        break;
//                                    case "keyPressed":
//                                        r.keyPress(((Long)msg.get("value")).intValue());
//                                        break;
//                                    case "swap":
//                                        SessionClient sc = new SessionClient(p.getAddress(),port,type);
//                                        socket.close();
//                                        Session.sessions.add(sc);
//                                        Session.sessions.remove(this);
//                                        sc.Start();
//                                        return;
//                                    case "finish":
//                                        r.keyRelease(KeyEvent.VK_ALT);
//                                        Stop();
//                                        return;
//                                    default:
//                                        System.out.println(msgString);
//                                }
//
//                        }
//                    } catch (AWTException | IOException e) {
//                        e.printStackTrace();
//                    }
//
//                });
//                break;
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