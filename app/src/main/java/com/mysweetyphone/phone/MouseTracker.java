package com.mysweetyphone.phone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.KeyListener;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import Utils.Message;
import Utils.SessionClient;

public class MouseTracker extends AppCompatActivity {

    public static abstract class CustomTimerTask extends TimerTask{
        SingleClick click;

        CustomTimerTask(SingleClick click){
            this.click = click;
        }

        public abstract void action();

        @Override
        public void run() {
            action();
            click.t.cancel();
        }
    }

    static class SingleClick{

        int x,y;
        int type;
        long time;
        Timer t;
        SingleClick(int x, int y, long time, int type){
            this.x = x;
            this.y = y;
            this.time = time;
            this.type = type;
        }

        void Later(CustomTimerTask tt, int delay){
            t = new Timer();
            t.scheduleAtFixedRate(tt, delay, 1000);
        }

        void Cancel(){
            if(t!=null) t.cancel();
        }
    }

    static public SessionClient sc;
    static final int MESSAGESIZE = 100;
    static String name;
    Switch win, alt, shift, ctrl;
    EditText inputView;
    SingleClick singleClick;
    long lastUpTime = 0;
    long lastDownTime = 0;
    boolean LPressed = false;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mouse_tracker);
        Toolbar toolbar = findViewById(R.id.toolbarMOUSETRACKER);
        setSupportActionBar(toolbar);

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

        win = findViewById(R.id.winMOUSETRACKER);
        alt = findViewById(R.id.altMOUSETRACKER);
        ctrl = findViewById(R.id.ctrlMOUSETRACKER);
        shift = findViewById(R.id.shiftMOUSETRACKER);
        ImageButton keyboardButton = findViewById(R.id.keyboardMOUSETRACKER);
        HorizontalScrollView extraButtons = findViewById(R.id.extra1MOUSETRACKER);
        HorizontalScrollView extra2Buttons = findViewById(R.id.extra2MOUSETRACKER);
        HorizontalScrollView extra3Buttons = findViewById(R.id.extra3MOUSETRACKER);
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
                    if(win.isChecked() || alt.isChecked() || shift.isChecked() || ctrl.isChecked()){
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
                TableLayout tl = findViewById(R.id.extraButtonsMOUSETRACKER);
                String value = parent.getSelectedItem().toString();
                if(Objects.equals(value, mouse)) {
                    inputView.setVisibility(View.GONE);
                    extraButtons.setVisibility(View.GONE);
                    extra2Buttons.setVisibility(View.GONE);
                    tl.setVisibility(View.GONE);
                    keyboardButton.setVisibility(View.GONE);
                    content.setOnTouchListener(thisActivity::onTouchMOUSE);
                    extra3Buttons.setVisibility(View.GONE);
                }else if(value.equals(keyboard)) {
                    inputView.setVisibility(View.VISIBLE);
                    tl.setVisibility(View.VISIBLE);
                    extraButtons.setVisibility(View.VISIBLE);
                    extra2Buttons.setVisibility(View.VISIBLE);
                    keyboardButton.setVisibility(View.VISIBLE);
                    if(sc.getOS().startsWith("Windows")) extra3Buttons.setVisibility(View.VISIBLE);
                    thisActivity.openKeyboard(null);
                    content.setOnTouchListener((v,e)->false);
                }else if(value.equals(pen_tablet)) {
                    inputView.setVisibility(View.GONE);
                    tl.setVisibility(View.GONE);
                    keyboardButton.setVisibility(View.GONE);
                    content.setOnTouchListener(thisActivity::onTouchPENTABLET);
                    extraButtons.setVisibility(View.GONE);
                    extra2Buttons.setVisibility(View.GONE);
                    extra3Buttons.setVisibility(View.GONE);
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
                Message[] messages = Message.getMessages(b, MESSAGESIZE);
                for (Message m : messages) {
                    sc.getDatagramSocket().send(new DatagramPacket(m.getArr(), m.getArr().length, sc.getAddress(), sc.getPort()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public boolean onTouchMOUSE(View v, final MotionEvent event) {
        final int interval = 200;
        try {
            SingleClick nextSingleClick = new SingleClick((int) event.getX(), (int) event.getY(), System.currentTimeMillis(), event.getAction());
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if(singleClick != null && singleClick.type == MotionEvent.ACTION_UP  && nextSingleClick.time-singleClick.time <= interval) {
                        singleClick.Cancel();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    JSONObject msg = new JSONObject();
                    msg.put("Type", "mouseMoved");
                    msg.put("Name", name);
                    msg.put("X", nextSingleClick.x - singleClick.x);
                    msg.put("Y", nextSingleClick.y - singleClick.y);
                    Send(msg.toString().getBytes());
                    break;
                case MotionEvent.ACTION_UP:
                    if(singleClick.type == MotionEvent.ACTION_DOWN && nextSingleClick.time-singleClick.time <= interval) {
                        msg = new JSONObject();
                        msg.put("Type", "mousePressed");
                        msg.put("Name", name);
                        msg.put("Key", 1);
                        Send(msg.toString().getBytes());
                        nextSingleClick.Later(new CustomTimerTask(nextSingleClick) {
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
                        }, interval);
                    }else if(singleClick.type == MotionEvent.ACTION_DOWN && nextSingleClick.time-singleClick.time > interval){
                        msg = new JSONObject();
                        msg.put("Type", "mouseClicked");
                        msg.put("Name", name);
                        msg.put("Key", 3);
                        Send(msg.toString().getBytes());
                    }else{
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
            singleClick = nextSingleClick;

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
                return 27;
            case R.id.tabMOUSETRACKER:
                return 9;
            case R.id.capslockMOUSETRACKER:
                return 20;
            case R.id.leftMOUSETRACKER:
                return 37;
            case R.id.downMOUSETRACKER:
                return 40;
            case R.id.upMOUSETRACKER:
                return 38;
            case R.id.rightMOUSETRACKER:
                return 39;
            case R.id.delMOUSETRACKER:
                return 127;
            case R.id.numlockMOUSETRACKER:
                return 144;
            case R.id.scrolllockMOUSETRACKER:
                return 145;
            case R.id.homeMOUSETRACKER:
                return 36;
            case R.id.endMOUSETRACKER:
                return 35;
            case R.id.pageupMOUSETRACKER:
                return 33;
            case R.id.pagedownMOUSETRACKER:
                return 34;
            case R.id.insertMOUSETRACKER:
                return 155;
            case R.id.printscreenMOUSETRACKER:
                return 154;
            case R.id.ctrlMOUSETRACKER:
                return 17;
            case R.id.shiftMOUSETRACKER:
                return 16;
            case R.id.altMOUSETRACKER:
                return 18;
            case R.id.winMOUSETRACKER:
                return 524;
            case R.id.enterMOUSETRACKER:
                return 10;
            case R.id.F1MOUSETRACKER:
                return 112;
            case R.id.F2MOUSETRACKER:
                return 113;
            case R.id.F3MOUSETRACKER:
                return 114;
            case R.id.F4MOUSETRACKER:
                return 115;
            case R.id.F5MOUSETRACKER:
                return 116;
            case R.id.F6MOUSETRACKER:
                return 117;
            case R.id.F7MOUSETRACKER:
                return 118;
            case R.id.F8MOUSETRACKER:
                return 119;
            case R.id.F9MOUSETRACKER:
                return 120;
            case R.id.F10MOUSETRACKER:
                return 121;
            case R.id.F11MOUSETRACKER:
                return 122;
            case R.id.F12MOUSETRACKER:
                return 123;
            case R.id.volumeMuteMOUSETRACKER:
                return 173;
            case R.id.volumeDownMOUSETRACKER:
                return 174;
            case R.id.volumeUpMOUSETRACKER:
                return 175;
            case R.id.nextTrackMOUSETRACKER:
                return 176;
            case R.id.prevTrackMOUSETRACKER:
                return 177;
            case R.id.stopMOUSETRACKER:
                return 178;
            case R.id.pauseMOUSETRACKER:
                return 179;
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

    public void sendMediaButton(View v) throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("Name", name);
        msg.put("value", ViewToButtonId(v));
        msg.put("Type", "winApiClicked");
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

    public int AndroidToAwt(int e){
        switch (e){
            case KeyEvent.KEYCODE_HOME: return 36;
            case KeyEvent.KEYCODE_BACK: return 35;
            case KeyEvent.KEYCODE_0: return 48;
            case KeyEvent.KEYCODE_1: return 49;
            case KeyEvent.KEYCODE_2: return 50;
            case KeyEvent.KEYCODE_3: return 51;
            case KeyEvent.KEYCODE_4: return 52;
            case KeyEvent.KEYCODE_5: return 53;
            case KeyEvent.KEYCODE_6: return 54;
            case KeyEvent.KEYCODE_7: return 55;
            case KeyEvent.KEYCODE_8: return 56;
            case KeyEvent.KEYCODE_9: return 57;
            case KeyEvent.KEYCODE_STAR: return 151;
            case KeyEvent.KEYCODE_POUND: return 520;
            case KeyEvent.KEYCODE_DPAD_UP: return 38;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 40;
            case KeyEvent.KEYCODE_DPAD_LEFT: return 37;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 39;
            case KeyEvent.KEYCODE_CLEAR: return 12;
            case KeyEvent.KEYCODE_A: return 65;
            case KeyEvent.KEYCODE_B: return 66;
            case KeyEvent.KEYCODE_C: return 67;
            case KeyEvent.KEYCODE_D: return 68;
            case KeyEvent.KEYCODE_E: return 69;
            case KeyEvent.KEYCODE_F: return 70;
            case KeyEvent.KEYCODE_G: return 71;
            case KeyEvent.KEYCODE_H: return 72;
            case KeyEvent.KEYCODE_I: return 73;
            case KeyEvent.KEYCODE_J: return 74;
            case KeyEvent.KEYCODE_K: return 75;
            case KeyEvent.KEYCODE_L: return 76;
            case KeyEvent.KEYCODE_M: return 77;
            case KeyEvent.KEYCODE_N: return 78;
            case KeyEvent.KEYCODE_O: return 79;
            case KeyEvent.KEYCODE_P: return 80;
            case KeyEvent.KEYCODE_Q: return 81;
            case KeyEvent.KEYCODE_R: return 82;
            case KeyEvent.KEYCODE_S: return 83;
            case KeyEvent.KEYCODE_T: return 84;
            case KeyEvent.KEYCODE_U: return 85;
            case KeyEvent.KEYCODE_V: return 86;
            case KeyEvent.KEYCODE_W: return 87;
            case KeyEvent.KEYCODE_X: return 88;
            case KeyEvent.KEYCODE_Y: return 89;
            case KeyEvent.KEYCODE_Z: return 90;
            case KeyEvent.KEYCODE_COMMA: return 44;
            case KeyEvent.KEYCODE_PERIOD: return 46;
            case KeyEvent.KEYCODE_ALT_LEFT: return 18;
            case KeyEvent.KEYCODE_ALT_RIGHT: return 18;
            case KeyEvent.KEYCODE_SHIFT_LEFT: return 16;
            case KeyEvent.KEYCODE_SHIFT_RIGHT: return 16;
            case KeyEvent.KEYCODE_TAB: return 9;
            case KeyEvent.KEYCODE_SPACE: return 32;
            case KeyEvent.KEYCODE_ENTER: return 10;
            case KeyEvent.KEYCODE_DEL: return 8;
            case KeyEvent.KEYCODE_GRAVE: return 192;
            case KeyEvent.KEYCODE_MINUS: return 45;
            case KeyEvent.KEYCODE_EQUALS: return 61;
            case KeyEvent.KEYCODE_LEFT_BRACKET: return 91;
            case KeyEvent.KEYCODE_RIGHT_BRACKET: return 93;
            case KeyEvent.KEYCODE_BACKSLASH: return 92;
            case KeyEvent.KEYCODE_SEMICOLON: return 59;
            case KeyEvent.KEYCODE_APOSTROPHE: return 222;
            case KeyEvent.KEYCODE_SLASH: return 47;
            case KeyEvent.KEYCODE_AT: return 512;
            case KeyEvent.KEYCODE_PLUS: return 521;
            case KeyEvent.KEYCODE_MENU: return 525;
            case KeyEvent.KEYCODE_SEARCH: return 65488;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: return 19;
            case KeyEvent.KEYCODE_MEDIA_STOP: return 65480;
            case KeyEvent.KEYCODE_PAGE_UP: return 33;
            case KeyEvent.KEYCODE_PAGE_DOWN: return 34;
            case KeyEvent.KEYCODE_ESCAPE: return 27;
            case KeyEvent.KEYCODE_FORWARD_DEL: return 127;
            case KeyEvent.KEYCODE_CTRL_LEFT: return 17;
            case KeyEvent.KEYCODE_CTRL_RIGHT: return 17;
            case KeyEvent.KEYCODE_CAPS_LOCK: return 20;
            case KeyEvent.KEYCODE_SCROLL_LOCK: return 145;
            case KeyEvent.KEYCODE_META_LEFT: return 157;
            case KeyEvent.KEYCODE_META_RIGHT: return 157;
            case KeyEvent.KEYCODE_SYSRQ: return 154;
            case KeyEvent.KEYCODE_MOVE_HOME: return 36;
            case KeyEvent.KEYCODE_MOVE_END: return 35;
            case KeyEvent.KEYCODE_INSERT: return 155;
            case KeyEvent.KEYCODE_MEDIA_PLAY: return 19;
            case KeyEvent.KEYCODE_MEDIA_PAUSE: return 19;
            case KeyEvent.KEYCODE_F1: return 112;
            case KeyEvent.KEYCODE_F2: return 113;
            case KeyEvent.KEYCODE_F3: return 114;
            case KeyEvent.KEYCODE_F4: return 115;
            case KeyEvent.KEYCODE_F5: return 116;
            case KeyEvent.KEYCODE_F6: return 117;
            case KeyEvent.KEYCODE_F7: return 118;
            case KeyEvent.KEYCODE_F8: return 119;
            case KeyEvent.KEYCODE_F9: return 120;
            case KeyEvent.KEYCODE_F10: return 121;
            case KeyEvent.KEYCODE_F11: return 122;
            case KeyEvent.KEYCODE_F12: return 123;
            case KeyEvent.KEYCODE_NUM_LOCK: return 144;
            case KeyEvent.KEYCODE_NUMPAD_0: return 96;
            case KeyEvent.KEYCODE_NUMPAD_1: return 97;
            case KeyEvent.KEYCODE_NUMPAD_2: return 98;
            case KeyEvent.KEYCODE_NUMPAD_3: return 99;
            case KeyEvent.KEYCODE_NUMPAD_4: return 100;
            case KeyEvent.KEYCODE_NUMPAD_5: return 101;
            case KeyEvent.KEYCODE_NUMPAD_6: return 102;
            case KeyEvent.KEYCODE_NUMPAD_7: return 103;
            case KeyEvent.KEYCODE_NUMPAD_8: return 104;
            case KeyEvent.KEYCODE_NUMPAD_9: return 105;
            case KeyEvent.KEYCODE_NUMPAD_DIVIDE: return 111;
            case KeyEvent.KEYCODE_NUMPAD_MULTIPLY: return 106;
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT: return 109;
            case KeyEvent.KEYCODE_NUMPAD_ADD: return 107;
            case KeyEvent.KEYCODE_NUMPAD_COMMA: return 44;
            case KeyEvent.KEYCODE_NUMPAD_ENTER: return 10;
            case KeyEvent.KEYCODE_NUMPAD_EQUALS: return 61;
            case KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN: return 519;
            case KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN: return 522;
            case KeyEvent.KEYCODE_WINDOW: return 524;
            case KeyEvent.KEYCODE_HELP: return 156;
            case KeyEvent.KEYCODE_DPAD_UP_LEFT: return 224;
            case KeyEvent.KEYCODE_DPAD_DOWN_LEFT: return 225;
            case KeyEvent.KEYCODE_DPAD_UP_RIGHT: return 226;
            case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT: return 227;
            case KeyEvent.KEYCODE_CUT: return 65489;
            case KeyEvent.KEYCODE_COPY: return 65485;
            case KeyEvent.KEYCODE_PASTE: return 65487;
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP: return 38;
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN: return 40;
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT: return 37;
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT: return 39;
            default: return 0;
        }
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
