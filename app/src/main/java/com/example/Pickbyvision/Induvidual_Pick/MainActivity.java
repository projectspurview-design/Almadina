package com.example.Pickbyvision.Induvidual_Pick;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.Pickbyvision.R;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeText;
    private ImageView imageView1, imageView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, Barcodescanner.class);
                startActivity(intent);
                finish();
            }
        }, 5000);
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
    private void goBackToTransactionList() {
        Intent intent = new Intent(this, MainActivity.class);


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
