package com.mysweetyphone.phone;

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
import android.text.method.BaseKeyListener;
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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;

import Utils.AndroidToAwt;
import Utils.Message;
import Utils.SessionClient;

public class MouseTracker extends AppCompatActivity {

    static public SessionClient sc;
    static final int MESSAGESIZE = 100;
    static String name;
    Switch win, alt, shift, ctrl;
    EditText inputView;
    long DownX,x,DownY,y;
    long lastUpTime = 0;
    long lastDownTime = 0;
    boolean LPressed = false;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mouse_tracker);
        Toolbar toolbar = findViewById(R.id.toolbar);
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
                    msg.put("Type", "keysTyped");
                    msg.put("value", s);
                    msg.put("Name", name);
                    Send(msg.toString().getBytes());
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
                    msg.put("value", AndroidToAwt.AndroidToAwt(keyCode));
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
                getResources().getString(R.string.gamepadMOUSETRACKER),
                getResources().getString(R.string.pen_tabletMOUSETRACKER)
        }));

        MouseTracker thisActivity = this;
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            final String mouse = getResources().getString(R.string.mouseMOUSETRACKER);
            final String keyboard = getResources().getString(R.string.keyboardMOUSETRACKER);
            final String gamepad = getResources().getString(R.string.gamepadMOUSETRACKER);
            final String pen_tablet = getResources().getString(R.string.pen_tabletMOUSETRACKER);

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TableLayout tl = findViewById(R.id.extraButtonsMOUSETRACKER);
                String value = parent.getSelectedItem().toString();
                if(value == mouse) {
                    inputView.setVisibility(View.INVISIBLE);
                    extraButtons.setVisibility(View.INVISIBLE);
                    extra2Buttons.setVisibility(View.INVISIBLE);
                    tl.setVisibility(View.INVISIBLE);
                    keyboardButton.setVisibility(View.INVISIBLE);
                    content.setOnTouchListener(thisActivity::onTouchMOUSE);
                }else if(value == keyboard) {
                    inputView.setVisibility(View.VISIBLE);
                    tl.setVisibility(View.VISIBLE);
                    extraButtons.setVisibility(View.VISIBLE);
                    extra2Buttons.setVisibility(View.VISIBLE);
                    keyboardButton.setVisibility(View.VISIBLE);
                    thisActivity.openKeyboard(null);
                    content.setOnTouchListener((v,e)->false);
                }else if(value == gamepad) {
                    inputView.setVisibility(View.INVISIBLE);
                    extraButtons.setVisibility(View.INVISIBLE);
                    extra2Buttons.setVisibility(View.INVISIBLE);
                    tl.setVisibility(View.INVISIBLE);
                    keyboardButton.setVisibility(View.INVISIBLE);
                    System.out.println("Gamepad");
                    content.setOnTouchListener((v,e)->false);
                }else if(value == pen_tablet) {
                    inputView.setVisibility(View.INVISIBLE);
                    tl.setVisibility(View.INVISIBLE);
                    keyboardButton.setVisibility(View.INVISIBLE);
                    content.setOnTouchListener(thisActivity::onTouchPENTABLET);
                    extraButtons.setVisibility(View.INVISIBLE);
                    extra2Buttons.setVisibility(View.INVISIBLE);
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
                    sc.getSocket().send(new DatagramPacket(m.getArr(), m.getArr().length, sc.getAddress(), sc.getPort()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public boolean onTouchMOUSE(View v, final MotionEvent event) {
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
            default: throw new RuntimeException("Неизвестная команда");
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
}
