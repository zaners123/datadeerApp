package net.datadeer.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.datadeer.app.lifestream.TrackerManager;

/**@deprecated yep*/
class MainMenu extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_main);
        startActivity(new Intent(MainMenu.this, DeerView.class));
        startActivity(new Intent(MainMenu.this, TrackerManager.class));

    }
}
