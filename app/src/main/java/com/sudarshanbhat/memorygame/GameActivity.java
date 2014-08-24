package com.sudarshanbhat.memorygame;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class GameActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity_layout);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(new GameHelperFragment(), GameHelperFragment.TAG)
                    .commit();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new GameFragment())
                    .commit();
        }
    }
}
