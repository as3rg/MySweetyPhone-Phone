package com.mysweetyphone.phone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import Utils.ServerMode;

public class Main extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private int id;
    private String login;
    private String name;

    private ImageButton reload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        login = PreferenceManager.getDefaultSharedPreferences(this).getString("login","");

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if(getIntent().getAction() == Intent.ACTION_SEND){
            fm.getFragments().clear();
            ft.replace(R.id.MainFragment,new Saved());
        }else {
            if (fm.getFragments().isEmpty()) {
                if(login.isEmpty())
                    ft.replace(R.id.MainFragment, new SClient());
                else
                    ft.replace(R.id.MainFragment, new DevicesList());
            }
        }
        ft.commit();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbarMAIN);
        setSupportActionBar(toolbar);
        TextView title = findViewById(R.id.titleMAIN);
        Shader textShader = new LinearGradient(0, 0, title.getMeasuredWidth(),title.getLineHeight(),
                new int[]{
                        Color.parseColor("#d53369"),
                        Color.parseColor("#cbad6d"),
                }, null, Shader.TileMode.CLAMP);
        title.getPaint().setShader(textShader);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        reload = findViewById(R.id.reloadMAIN);
        if(login.isEmpty()){
            navigationView.inflateMenu(R.menu.activity_main_drawer_offline);
            reload.setVisibility(View.GONE);

        }else{
            navigationView.inflateMenu(R.menu.activity_main_drawer);
            reload.setVisibility(View.VISIBLE);
        }

        ServerMode.SetContext(this);
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        TextView name = findViewById(R.id.NameNav);
        if(login.isEmpty() && name != null) {
            name.setText("Инкогнито");
            name.setTextColor(Color.parseColor("#cccccc"));
            name.setTypeface(null, Typeface.ITALIC);
        }
        else if(name != null) name.setText(login);
        return true;
    }

    public boolean reload(View v){
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment currentFragment = fm.findFragmentById(R.id.MainFragment);
        ft.detach(currentFragment);
        ft.attach(currentFragment);
        ft.commit();
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        try {
            int itemId = item.getItemId();
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment currentFragment = fm.findFragmentById(R.id.MainFragment);
            currentFragment.onDestroy();
            Fragment FragmentToReplace = null;
            switch (itemId) {
                case R.id.nav_exit:
                    final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                    final SharedPreferences.Editor editor = sharedPreferences.edit();
                    id = PreferenceManager.getDefaultSharedPreferences(this).getInt("id", -1);
                    name = (PreferenceManager.getDefaultSharedPreferences(this)).getString("name", "");
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.get("http://mysweetyphone.herokuapp.com/?Type=RemoveDevice&Login=" + URLEncoder.encode(login, "UTF-8") + "&Id=" + id + "&Name=" + URLEncoder.encode(name, "UTF-8"), new JsonHttpResponseHandler());
                    editor.remove("id");
                    editor.remove("name");
                    editor.remove("login");
                    editor.apply();
                    finish();
                    return false;
                case R.id.nav_devices_list:
                    reload.setVisibility(View.VISIBLE);
                    FragmentToReplace = new DevicesList();
                    break;
                case R.id.nav_saved:
                    reload.setVisibility(View.VISIBLE);
                    FragmentToReplace = new Saved();
                    break;
                case R.id.nav_sclient:
                    reload.setVisibility(View.GONE);
                    FragmentToReplace = new SClient();
                    break;
                case R.id.nav_sserver:
                    reload.setVisibility(View.GONE);
                    FragmentToReplace = new SServer();
                    break;
            }
            fm.getFragments().clear();
            ft.replace(R.id.MainFragment, FragmentToReplace);
            ft.commit();
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return true;
    }
}
