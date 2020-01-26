package com.admin.wifismsgenric;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.root_layout);
        showNumberGetterFragment();
    }

    private void showNumberGetterFragment() {
        getSupportFragmentManager().beginTransaction().add(R.id.root_layout, new NumberGetterFragment())
                .commit();
    }

}
