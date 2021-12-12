package com.example.protibadidevice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Intent anotherActivity;
    String image;
    String name;
    List<String> publish_permission;
    TextView textView;
    Button tofacebook;

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_main);
        this.publish_permission = new ArrayList();
        this.publish_permission.add("publish_actions");

//        Toast.makeText(MainActivity.this.getApplicationContext(), "Successfully Logged In", 1).show();
        MainActivity.this.startActivity(new Intent(MainActivity.this, Main2Activity.class));


        /* Access modifiers changed, original: protected */

    }
}