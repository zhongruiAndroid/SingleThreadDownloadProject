package com.test.singlethreaddownloadproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btApiTest;
    Button btDownload;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btApiTest=findViewById(R.id.btApiTest);
        btApiTest.setOnClickListener(this);


        btDownload=findViewById(R.id.btDownload);
        btDownload.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btApiTest:
                startActivity(new Intent(this,ApiTestActivity.class));
            break;
            case R.id.btDownload:
                startActivity(new Intent(this,TestActivity.class));
            break;
        }
    }
}