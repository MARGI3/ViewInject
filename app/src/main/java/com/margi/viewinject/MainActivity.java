package com.margi.viewinject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.margi.annotation.InjectView;
import com.margi.core.Injector;

public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.tv)
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Injector.inject(this);
        textView.setText("bind success");
    }
}
