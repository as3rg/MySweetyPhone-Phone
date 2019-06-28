package Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.json.JSONException;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.stream.Stream;

public class ServerMode {
    private static ArrayList<SessionServer> opened = new ArrayList<>();

    static boolean State = false;

    static public boolean getState(){
        return State;
    }

    static private Context context;

    static private void CreateServer(int i) {
        try {
            SessionServer sessionServer = new SessionServer(i, 0, null, context);
            sessionServer.setOnStop(() -> {
                opened.remove(sessionServer);
                CreateServer(i);
            });
            opened.add(sessionServer);
            sessionServer.Start();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    static public void SetContext(Activity c){
        context = c;
        State = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("serverMode", false);
        if (State)
            Start();
    }

    static public void Start() {
        State = true;
        RewriteState();
        for(int i : SessionServer.allowedTypes){
            CreateServer(i);
        }
    }

    static public void Stop() throws IOException {
        State = false;
        RewriteState();
        for(SessionServer s : opened){
            s.setOnStop(null);
            s.Stop();
        }
    }

    static private void RewriteState() {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .remove("serverMode")
                .putBoolean("serverMode", State)
                .apply();
    }
}
