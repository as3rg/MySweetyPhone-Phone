package com.mysweetyphone.phone;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import Utils.ImageFilePath;
import Utils.Message;
import Utils.MessageParser;
import Utils.SessionClient;
import Utils.SessionServer;

public class FileViewer extends AppCompatActivity {

    static public SessionClient sc;
    Thread receiving;
    static final int MESSAGESIZE = 100;
    PrintWriter writer;
    BufferedReader reader;
    String name;
    Set<String> files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer);
        TextView path = findViewById(R.id.pathFILEVIEWER);

        files = new HashSet<>();
        name = PreferenceManager.getDefaultSharedPreferences(this).getString("name", "");

        receiving = new Thread(()-> {
            try {
                sc.setSocket(new Socket(sc.getAddress(), sc.getPort()));
                writer = new PrintWriter(sc.getSocket().getOutputStream());
                reader = new BufferedReader(new InputStreamReader(sc.getSocket().getInputStream()));
                Timer t = new Timer();
                LinearLayout folders = this.findViewById(R.id.foldersFILEVIEWER);
                t.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            JSONObject msg2 = new JSONObject();
                            msg2.put("Type", "start");
                            msg2.put("Name", name);
                            writer.println(msg2.toString());
                            writer.flush();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },0,2000);
                while (true) {
                    String line = reader.readLine();
                    t.cancel();
                    System.out.println(line);
                    JSONObject msg = new JSONObject(line);
                    switch ((String) msg.get("Type")) {
                        case "showDir":
                            JSONArray values = msg.getJSONArray("Inside");
                            files.clear();
                            this.runOnUiThread(() -> {
                                try {
                                    if (msg.getInt("State") == 1) {
                                        Toast toast = Toast.makeText(this,
                                                "Нет доступа", Toast.LENGTH_LONG);
                                        toast.show();
                                        return;
                                    }
                                    folders.removeAllViews();
                                    path.setText(msg.getString("Dir"));
                                    findViewById(R.id.backFILEVIEWER).setVisibility(path.getText().toString().isEmpty() ? View.INVISIBLE : View.VISIBLE);
                                    findViewById(R.id.newDirFILEVIEWER).setVisibility(path.getText().toString().isEmpty() ? View.INVISIBLE : View.VISIBLE);
                                    findViewById(R.id.uploadFileFILEVIEWER).setVisibility(path.getText().toString().isEmpty() ? View.INVISIBLE : View.VISIBLE);
                                    findViewById(R.id.reloadFolderFILEVIEWER).setVisibility(path.getText().toString().isEmpty() ? View.INVISIBLE : View.VISIBLE);
                                    for (int i = 0; i < values.length(); i++) {
                                        TextView folder = new TextView(this);
                                        folder.setText(values.getJSONObject(i).getString("Name"));
                                        files.add(values.getJSONObject(i).getString("Name"));
                                        folder.setPadding(20, 20, 20, 20);
                                        folder.setTextSize(20);
                                        folder.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                                        Drawable d = getDrawable(values.getJSONObject(i).getString("Type").equals("Folder") ? R.drawable.ic_file_viewer_folder : R.drawable.ic_file_viewer_file);
                                        d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                                        folder.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
                                        folders.addView(folder);
                                        final int i2 = i;
                                        if(values.getJSONObject(i2).getString("Type").equals("Folder"))
                                            folder.setOnClickListener(v -> new Thread(() -> {
                                                try {
                                                    JSONObject msg3 = new JSONObject();
                                                    msg3.put("Type", "showDir");
                                                    msg3.put("Name", name);
                                                    msg3.put("Dir", new File(msg.getString("Dir"), values.getJSONObject(i2).getString("Name")).toPath());
                                                    writer.println(msg3.toString());
                                                    writer.flush();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }).start());
                                        else folder.setOnClickListener(v -> new Thread(() -> {
                                            try {
                                                File out = new File(Environment.getExternalStorageDirectory() + "/MySweetyPhone");
                                                out.mkdirs();
                                                File out2 = new File(out, (String) values.getJSONObject(i2).getString("Name"));
                                                out2.createNewFile();

                                                ServerSocket ss = new ServerSocket(0);
                                                JSONObject msg2 = new JSONObject();
                                                msg2.put("Type", "downloadFile");
                                                msg2.put("Name", name);
                                                msg2.put("FileName", values.getJSONObject(i2).getString("Name"));
                                                msg2.put("FileSocketPort", ss.getLocalPort());
                                                msg2.put("Dir", ((TextView) findViewById(R.id.pathFILEVIEWER)).getText().toString());
                                                writer.println(msg2.toString());
                                                writer.flush();
                                                Socket socket = ss.accept();
                                                DataInputStream filein = new DataInputStream(socket.getInputStream());
                                                FileOutputStream fileout = new FileOutputStream(out2);
                                                IOUtils.copy(filein, fileout);
                                                fileout.close();
                                                socket.close();
                                                runOnUiThread(()->{
                                                    Toast toast = Toast.makeText(this,
                                                            "Файл "+out2.getName()+" скачен", Toast.LENGTH_LONG);
                                                    toast.show();
                                                });
                                            } catch (JSONException | IOException e) {
                                                e.printStackTrace();
                                            }
                                        }).start());
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                            break;
                        case "newDirAnswer":
                            runOnUiThread(()-> {
                                try {
                                    TextView folder = new TextView(this);
                                    folder.setText(msg.getString("DirName"));
                                    files.add(msg.getString("DirName"));
                                    folder.setPadding(20, 20, 20, 20);
                                    folder.setTextSize(20);
                                    folder.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                                    Drawable d = getDrawable(R.drawable.ic_file_viewer_folder);
                                    d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                                    folder.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
                                    folders.addView(folder);
                                    folder.setOnClickListener(v -> {
                                        new Thread(() -> {
                                            try {
                                                JSONObject msg3 = new JSONObject();
                                                msg3.put("Type", "showDir");
                                                msg3.put("Name", name);
                                                msg3.put("Dir", new File(msg.getString("Dir"), msg.getString("DirName")).toPath());
                                                writer.println(msg3.toString());
                                                writer.flush();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }).start();
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                            break;
                    }
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        });
        receiving.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        new Thread(()-> {
            try {
                receiving.interrupt();
                JSONObject msg = new JSONObject();
                msg.put("Type", "finish");
                msg.put("Name", name);
                writer.println(msg.toString());
                writer.flush();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void back(View v){
        new Thread(() -> {
            try {
                JSONObject msg2 = new JSONObject();
                msg2.put("Type", "back");
                msg2.put("Name", name);
                msg2.put("Dir", ((TextView)findViewById(R.id.pathFILEVIEWER)).getText().toString());
                writer.println(msg2.toString());
                writer.flush();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void newFolder(View v){
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle("Имя папки")
                .setMessage("Введите имя папки")
                .setView(input)
                .setPositiveButton("Создать", null)
                .setNegativeButton("Отмена", (dialog, which) -> {})
                .create();
        alert.setOnShowListener(dialog -> {
            alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                if(
                        input.getText().toString().contains("\\")
                        || input.getText().toString().contains("/")
                        || input.getText().toString().contains(":")
                        || input.getText().toString().contains("*")
                        || input.getText().toString().contains("?")
                        || input.getText().toString().contains("\"")
                        || input.getText().toString().contains("<")
                        || input.getText().toString().contains(">")
                        || input.getText().toString().contains("|")
                ) {
                    alert.setMessage("Имя содержит недопустимые символы");
                }else if(files.contains(input.getText().toString())) {
                    alert.setMessage("Такая папка уже существует");
                }else if(input.getText().toString().isEmpty()) {
                    alert.setMessage("Имя файла не может быть пустым");
                }else {
                    new Thread(() -> {
                        try {
                            JSONObject msg2 = new JSONObject();
                            msg2.put("Type", "newDir");
                            msg2.put("DirName", input.getText().toString());
                            msg2.put("Name", name);
                            msg2.put("Dir", ((TextView)findViewById(R.id.pathFILEVIEWER)).getText().toString());
                            writer.println(msg2.toString());
                            writer.flush();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    alert.dismiss();
                }
            });
        });
        alert.show();
    }

    public void uploadFile(View v){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent,43);
    }

    public void reloadFolder(View v){
        new Thread(() -> {
            try {
                JSONObject msg3 = new JSONObject();
                msg3.put("Type", "showDir");
                msg3.put("Name", name);
                msg3.put("Dir", ((TextView)findViewById(R.id.pathFILEVIEWER)).getText());
                writer.println(msg3.toString());
                writer.flush();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            File file = new File(ImageFilePath.getPath(this, data.getData()));
            new Thread(() -> {
                try {
                    ServerSocket ss = new ServerSocket(0);
                    JSONObject msg2 = new JSONObject();
                    msg2.put("Type", "uploadFile");
                    msg2.put("Name", name);
                    msg2.put("FileName", file.getName());
                    msg2.put("FileSocketPort", ss.getLocalPort());
                    msg2.put("Dir", ((TextView) findViewById(R.id.pathFILEVIEWER)).getText().toString());
                    writer.println(msg2.toString());
                    writer.flush();
                    Socket socket = ss.accept();
                    DataOutputStream fileout = new DataOutputStream(socket.getOutputStream());
                    FileInputStream filein = new FileInputStream(file);
                    IOUtils.copy(filein, fileout);
                    fileout.flush();
                    filein.close();
                    socket.close();
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}