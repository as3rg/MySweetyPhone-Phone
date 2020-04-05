package com.mysweetyphone.phone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Pair;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import Utils.Session;
import Utils.SessionClient;

public class MouseTracker extends AppCompatActivity {

    public static abstract class CustomTimerTask extends TimerTask{
        Click click;

        CustomTimerTask(Click click){
            this.click = click;
        }

        public abstract void action();

        @Override
        public void run() {
            action();
            click.t.cancel();
        }
    }

    static class Click{

        int x,y;
        int type;
        long time;
        Timer t;
        Click(int x, int y, long time, int type){
            this.x = x;
            this.y = y;
            this.time = time;
            this.type = type;
            t = new Timer();
        }

        void Later(CustomTimerTask tt, int delay){
            t.schedule(tt, delay);
        }

        void Cancel(){
            if(t!=null) t.cancel();
        }
    }

    static public SessionClient sc;
    static String name;
    static int code;
    Switch win, alt, shift, ctrl;
    EditText inputView;
    Click singleClick;
    Pair<Click, Click> doubleClick;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(sc.getType() == Session.KEYBOARD)
            setContentView(R.layout.activity_mouse_tracker_phone);
        else
            setContentView(R.layout.activity_mouse_tracker);
        Toolbar toolbar = findViewById(R.id.toolbarMOUSETRACKER);
        setSupportActionBar(toolbar);

        name = PreferenceManager.getDefaultSharedPreferences(this).getString("name", "");
        code = PreferenceManager.getDefaultSharedPreferences(this).getInt("code", 0);

        View content = findViewById(android.R.id.content);
        JSONObject msg2 = new JSONObject();
        try {
            msg2.put("Type", "start");
            msg2.put("Name", name);
            msg2.put("Code", code % sc.getMode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Send(msg2.toString().getBytes());

        win = findViewById(R.id.winMOUSETRACKER);
        alt = findViewById(R.id.altMOUSETRACKER);
        ctrl = findViewById(R.id.ctrlMOUSETRACKER);
        shift = findViewById(R.id.shiftMOUSETRACKER);
        ImageButton keyboardButton = findViewById(R.id.keyboardMOUSETRACKER);
        ImageButton makeScreenshotButton = findViewById(R.id.makeScreenshotMOUSETRACKER);
        HorizontalScrollView extraButtons = findViewById(R.id.extra1MOUSETRACKER);
        HorizontalScrollView extra2Buttons = findViewById(R.id.extra2MOUSETRACKER);
        TableLayout TableExtraButtons = findViewById(R.id.extraButtonsMOUSETRACKER);
        inputView = findViewById(R.id.inputMOUSETRACKER);
        inputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().isEmpty()) return;
                try {
                    JSONObject msg = new JSONObject();
                    msg.put("Name", name);
                    msg.put("Type", "keysTyped");
                    if(!(sc.getType() == Session.KEYBOARD) && (win.isChecked() || alt.isChecked() || shift.isChecked() || ctrl.isChecked())){
                        msg.put("Subtype", "hotkey");
                        for(char c : s.toString().toCharArray()){
                            msg.put("value", Character.toString(c));
                            Send(msg.toString().getBytes());
                        }
                    } else {
                        msg.put("Subtype", "none");
                        msg.put("value", s);
                        Send(msg.toString().getBytes());
                    }
                    inputView.setText("");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        inputView.setKeyListener(new KeyListener() {
            @Override
            public int getInputType() {
                return InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            }

            @Override
            public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
                return true;
            }

            @Override
            public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
                try {
                    JSONObject msg = new JSONObject();
                    msg.put("Type", "keyClicked");
                    msg.put("value", AndroidToAwt(keyCode));
                    msg.put("Name", name);
                    Send(msg.toString().getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean onKeyOther(View view, Editable text, KeyEvent event) {
                return true;
            }

            @Override
            public void clearMetaKeyState(View view, Editable content, int states) {

            }
        });

        if(sc.getType() == Session.KEYBOARD) return;

        content.setOnTouchListener(this::onTouchMOUSE);
        Spinner type = findViewById(R.id.typeMOUSETRACKER);
        type.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, new String[]{
                getResources().getString(R.string.mouseMOUSETRACKER),
                getResources().getString(R.string.keyboardMOUSETRACKER),
                getResources().getString(R.string.pen_tabletMOUSETRACKER)
        }));

        MouseTracker thisActivity = this;

        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            final String mouse = getResources().getString(R.string.mouseMOUSETRACKER);
            final String keyboard = getResources().getString(R.string.keyboardMOUSETRACKER);
            final String pen_tablet = getResources().getString(R.string.pen_tabletMOUSETRACKER);

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = parent.getSelectedItem().toString();
                if(Objects.equals(value, mouse)) {
                    inputView.setVisibility(View.GONE);
                    extraButtons.setVisibility(View.GONE);
                    extra2Buttons.setVisibility(View.GONE);
                    TableExtraButtons.setVisibility(View.GONE);
                    keyboardButton.setVisibility(View.GONE);
                    makeScreenshotButton.setVisibility(View.GONE);
                    content.setOnTouchListener(thisActivity::onTouchMOUSE);
                }else if(value.equals(keyboard)) {
                    inputView.setVisibility(View.VISIBLE);
                    TableExtraButtons.setVisibility(View.VISIBLE);
                    extraButtons.setVisibility(View.VISIBLE);
                    extra2Buttons.setVisibility(View.VISIBLE);
                    keyboardButton.setVisibility(View.VISIBLE);
                    makeScreenshotButton.setVisibility(View.VISIBLE);
                    thisActivity.openKeyboard(null);
                    content.setOnTouchListener((v,e)->false);
                }else if(value.equals(pen_tablet)) {
                    inputView.setVisibility(View.GONE);
                    TableExtraButtons.setVisibility(View.GONE);
                    keyboardButton.setVisibility(View.GONE);
                    makeScreenshotButton.setVisibility(View.GONE);
                    content.setOnTouchListener(thisActivity::onTouchPENTABLET);
                    extraButtons.setVisibility(View.GONE);
                    extra2Buttons.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void Send(byte[] b) {
        new Thread(()-> {
            try {
                sc.getDatagramSocket().send(new DatagramPacket(b, b.length, sc.getAddress(), sc.getPort()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void Screenshot(View v){
        new Thread(() -> {
            try {
                ServerSocket ss = new ServerSocket(0);
                JSONObject msg2 = new JSONObject();
                msg2.put("Type", "makeScreenshot");
                msg2.put("Port", ss.getLocalPort());
                msg2.put("Name", name);
                msg2.put("Code", code % sc.getMode());
                Send(msg2.toString().getBytes());
                ss.setSoTimeout(10000);
                Socket socket = ss.accept();


                File out = new File(Environment.getExternalStorageDirectory() + "/MySweetyPhone");
                out.mkdirs();
                String fileName = "Screenshot_"+new SimpleDateFormat("HH:mm:ss_dd.MM.yyyy").format(System.currentTimeMillis())+".png";
                FileOutputStream fileout = new FileOutputStream(new File(out, fileName));
                BitmapFactory.decodeStream(socket.getInputStream()).compress(Bitmap.CompressFormat.PNG,100, fileout);

                fileout.close();
                socket.close();
                ss.close();
                runOnUiThread(() -> {
                    Toast toast = Toast.makeText(this,
                            "Скриншот сохранен в файл \""+fileName+"\"", Toast.LENGTH_LONG);
                    toast.show();
                });
            } catch (IOException | JSONException e2) {
                e2.printStackTrace();
            }
        }).start();
    }

    public boolean onTouchMOUSE(View v, final MotionEvent event) {
        final int LInterval = 200, RInterval = 500;
        try {
            if(event.getPointerCount() == 1) {
                Click nextClick = new Click((int) event.getX(), (int) event.getY(), System.currentTimeMillis(), event.getAction());
                if(singleClick == null)
                    event.setAction(MotionEvent.ACTION_DOWN);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_DOWN:
                        if (singleClick != null && singleClick.type == MotionEvent.ACTION_UP && nextClick.time - singleClick.time <= LInterval) {
                            singleClick.Cancel();
                        }

                        nextClick.Later(new CustomTimerTask(nextClick) {
                            @Override
                            public void action() {
                                try {
                                    if(nextClick != singleClick) return;
                                    JSONObject msg = new JSONObject();
                                    msg.put("Type", "mousePressed");
                                    msg.put("Name", name);
                                    msg.put("Key", 3);
                                    if(nextClick != singleClick) return;
                                    Send(msg.toString().getBytes());
                                    msg.put("Type", "mouseReleased");
                                    Send(msg.toString().getBytes());

                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    v.vibrate(50);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, RInterval);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        singleClick.Cancel();
                        JSONObject msg = new JSONObject();
                        msg.put("Type", "mouseMoved");
                        msg.put("Name", name);
                        msg.put("X", nextClick.x - singleClick.x);
                        msg.put("Y", nextClick.y - singleClick.y);
                        Send(msg.toString().getBytes());
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_UP:
                        singleClick.Cancel();
                        if (singleClick.type == MotionEvent.ACTION_DOWN && nextClick.time - singleClick.time <= LInterval) {
                            msg = new JSONObject();
                            msg.put("Type", "mousePressed");
                            msg.put("Name", name);
                            msg.put("Key", 1);
                            Send(msg.toString().getBytes());
                            nextClick.Later(new CustomTimerTask(nextClick) {
                                @Override
                                public void action() {
                                    try {
                                        JSONObject msg = new JSONObject();
                                        msg.put("Type", "mouseReleased");
                                        msg.put("Name", name);
                                        msg.put("Key", 1);
                                        Send(msg.toString().getBytes());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, LInterval);
                        } else {
                            msg = new JSONObject();
                            msg.put("Type", "mouseReleased");
                            msg.put("Name", name);
                            msg.put("Key", 1);
                            Send(msg.toString().getBytes());
                        }
                        break;
                    default:
                        break;
                }
                singleClick = nextClick;
            }else if(event.getPointerCount() == 2 && Arrays.asList(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP).contains(event.getAction())) {
                if(singleClick != null) {
                    singleClick.Cancel();
                    singleClick = null;
                }
                Click nextClick1 = new Click((int) event.getX(0), (int) event.getY(0), System.currentTimeMillis(), event.getAction()),
                    nextClick2 = new Click((int) event.getX(1), (int) event.getY(1), System.currentTimeMillis(), event.getAction());
                if(doubleClick != null){
                    int range1 = nextClick1.y - doubleClick.first.y,
                        range2 = nextClick2.y - doubleClick.second.y;
                    if (range1 != 0 && range2 != 0 && range1 / Math.abs(range1) == range2 / Math.abs(range2)) {
                        JSONObject msg = new JSONObject();
                        msg.put("Type", "mouseWheel");
                        msg.put("Name", name);
                        msg.put("value", range1 / Math.abs(range1));
                        Send(msg.toString().getBytes());
                    }
                }
                doubleClick = new Pair<>(nextClick1, nextClick2);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean onTouchPENTABLET(View v, final MotionEvent event) {
        try {
            Display display = getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    JSONObject msg = new JSONObject();
                    msg.put("Type", "startDrawing");
                    msg.put("Name", name);
                    msg.put("X", event.getX() / width);
                    msg.put("Y", event.getY() / height);
                    Send(msg.toString().getBytes());
                    break;
                case MotionEvent.ACTION_MOVE:
                    msg = new JSONObject();
                    msg.put("Type", "draw");
                    msg.put("Name", name);
                    msg.put("X", event.getX() / width);
                    msg.put("Y", event.getY() / height);
                    Send(msg.toString().getBytes());
                    break;
                case MotionEvent.ACTION_UP:
                    msg = new JSONObject();
                    msg.put("Type", "mouseReleased");
                    msg.put("Key", 1);
                    msg.put("Name", name);
                    Send(msg.toString().getBytes());
                default:
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            JSONObject msg = new JSONObject();
            msg.put("Type", "finish");
            msg.put("Name", name);
            Send(msg.toString().getBytes());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void openKeyboard(View v) {
        EditText et = findViewById(R.id.inputMOUSETRACKER);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInputFromWindow(et.getWindowToken(),InputMethodManager.SHOW_FORCED, 0);
        et.requestFocus();
    }

    static int ViewToButtonId(View v){
        switch(v.getId()){
            case R.id.escapeMOUSETRACKER:
                return PCKeys.Escape;
            case R.id.tabMOUSETRACKER:
                return PCKeys.Tab;
            case R.id.capslockMOUSETRACKER:
                return PCKeys.CapsLock;
            case R.id.leftMOUSETRACKER:
                return PCKeys.Left;
            case R.id.downMOUSETRACKER:
                return PCKeys.Down;
            case R.id.upMOUSETRACKER:
                return PCKeys.Up;
            case R.id.rightMOUSETRACKER:
                return PCKeys.Right;
            case R.id.delMOUSETRACKER:
                return PCKeys.Delete;
            case R.id.numlockMOUSETRACKER:
                return PCKeys.NumLock;
            case R.id.scrolllockMOUSETRACKER:
                return PCKeys.Scroll;
            case R.id.homeMOUSETRACKER:
                return PCKeys.Home;
            case R.id.endMOUSETRACKER:
                return PCKeys.End;
            case R.id.pageupMOUSETRACKER:
                return PCKeys.PageUp;
            case R.id.pagedownMOUSETRACKER:
                return PCKeys.PageDown;
            case R.id.insertMOUSETRACKER:
                return PCKeys.Insert;
            case R.id.printscreenMOUSETRACKER:
                return PCKeys.PrintScreen;
            case R.id.ctrlMOUSETRACKER:
                return PCKeys.LeftCtrl;
            case R.id.shiftMOUSETRACKER:
                return PCKeys.LeftShift;
            case R.id.altMOUSETRACKER:
                return PCKeys.LeftAlt;
            case R.id.winMOUSETRACKER:
                return PCKeys.LWin;
            case R.id.enterMOUSETRACKER:
                return PCKeys.Enter;
            case R.id.F1MOUSETRACKER:
                return PCKeys.F1;
            case R.id.F2MOUSETRACKER:
                return PCKeys.F2;
            case R.id.F3MOUSETRACKER:
                return PCKeys.F3;
            case R.id.F4MOUSETRACKER:
                return PCKeys.F4;
            case R.id.F5MOUSETRACKER:
                return PCKeys.F5;
            case R.id.F6MOUSETRACKER:
                return PCKeys.F6;
            case R.id.F7MOUSETRACKER:
                return PCKeys.F7;
            case R.id.F8MOUSETRACKER:
                return PCKeys.F8;
            case R.id.F9MOUSETRACKER:
                return PCKeys.F9;
            case R.id.F10MOUSETRACKER:
                return PCKeys.F10;
            case R.id.F11MOUSETRACKER:
                return PCKeys.F11;
            case R.id.F12MOUSETRACKER:
                return PCKeys.F12;
            default:
                return -1;
        }
    }

    public void sendExtraButton(View v) throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("Name", name);
        msg.put("value", ViewToButtonId(v));
        msg.put("Type", "keyClicked");
        Send(msg.toString().getBytes());
    }

    public void switchExtraButton(View v) throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("Name", name);
        msg.put("value", ViewToButtonId(v));
        if(((Switch)v).isChecked())
            msg.put("Type", "keyPressed");
        else
            msg.put("Type", "keyReleased");
        Send(msg.toString().getBytes());
    }

    public static int AwtToAndroid(int e){
        switch (e){
            case PCKeys.Back: return KeyEvent.KEYCODE_DEL;
            case PCKeys.Tab: return KeyEvent.KEYCODE_TAB ;
            case PCKeys.Clear: return KeyEvent.KEYCODE_CLEAR ;
            case PCKeys.Enter: return KeyEvent.KEYCODE_ENTER ;
            case PCKeys.Pause: return KeyEvent.KEYCODE_MEDIA_PAUSE ;
            case PCKeys.CapsLock: return KeyEvent.KEYCODE_CAPS_LOCK ;
            case PCKeys.Escape: return KeyEvent.KEYCODE_ESCAPE ;
            case PCKeys.Space: return KeyEvent.KEYCODE_SPACE ;
            case PCKeys.PageUp: return KeyEvent.KEYCODE_PAGE_UP ;
            case PCKeys.PageDown: return KeyEvent.KEYCODE_PAGE_DOWN ;
            case PCKeys.End: return KeyEvent.KEYCODE_MOVE_END ;
            case PCKeys.Home: return KeyEvent.KEYCODE_MOVE_HOME ;
            case PCKeys.Left: return KeyEvent.KEYCODE_DPAD_LEFT ;
            case PCKeys.Up: return KeyEvent.KEYCODE_DPAD_UP ;
            case PCKeys.Right: return KeyEvent.KEYCODE_DPAD_RIGHT ;
            case PCKeys.Down: return KeyEvent.KEYCODE_DPAD_DOWN ;
            case PCKeys.Select: return KeyEvent.KEYCODE_BUTTON_SELECT ;
            case PCKeys.PrintScreen: return KeyEvent.KEYCODE_SYSRQ ;
            case PCKeys.Insert: return KeyEvent.KEYCODE_INSERT ;
            case PCKeys.Delete: return KeyEvent.KEYCODE_FORWARD_DEL ;
            case PCKeys.Help: return KeyEvent.KEYCODE_HELP ;
            case PCKeys.D0: return KeyEvent.KEYCODE_0 ;
            case PCKeys.D1: return KeyEvent.KEYCODE_1 ;
            case PCKeys.D2: return KeyEvent.KEYCODE_2 ;
            case PCKeys.D3: return KeyEvent.KEYCODE_3 ;
            case PCKeys.D4: return KeyEvent.KEYCODE_4 ;
            case PCKeys.D5: return KeyEvent.KEYCODE_5 ;
            case PCKeys.D6: return KeyEvent.KEYCODE_6 ;
            case PCKeys.D7: return KeyEvent.KEYCODE_7 ;
            case PCKeys.D8: return KeyEvent.KEYCODE_8 ;
            case PCKeys.D9: return KeyEvent.KEYCODE_9 ;
            case PCKeys.A: return KeyEvent.KEYCODE_A ;
            case PCKeys.B: return KeyEvent.KEYCODE_B ;
            case PCKeys.C: return KeyEvent.KEYCODE_C ;
            case PCKeys.D: return KeyEvent.KEYCODE_D ;
            case PCKeys.E: return KeyEvent.KEYCODE_E ;
            case PCKeys.F: return KeyEvent.KEYCODE_F ;
            case PCKeys.G: return KeyEvent.KEYCODE_G ;
            case PCKeys.H: return KeyEvent.KEYCODE_H ;
            case PCKeys.I: return KeyEvent.KEYCODE_I ;
            case PCKeys.J: return KeyEvent.KEYCODE_J ;
            case PCKeys.K: return KeyEvent.KEYCODE_K ;
            case PCKeys.L: return KeyEvent.KEYCODE_L ;
            case PCKeys.M: return KeyEvent.KEYCODE_M ;
            case PCKeys.N: return KeyEvent.KEYCODE_N ;
            case PCKeys.O: return KeyEvent.KEYCODE_O ;
            case PCKeys.P: return KeyEvent.KEYCODE_P ;
            case PCKeys.Q: return KeyEvent.KEYCODE_Q ;
            case PCKeys.R: return KeyEvent.KEYCODE_R ;
            case PCKeys.S: return KeyEvent.KEYCODE_S ;
            case PCKeys.T: return KeyEvent.KEYCODE_T ;
            case PCKeys.U: return KeyEvent.KEYCODE_U ;
            case PCKeys.V: return KeyEvent.KEYCODE_V ;
            case PCKeys.W: return KeyEvent.KEYCODE_W ;
            case PCKeys.X: return KeyEvent.KEYCODE_X ;
            case PCKeys.Y: return KeyEvent.KEYCODE_Y ;
            case PCKeys.Z: return KeyEvent.KEYCODE_Z ;
            case PCKeys.LWin: return KeyEvent.KEYCODE_MENU ;
            case PCKeys.RWin: return KeyEvent.KEYCODE_MENU ;
            case PCKeys.Apps: return KeyEvent.KEYCODE_ALL_APPS ;
            case PCKeys.Sleep: return KeyEvent.KEYCODE_SLEEP ;
            case PCKeys.NumPad0: return KeyEvent.KEYCODE_NUMPAD_0 ;
            case PCKeys.NumPad1: return KeyEvent.KEYCODE_NUMPAD_1 ;
            case PCKeys.NumPad2: return KeyEvent.KEYCODE_NUMPAD_2 ;
            case PCKeys.NumPad3: return KeyEvent.KEYCODE_NUMPAD_3 ;
            case PCKeys.NumPad4: return KeyEvent.KEYCODE_NUMPAD_4 ;
            case PCKeys.NumPad5: return KeyEvent.KEYCODE_NUMPAD_5 ;
            case PCKeys.NumPad6: return KeyEvent.KEYCODE_NUMPAD_6 ;
            case PCKeys.NumPad7: return KeyEvent.KEYCODE_NUMPAD_7 ;
            case PCKeys.NumPad8: return KeyEvent.KEYCODE_NUMPAD_8 ;
            case PCKeys.NumPad9: return KeyEvent.KEYCODE_NUMPAD_9 ;
            case PCKeys.Multiply: return KeyEvent.KEYCODE_STAR ;
            case PCKeys.Add: return KeyEvent.KEYCODE_PLUS ;
            case PCKeys.Separator: return KeyEvent.KEYCODE_NUMPAD_COMMA ;
            case PCKeys.Subtract: return KeyEvent.KEYCODE_MINUS ;
            case PCKeys.Decimal: return KeyEvent.KEYCODE_NUMPAD_DOT ;
            case PCKeys.Divide: return KeyEvent.KEYCODE_SLASH ;
            case PCKeys.F1: return KeyEvent.KEYCODE_F1 ;
            case PCKeys.F2: return KeyEvent.KEYCODE_F2 ;
            case PCKeys.F3: return KeyEvent.KEYCODE_F3 ;
            case PCKeys.F4: return KeyEvent.KEYCODE_F4 ;
            case PCKeys.F5: return KeyEvent.KEYCODE_F5 ;
            case PCKeys.F6: return KeyEvent.KEYCODE_F6 ;
            case PCKeys.F7: return KeyEvent.KEYCODE_F7 ;
            case PCKeys.F8: return KeyEvent.KEYCODE_F8 ;
            case PCKeys.F9: return KeyEvent.KEYCODE_F9 ;
            case PCKeys.F10: return KeyEvent.KEYCODE_F10 ;
            case PCKeys.F11: return KeyEvent.KEYCODE_F11 ;
            case PCKeys.F12: return KeyEvent.KEYCODE_F12 ;
            case PCKeys.NumLock: return KeyEvent.KEYCODE_NUM_LOCK ;
            case PCKeys.Scroll: return KeyEvent.KEYCODE_SCROLL_LOCK ;
            case PCKeys.LeftShift: return KeyEvent.KEYCODE_SHIFT_LEFT ;
            case PCKeys.RightShift: return KeyEvent.KEYCODE_SHIFT_RIGHT ;
            case PCKeys.LeftCtrl: return KeyEvent.KEYCODE_CTRL_LEFT ;
            case PCKeys.RightCtrl: return KeyEvent.KEYCODE_CTRL_RIGHT ;
            case PCKeys.LeftAlt: return KeyEvent.KEYCODE_ALT_LEFT ;
            case PCKeys.RightAlt: return KeyEvent.KEYCODE_ALT_RIGHT ;
            case PCKeys.VolumeMute: return KeyEvent.KEYCODE_VOLUME_MUTE ;
            case PCKeys.VolumeDown: return KeyEvent.KEYCODE_VOLUME_DOWN ;
            case PCKeys.VolumeUp: return KeyEvent.KEYCODE_VOLUME_UP ;
            case PCKeys.MediaNextTrack: return KeyEvent.KEYCODE_MEDIA_NEXT ;
            case PCKeys.MediaPreviousTrack: return KeyEvent.KEYCODE_MEDIA_PREVIOUS ;
            case PCKeys.MediaStop: return KeyEvent.KEYCODE_MEDIA_STOP ;
            case PCKeys.MediaPlayPause: return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ;
            case PCKeys.SelectMedia: return KeyEvent.KEYCODE_BUTTON_SELECT ;
            case PCKeys.OemPlus: return KeyEvent.KEYCODE_PLUS ;
            case PCKeys.OemComma: return KeyEvent.KEYCODE_COMMA ;
            case PCKeys.OemMinus: return KeyEvent.KEYCODE_MINUS ;
            case PCKeys.OemPeriod: return KeyEvent.KEYCODE_PERIOD ;
            case PCKeys.System: return KeyEvent.KEYCODE_MENU ;
            case PCKeys.OemBackTab: return KeyEvent.KEYCODE_TAB ;
            case PCKeys.Play: return KeyEvent.KEYCODE_MEDIA_PLAY ;
            case PCKeys.Zoom: return KeyEvent.KEYCODE_ZOOM_IN;
            case PCKeys.NoName: return KeyEvent.KEYCODE_UNKNOWN ;
            case PCKeys.OemTilde: return KeyEvent.KEYCODE_GRAVE;
            case PCKeys.OemClear: return KeyEvent.KEYCODE_CLEAR ;
            case PCKeys.OemOpenBrackets: return KeyEvent.KEYCODE_LEFT_BRACKET;
            case PCKeys.OemCloseBrackets: return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case PCKeys.OemSemicolon: return KeyEvent.KEYCODE_SEMICOLON;
            case PCKeys.OemQuotes: return KeyEvent.KEYCODE_APOSTROPHE;
            case PCKeys.OemQuestion: return KeyEvent.KEYCODE_SLASH;
            case PCKeys.OemBackslash: return KeyEvent.KEYCODE_BACKSLASH;
        }
        return -1;
    }

    public static int AndroidToAwt(int e){
        switch (e){
            case KeyEvent.KEYCODE_HOME: return PCKeys.BrowserHome;
            case KeyEvent.KEYCODE_BACK: return PCKeys.BrowserBack;
            case KeyEvent.KEYCODE_VOLUME_MUTE: return PCKeys.VolumeMute;
            case KeyEvent.KEYCODE_VOLUME_DOWN: return PCKeys.VolumeDown;
            case KeyEvent.KEYCODE_VOLUME_UP: return PCKeys.VolumeUp;
            case KeyEvent.KEYCODE_0: return PCKeys.D0;
            case KeyEvent.KEYCODE_1: return PCKeys.D1;
            case KeyEvent.KEYCODE_2: return PCKeys.D2;
            case KeyEvent.KEYCODE_3: return PCKeys.D3;
            case KeyEvent.KEYCODE_4: return PCKeys.D4;
            case KeyEvent.KEYCODE_5: return PCKeys.D5;
            case KeyEvent.KEYCODE_6: return PCKeys.D6;
            case KeyEvent.KEYCODE_7: return PCKeys.D7;
            case KeyEvent.KEYCODE_8: return PCKeys.D8;
            case KeyEvent.KEYCODE_9: return PCKeys.D9;
            case KeyEvent.KEYCODE_STAR: return PCKeys.Multiply;
            case KeyEvent.KEYCODE_POUND: return 0;
            case KeyEvent.KEYCODE_DPAD_UP: return PCKeys.Up;
            case KeyEvent.KEYCODE_DPAD_DOWN: return PCKeys.Down;
            case KeyEvent.KEYCODE_DPAD_LEFT: return PCKeys.Left;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return PCKeys.Right;
            case KeyEvent.KEYCODE_CLEAR: return PCKeys.Clear;
            case KeyEvent.KEYCODE_HELP: return PCKeys.Help ;
            case KeyEvent.KEYCODE_A: return PCKeys.A;
            case KeyEvent.KEYCODE_B: return PCKeys.B;
            case KeyEvent.KEYCODE_C: return PCKeys.C;
            case KeyEvent.KEYCODE_D: return PCKeys.D;
            case KeyEvent.KEYCODE_E: return PCKeys.E;
            case KeyEvent.KEYCODE_F: return PCKeys.F;
            case KeyEvent.KEYCODE_G: return PCKeys.G;
            case KeyEvent.KEYCODE_H: return PCKeys.H;
            case KeyEvent.KEYCODE_I: return PCKeys.I;
            case KeyEvent.KEYCODE_J: return PCKeys.J;
            case KeyEvent.KEYCODE_K: return PCKeys.K;
            case KeyEvent.KEYCODE_L: return PCKeys.L;
            case KeyEvent.KEYCODE_M: return PCKeys.M;
            case KeyEvent.KEYCODE_N: return PCKeys.N;
            case KeyEvent.KEYCODE_O: return PCKeys.O;
            case KeyEvent.KEYCODE_P: return PCKeys.P;
            case KeyEvent.KEYCODE_Q: return PCKeys.Q;
            case KeyEvent.KEYCODE_R: return PCKeys.R;
            case KeyEvent.KEYCODE_S: return PCKeys.S;
            case KeyEvent.KEYCODE_T: return PCKeys.T;
            case KeyEvent.KEYCODE_U: return PCKeys.U;
            case KeyEvent.KEYCODE_V: return PCKeys.V;
            case KeyEvent.KEYCODE_W: return PCKeys.W;
            case KeyEvent.KEYCODE_X: return PCKeys.X;
            case KeyEvent.KEYCODE_Y: return PCKeys.Y;
            case KeyEvent.KEYCODE_Z: return PCKeys.Z;
            case KeyEvent.KEYCODE_ALL_APPS: return PCKeys.Apps;
            case KeyEvent.KEYCODE_SLEEP: return PCKeys.Sleep;
            case KeyEvent.KEYCODE_COMMA: return PCKeys.OemComma;
            case KeyEvent.KEYCODE_PERIOD: return PCKeys.OemPeriod;
            case KeyEvent.KEYCODE_ALT_LEFT: return PCKeys.LeftAlt;
            case KeyEvent.KEYCODE_ALT_RIGHT: return PCKeys.RightAlt;
            case KeyEvent.KEYCODE_SHIFT_LEFT: return PCKeys.LeftShift;
            case KeyEvent.KEYCODE_SHIFT_RIGHT: return PCKeys.RightShift;
            case KeyEvent.KEYCODE_TAB: return PCKeys.Tab;
            case KeyEvent.KEYCODE_SPACE: return PCKeys.Space;
            case KeyEvent.KEYCODE_ENTER: return PCKeys.Enter;
            case KeyEvent.KEYCODE_DEL: return PCKeys.Back;
            case KeyEvent.KEYCODE_GRAVE: return PCKeys.OemTilde;
            case KeyEvent.KEYCODE_MINUS: return PCKeys.Subtract;
            case KeyEvent.KEYCODE_EQUALS: return 0;
            case KeyEvent.KEYCODE_LEFT_BRACKET: return PCKeys.OemOpenBrackets;
            case KeyEvent.KEYCODE_RIGHT_BRACKET: return PCKeys.OemCloseBrackets;
            case KeyEvent.KEYCODE_BACKSLASH: return PCKeys.OemBackslash;
            case KeyEvent.KEYCODE_SEMICOLON: return PCKeys.OemSemicolon;
            case KeyEvent.KEYCODE_APOSTROPHE: return PCKeys.OemQuotes;
            case KeyEvent.KEYCODE_SLASH: return PCKeys.Divide;
            case KeyEvent.KEYCODE_AT: return 0;
            case KeyEvent.KEYCODE_PLUS: return PCKeys.Add;
            case KeyEvent.KEYCODE_MENU: return PCKeys.LWin;
            case KeyEvent.KEYCODE_SEARCH: return PCKeys.BrowserSearch;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: return PCKeys.MediaPlayPause;
            case KeyEvent.KEYCODE_MEDIA_STOP: return PCKeys.MediaStop;
            case KeyEvent.KEYCODE_MEDIA_NEXT: return PCKeys.MediaNextTrack;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: return PCKeys.MediaPreviousTrack;
            case KeyEvent.KEYCODE_PAGE_UP: return PCKeys.PageUp;
            case KeyEvent.KEYCODE_PAGE_DOWN: return PCKeys.PageDown;
            case KeyEvent.KEYCODE_ESCAPE: return PCKeys.Escape;
            case KeyEvent.KEYCODE_FORWARD_DEL: return PCKeys.Delete;
            case KeyEvent.KEYCODE_CTRL_LEFT: return PCKeys.LeftCtrl;
            case KeyEvent.KEYCODE_CTRL_RIGHT: return PCKeys.RightCtrl;
            case KeyEvent.KEYCODE_CAPS_LOCK: return PCKeys.CapsLock;
            case KeyEvent.KEYCODE_SCROLL_LOCK: return PCKeys.Scroll;
            case KeyEvent.KEYCODE_META_LEFT: return 0;
            case KeyEvent.KEYCODE_META_RIGHT: return 0;
            case KeyEvent.KEYCODE_SYSRQ: return PCKeys.PrintScreen;
            case KeyEvent.KEYCODE_MOVE_HOME: return PCKeys.Home;
            case KeyEvent.KEYCODE_MOVE_END: return PCKeys.End;
            case KeyEvent.KEYCODE_INSERT: return PCKeys.Insert;
            case KeyEvent.KEYCODE_MEDIA_PLAY: return PCKeys.Play;
            case KeyEvent.KEYCODE_MEDIA_PAUSE: return PCKeys.Pause;
            case KeyEvent.KEYCODE_F1: return PCKeys.F1;
            case KeyEvent.KEYCODE_F2: return PCKeys.F2;
            case KeyEvent.KEYCODE_F3: return PCKeys.F3;
            case KeyEvent.KEYCODE_F4: return PCKeys.F4;
            case KeyEvent.KEYCODE_F5: return PCKeys.F5;
            case KeyEvent.KEYCODE_F6: return PCKeys.F6;
            case KeyEvent.KEYCODE_F7: return PCKeys.F7;
            case KeyEvent.KEYCODE_F8: return PCKeys.F8;
            case KeyEvent.KEYCODE_F9: return PCKeys.F9;
            case KeyEvent.KEYCODE_F10: return PCKeys.F10;
            case KeyEvent.KEYCODE_F11: return PCKeys.F11;
            case KeyEvent.KEYCODE_F12: return PCKeys.F12;
            case KeyEvent.KEYCODE_NUM_LOCK: return PCKeys.NumLock;
            case KeyEvent.KEYCODE_NUMPAD_0: return PCKeys.NumPad0;
            case KeyEvent.KEYCODE_NUMPAD_1: return PCKeys.NumPad1;
            case KeyEvent.KEYCODE_NUMPAD_2: return PCKeys.NumPad2;
            case KeyEvent.KEYCODE_NUMPAD_3: return PCKeys.NumPad3;
            case KeyEvent.KEYCODE_NUMPAD_4: return PCKeys.NumPad4;
            case KeyEvent.KEYCODE_NUMPAD_5: return PCKeys.NumPad5;
            case KeyEvent.KEYCODE_NUMPAD_6: return PCKeys.NumPad6;
            case KeyEvent.KEYCODE_NUMPAD_7: return PCKeys.NumPad7;
            case KeyEvent.KEYCODE_NUMPAD_8: return PCKeys.NumPad8;
            case KeyEvent.KEYCODE_NUMPAD_9: return PCKeys.NumPad9;
            case KeyEvent.KEYCODE_REFRESH: return PCKeys.BrowserRefresh;
            case KeyEvent.KEYCODE_NUMPAD_DIVIDE: return PCKeys.Divide;
            case KeyEvent.KEYCODE_NUMPAD_MULTIPLY: return PCKeys.Multiply;
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT: return PCKeys.Subtract;
            case KeyEvent.KEYCODE_NUMPAD_ADD: return PCKeys.Add;
            case KeyEvent.KEYCODE_NUMPAD_COMMA: return PCKeys.OemComma;
            case KeyEvent.KEYCODE_NUMPAD_ENTER: return PCKeys.Enter;
            case KeyEvent.KEYCODE_NUMPAD_EQUALS: return 0;
            case KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN: return 0;
            case KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN: return 0;
            case KeyEvent.KEYCODE_WINDOW: return 0;
            case KeyEvent.KEYCODE_DPAD_UP_LEFT: return 0;
            case KeyEvent.KEYCODE_DPAD_DOWN_LEFT: return 0;
            case KeyEvent.KEYCODE_DPAD_UP_RIGHT: return 0;
            case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT: return 0;
            case KeyEvent.KEYCODE_CUT: return 0;
            case KeyEvent.KEYCODE_COPY: return 0;
            case KeyEvent.KEYCODE_PASTE: return 0;
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP: return PCKeys.Up;
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN: return PCKeys.Down;
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT: return PCKeys.Left;
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT: return PCKeys.Right;
            default: return 0;
        }
    }

    class PCKeys {
        public static final int
                None = 0,
        //
        // Сводка:
        //     Клавиша "Отмена".
        Cancel = 1,
        //
        // Сводка:
        //     Клавиша BACKSPACE.
        Back = 2,
        //
        // Сводка:
        //     Клавиша TAB.
        Tab = 3,
        //
        // Сводка:
        //     Клавиша перевода строки.
        LineFeed = 4,
        //
        // Сводка:
        //     Клавиша CLEAR.
        Clear = 5,
        //
        // Сводка:
        //     Клавиша RETURN.
        Return = 6,
        //
        // Сводка:
        //     Клавиша ВВОД.
        Enter = 6,
        //
        // Сводка:
        //     Клавиша паузы.
        Pause = 7,
        //
        // Сводка:
        //     Клавиша CAPS LOCK.
        Capital = 8,
        //
        // Сводка:
        //     Клавиша CAPS LOCK.
        CapsLock = 8,
        //
        // Сводка:
        //     Клавиша режима "Кана" редактора метода ввода.
        KanaMode = 9,
        //
        // Сводка:
        //     Клавиша режима "Хангыль" редактора метода ввода.
        HangulMode = 9,
        //
        // Сводка:
        //     Клавиша режима "Джунджа" редактора метода ввода.
        JunjaMode = 10,
        //
        // Сводка:
        //     Клавиша режима "Последний" редактора метода ввода.
        FinalMode = 11,
        //
        // Сводка:
        //     Клавиша режима "Ханджа" редактора метода ввода.
        HanjaMode = 12,
        //
        // Сводка:
        //     Клавиша режима "Кандзи" редактора метода ввода.
        KanjiMode = 12,
        //
        // Сводка:
        //     Клавиша ESC.
        Escape = 13,
        //
        // Сводка:
        //     Клавиша преобразования в редакторе метода ввода.
        ImeConvert = 14,
        //
        // Сводка:
        //     Клавиша без преобразования в редакторе метода ввода.
        ImeNonConvert = 15,
        //
        // Сводка:
        //     Клавиша принятия в редакторе метода ввода.
        ImeAccept = 16,
        //
        // Сводка:
        //     Запрос на изменение режима редактора метода ввода.
        ImeModeChange = 17,
        //
        // Сводка:
        //     Клавиша ПРОБЕЛ.
        Space = 18,
        //
        // Сводка:
        //     Клавиша PAGE UP.
        Prior = 19,
        //
        // Сводка:
        //     Клавиша PAGE UP.
        PageUp = 19,
        //
        // Сводка:
        //     Клавиша PAGE DOWN.
        Next = 20,
        //
        // Сводка:
        //     Клавиша PAGE DOWN.
        PageDown = 20,
        //
        // Сводка:
        //     Клавиша END.
        End = 21,
        //
        // Сводка:
        //     Клавиша HOME.
        Home = 22,
        //
        // Сводка:
        //     Клавиша СТРЕЛКА ВЛЕВО.
        Left = 23,
        //
        // Сводка:
        //     Клавиша СТРЕЛКА ВВЕРХ.
        Up = 24,
        //
        // Сводка:
        //     Клавиша СТРЕЛКА ВПРАВО.
        Right = 25,
        //
        // Сводка:
        //     Клавиша СТРЕЛКА ВНИЗ.
        Down = 26,
        //
        // Сводка:
        //     Клавиша "Выбрать".
        Select = 27,
        //
        // Сводка:
        //     Клавиша "Печать".
        Print = 28,
        //
        // Сводка:
        //     Клавиша "Выполнить".
        Execute = 29,
        //
        // Сводка:
        //     Клавиша PRINT SCREEN.
        Snapshot = 30,
        //
        // Сводка:
        //     Клавиша PRINT SCREEN.
        PrintScreen = 30,
        //
        // Сводка:
        //     Клавиша INSERT.
        Insert = 31,
        //
        // Сводка:
        //     Клавиша DELETE.
        Delete = 32,
        //
        // Сводка:
        //     Клавиша справки.
        Help = 33,
        //
        // Сводка:
        //     Клавиша 0 (нуль).
        D0 = 34,
        //
        // Сводка:
        //     Клавиша 1 (один).
        D1 = 35,
        //
        // Сводка:
        //     Клавиша 2.
        D2 = 36,
        //
        // Сводка:
        //     Клавиша 3.
        D3 = 37,
        //
        // Сводка:
        //     Клавиша 4.
        D4 = 38,
        //
        // Сводка:
        //     Клавиша 5.
        D5 = 39,
        //
        // Сводка:
        //     Клавиша 6.
        D6 = 40,
        //
        // Сводка:
        //     Клавиша 7.
        D7 = 41,
        //
        // Сводка:
        //     Клавиша 8.
        D8 = 42,
        //
        // Сводка:
        //     Клавиша 9.
        D9 = 43,
        //
        // Сводка:
        //     Клавиша A.
        A = 44,
        //
        // Сводка:
        //     Клавиша B.
        B = 45,
        //
        // Сводка:
        //     Клавиша C.
        C = 46,
        //
        // Сводка:
        //     Клавиша D.
        D = 47,
        //
        // Сводка:
        //     Клавиша E.
        E = 48,
        //
        // Сводка:
        //     Клавиша F.
        F = 49,
        //
        // Сводка:
        //     Клавиша G.
        G = 50,
        //
        // Сводка:
        //     Клавиша H.
        H = 51,
        //
        // Сводка:
        //     Клавиша I.
        I = 52,
        //
        // Сводка:
        //     Клавиша J.
        J = 53,
        //
        // Сводка:
        //     Клавиша K.
        K = 54,
        //
        // Сводка:
        //     Клавиша L.
        L = 55,
        //
        // Сводка:
        //     Клавиша M.
        M = 56,
        //
        // Сводка:
        //     Клавиша N.
        N = 57,
        //
        // Сводка:
        //     Клавиша O.
        O = 58,
        //
        // Сводка:
        //     Клавиша P.
        P = 59,
        //
        // Сводка:
        //     Клавиша Q.
        Q = 60,
        //
        // Сводка:
        //     Клавиша R.
        R = 61,
        //
        // Сводка:
        //     Клавиша S.
        S = 62,
        //
        // Сводка:
        //     Клавиша T.
        T = 63,
        //
        // Сводка:
        //     Клавиша U.
        U = 64,
        //
        // Сводка:
        //     Клавиша V.
        V = 65,
        //
        // Сводка:
        //     Клавиша W.
        W = 66,
        //
        // Сводка:
        //     Клавиша X.
        X = 67,
        //
        // Сводка:
        //     Клавиша Y.
        Y = 68,
        //
        // Сводка:
        //     Клавиша Z.
        Z = 69,
        //
        // Сводка:
        //     Левая клавиша с логотипом Windows (клавиатура Microsoft Natural Keyboard).
        LWin = 70,
        //
        // Сводка:
        //     Правая клавиша с логотипом Windows (клавиатура Microsoft Natural Keyboard).
        RWin = 71,
        //
        // Сводка:
        //     Клавиша приложения (клавиатура Microsoft Natural Keyboard).
        Apps = 72,
        //
        // Сводка:
        //     Клавиша перевода компьютера в спящий режим.
        Sleep = 73,
        //
        // Сводка:
        //     Клавиша 0 на цифровой клавиатуре.
        NumPad0 = 74,
        //
        // Сводка:
        //     Клавиша 1 на цифровой клавиатуре.
        NumPad1 = 75,
        //
        // Сводка:
        //     Клавиша 2 на цифровой клавиатуре.
        NumPad2 = 76,
        //
        // Сводка:
        //     Клавиша 3 на цифровой клавиатуре.
        NumPad3 = 77,
        //
        // Сводка:
        //     Клавиша 4 на цифровой клавиатуре.
        NumPad4 = 78,
        //
        // Сводка:
        //     Клавиша 5 на цифровой клавиатуре.
        NumPad5 = 79,
        //
        // Сводка:
        //     Клавиша 6 на цифровой клавиатуре.
        NumPad6 = 80,
        //
        // Сводка:
        //     Клавиша 7 на цифровой клавиатуре.
        NumPad7 = 81,
        //
        // Сводка:
        //     Клавиша 8 на цифровой клавиатуре.
        NumPad8 = 82,
        //
        // Сводка:
        //     Клавиша 9 на цифровой клавиатуре.
        NumPad9 = 83,
        //
        // Сводка:
        //     Клавиша умножения.
        Multiply = 84,
        //
        // Сводка:
        //     Клавиша сложения.
        Add = 85,
        //
        // Сводка:
        //     Клавиша разделителя.
        Separator = 86,
        //
        // Сводка:
        //     Клавиша вычитания.
        Subtract = 87,
        //
        // Сводка:
        //     Клавиша десятичного разделителя.
        Decimal = 88,
        //
        // Сводка:
        //     Клавиша деления.
        Divide = 89,
        //
        // Сводка:
        //     Клавиша F1.
        F1 = 90,
        //
        // Сводка:
        //     Клавиша F2.
        F2 = 91,
        //
        // Сводка:
        //     Клавиша F3.
        F3 = 92,
        //
        // Сводка:
        //     Клавиша F4.
        F4 = 93,
        //
        // Сводка:
        //     Клавиша F5.
        F5 = 94,
        //
        // Сводка:
        //     Клавиша F6.
        F6 = 95,
        //
        // Сводка:
        //     Клавиша F7.
        F7 = 96,
        //
        // Сводка:
        //     Клавиша F8.
        F8 = 97,
        //
        // Сводка:
        //     Клавиша F9.
        F9 = 98,
        //
        // Сводка:
        //     Клавиша F10.
        F10 = 99,
        //
        // Сводка:
        //     Клавиша F11.
        F11 = 100,
        //
        // Сводка:
        //     Клавиша F12.
        F12 = 101,
        //
        // Сводка:
        //     Клавиша F13.
        F13 = 102,
        //
        // Сводка:
        //     Клавиша F14.
        F14 = 103,
        //
        // Сводка:
        //     Клавиша F15.
        F15 = 104,
        //
        // Сводка:
        //     Клавиша F16.
        F16 = 105,
        //
        // Сводка:
        //     Клавиша F17.
        F17 = 106,
        //
        // Сводка:
        //     Клавиша F18.
        F18 = 107,
        //
        // Сводка:
        //     Клавиша F19.
        F19 = 108,
        //
        // Сводка:
        //     Клавиша F20.
        F20 = 109,
        //
        // Сводка:
        //     Клавиша F21.
        F21 = 110,
        //
        // Сводка:
        //     Клавиша F22.
        F22 = 111,
        //
        // Сводка:
        //     Клавиша F23.
        F23 = 112,
        //
        // Сводка:
        //     Клавиша F24.
        F24 = 113,
        //
        // Сводка:
        //     Клавиша NUM LOCK.
        NumLock = 114,
        //
        // Сводка:
        //     Клавиша SCROLL LOCK.
        Scroll = 115,
        //
        // Сводка:
        //     Левая клавиша SHIFT.
        LeftShift = 116,
        //
        // Сводка:
        //     Правая клавиша SHIFT.
        RightShift = 117,
        //
        // Сводка:
        //     Левая клавиша CTRL.
        LeftCtrl = 118,
        //
        // Сводка:
        //     Правая клавиша CTRL.
        RightCtrl = 119,
        //
        // Сводка:
        //     Левая клавиша ALT.
        LeftAlt = 120,
        //
        // Сводка:
        //     Правая клавиша ALT.
        RightAlt = 121,
        //
        // Сводка:
        //     Клавиша браузера "Назад".
        BrowserBack = 122,
        //
        // Сводка:
        //     Клавиша браузера "Вперед".
        BrowserForward = 123,
        //
        // Сводка:
        //     Клавиша браузера "Обновить".
        BrowserRefresh = 124,
        //
        // Сводка:
        //     Клавиша браузера "Остановить".
        BrowserStop = 125,
        //
        // Сводка:
        //     Клавиша браузера "Поиск".
        BrowserSearch = 126,
        //
        // Сводка:
        //     Клавиша браузера "Избранное".
        BrowserFavorites = 127,
        //
        // Сводка:
        //     Клавиша браузера "Главная".
        BrowserHome = 128,
        //
        // Сводка:
        //     Клавиша выключения звука.
        VolumeMute = 129,
        //
        // Сводка:
        //     Клавиша уменьшения громкости.
        VolumeDown = 130,
        //
        // Сводка:
        //     Клавиша увеличения громкости.
        VolumeUp = 131,
        //
        // Сводка:
        //     Клавиша "Следующая запись".
        MediaNextTrack = 132,
        //
        // Сводка:
        //     Клавиша "Предыдущая запись".
        MediaPreviousTrack = 133,
        //
        // Сводка:
        //     Клавиша остановки воспроизведения.
        MediaStop = 134,
        //
        // Сводка:
        //     Клавиша приостановки воспроизведения.
        MediaPlayPause = 135,
        //
        // Сводка:
        //     Клавиша запуска почты.
        LaunchMail = 136,
        //
        // Сводка:
        //     Клавиша выбора мультимедиа.
        SelectMedia = 137,
        //
        // Сводка:
        //     Клавиша запуска приложения 1.
        LaunchApplication1 = 138,
        //
        // Сводка:
        //     Клавиша запуска приложения 2.
        LaunchApplication2 = 139,
        //
        // Сводка:
        //     Клавиша OEM 1.
        Oem1 = 140,
        //
        // Сводка:
        //     Клавиша OEM с точкой с запятой.
        OemSemicolon = 140,
        //
        // Сводка:
        //     Клавиша OEM со сложением.
        OemPlus = 141,
        //
        // Сводка:
        //     Клавиша OEM с запятой.
        OemComma = 142,
        //
        // Сводка:
        //     Клавиша OEM с минусом.
        OemMinus = 143,
        //
        // Сводка:
        //     Клавиша OEM с точкой.
        OemPeriod = 144,
        //
        // Сводка:
        //     Клавиша OEM 2.
        Oem2 = 145,
        //
        // Сводка:
        //     Клавиша OEM с вопросительным знаком.
        OemQuestion = 145,
        //
        // Сводка:
        //     Клавиша OEM 3.
        Oem3 = 146,
        //
        // Сводка:
        //     Клавиша OEM с тильдой.
        OemTilde = 146,
        //
        // Сводка:
        //     Клавиша ABNT_C1 (Бразилия).
        AbntC1 = 147,
        //
        // Сводка:
        //     Клавиша ABNT_C2 (Бразилия).
        AbntC2 = 148,
        //
        // Сводка:
        //     Клавиша OEM 4.
        Oem4 = 149,
        //
        // Сводка:
        //     Клавиша OEM с открывающими скобками.
        OemOpenBrackets = 149,
        //
        // Сводка:
        //     Клавиша OEM 5.
        Oem5 = 150,
        //
        // Сводка:
        //     Клавиша OEM с вертикальной чертой.
        OemPipe = 150,
        //
        // Сводка:
        //     Клавиша OEM 6.
        Oem6 = 151,
        //
        // Сводка:
        //     Клавиша OEM с закрывающими скобками.
        OemCloseBrackets = 151,
        //
        // Сводка:
        //     Клавиша OEM 7.
        Oem7 = 152,
        //
        // Сводка:
        //     Клавиша OEM с кавычками.
        OemQuotes = 152,
        //
        // Сводка:
        //     Клавиша OEM 8.
        Oem8 = 153,
        //
        // Сводка:
        //     Клавиша OEM 102.
        Oem102 = 154,
        //
        // Сводка:
        //     Клавиша OEM с обратной косой чертой.
        OemBackslash = 154,
        //
        // Сводка:
        //     Специальная клавиша, маскирующая фактическую клавишу, обрабатываемую редактором
        //     метода ввода.
        ImeProcessed = 155,
        //
        // Сводка:
        //     Специальный клавиша, маскирующая фактическую клавишу, обрабатываемую в качестве
        //     системной клавиши.
        System = 156,
        //
        // Сводка:
        //     Клавиша OEM ATTN.
        OemAttn = 157,
        //
        // Сводка:
        //     Клавиша DBE_ALPHANUMERIC.
        DbeAlphanumeric = 157,
        //
        // Сводка:
        //     Клавиша OEM FINISH.
        OemFinish = 158,
        //
        // Сводка:
        //     Клавиша DBE_KATAKANA.
        DbeKatakana = 158,
        //
        // Сводка:
        //     Клавиша OEM COPY.
        OemCopy = 159,
        //
        // Сводка:
        //     Клавиша DBE_HIRAGANA.
        DbeHiragana = 159,
        //
        // Сводка:
        //     Клавиша OEM AUTO.
        OemAuto = 160,
        //
        // Сводка:
        //     Клавиша DBE_SBCSCHAR.
        DbeSbcsChar = 160,
        //
        // Сводка:
        //     Клавиша OEM ENLW.
        OemEnlw = 161,
        //
        // Сводка:
        //     Клавиша DBE_DBCSCHAR.
        DbeDbcsChar = 161,
        //
        // Сводка:
        //     Клавиша OEM BACKTAB.
        OemBackTab = 162,
        //
        // Сводка:
        //     Клавиша DBE_ROMAN.
        DbeRoman = 162,
        //
        // Сводка:
        //     Клавиша ATTN.
        Attn = 163,
        //
        // Сводка:
        //     Клавиша DBE_NOROMAN.
        DbeNoRoman = 163,
        //
        // Сводка:
        //     Клавиша CRSEL.
        CrSel = 164,
        //
        // Сводка:
        //     Клавиша DBE_ENTERWORDREGISTERMODE.
        DbeEnterWordRegisterMode = 164,
        //
        // Сводка:
        //     Клавиша EXSEL.
        ExSel = 165,
        //
        // Сводка:
        //     Клавиша DBE_ENTERIMECONFIGMODE.
        DbeEnterImeConfigureMode = 165,
        //
        // Сводка:
        //     Клавиша ERASE EOF.
        EraseEof = 166,
        //
        // Сводка:
        //     Клавиша DBE_FLUSHSTRING.
        DbeFlushString = 166,
        //
        // Сводка:
        //     Клавиша ВОСПРОИЗВЕСТИ.
        Play = 167,
        //
        // Сводка:
        //     Клавиша DBE_CODEINPUT.
        DbeCodeInput = 167,
        //
        // Сводка:
        //     Клавиша МАСШТАБ.
        Zoom = 168,
        //
        // Сводка:
        //     Клавиша DBE_NOCODEINPUT.
        DbeNoCodeInput = 168,
        //
        // Сводка:
        //     Константа, зарезервированная для будущего использования.
        NoName = 169,
        //
        // Сводка:
        //     Клавиша DBE_DETERMINESTRING.
        DbeDetermineString = 169,
        //
        // Сводка:
        //     Клавиша PA1.
        Pa1 = 170,
        //
        // Сводка:
        //     Клавиша DBE_ENTERDLGCONVERSIONMODE.
        DbeEnterDialogConversionMode = 170,
        //
        // Сводка:
        //     Клавиша OEM очистки.
        OemClear = 171,
        //
        // Сводка:
        //     Клавиша используется вместе с другой клавишей для создания одного объединенного
        //     символа.
        DeadCharProcessed = 172;
    }



    public void Stop(View v){
        try {
            JSONObject msg3 = new JSONObject();
            msg3.put("Type", "finish");
            msg3.put("Name", name);
            Send(msg3.toString().getBytes());
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
