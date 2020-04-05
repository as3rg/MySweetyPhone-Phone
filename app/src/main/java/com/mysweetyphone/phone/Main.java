package com.mysweetyphone.phone;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import Utils.ServerMode;

public class Main extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (fm.getFragments().isEmpty()) {
            ft.replace(R.id.MainFragment, new SClient());
            ft.commit();

            setContentView(R.layout.activity_main);
            Toolbar toolbar = findViewById(R.id.toolbarMAIN);
            setSupportActionBar(toolbar);
            TextView title = findViewById(R.id.titleMAIN);
            Shader textShader = new LinearGradient(0, 0, title.getMeasuredWidth(), title.getLineHeight(),
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
            navigationView.setNavigationItemSelectedListener(this);

            navigationView.inflateMenu(R.menu.activity_main_drawer);

            ServerMode.SetContext(this);
        }
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
        name.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("name",""));
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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
                editor.remove("name");
                editor.apply();
                finish();
                return false;
            case R.id.nav_sclient:
                FragmentToReplace = new SClient();
                break;
            case R.id.nav_sserver:
                FragmentToReplace = new SServer();
                break;
        }
        fm.getFragments().clear();
        ft.replace(R.id.MainFragment, FragmentToReplace);
        ft.commit();
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void ChangeCode(View view) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine(true);
        int code = PreferenceManager.getDefaultSharedPreferences(this).getInt("code", 0);
        input.setText(String.format("%06d", code));
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle("Код")
                .setMessage("Введите новый код")
                .setView(input)
                .setPositiveButton("Ввод", null)
                .setNegativeButton("Отмена", (dialog, which) -> {})
                .create();
        alert.setOnShowListener(dialog -> {
            alert.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                int code2 = 0;
                System.out.println(input.getText()+" "+input.getText().toString());
                if(input.getText() != null && !input.getText().toString().isEmpty())
                    code2 = Integer.parseInt(input.getText().toString());
                editor.putInt("code", code2);
                editor.apply();
                alert.dismiss();
            });
        });
        alert.show();
    }
}
