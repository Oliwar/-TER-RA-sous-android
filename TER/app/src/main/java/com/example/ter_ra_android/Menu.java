package com.example.ter_ra_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class Menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
    }

    public void onJouerClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void onOptionsClick(View view) {
        Toast.makeText(Menu.this, "Les options ne sont pas disponibles.", Toast.LENGTH_SHORT).show();
    }

    public void onAideClick(View view) {
        Intent intent = new Intent(this, Help.class);
        startActivity(intent);
    }
}
