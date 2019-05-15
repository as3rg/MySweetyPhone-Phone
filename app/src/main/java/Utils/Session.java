package Utils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;

public abstract class Session{
    protected InetAddress address;
    protected DatagramSocket socket;
    protected int port;
    protected Type type;
    protected Thread t;
    protected Timer broadcasting;
    static int BroadCastingPort = 9000;

    public enum Type{
        MOUSE,
        FILEVIEW
    }

    public static ArrayList<Session> sessions;

    static {
        sessions=new ArrayList<>(10);
    }

    public Session(){
        sessions.add(this);
    }

    public void Start(){
        t.start();
    }

    public void Stop() throws IOException {
        broadcasting.cancel();
        t.interrupt();
        if(socket!=null && !socket.isClosed()) socket.close();
    }

    public int getPort() {
        return port;
    }

    public Type getType() {
        return type;
    }

    public InetAddress getAddress() {
        return address;
    }

    public abstract boolean isServer();

    public DatagramSocket getSocket() {
        return socket;
    }
}