package com.example.carbo.shootymcshootface;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TitleScreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private final Handler mDisplayHandler = new Handler();
    private final Runnable mDisplayRunnable = new Runnable() {
        @Override
        public void run() {
            startDisplayActivity();
        }
    };
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private Button mGoogleVisionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_title_screen);

        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        mGoogleVisionButton = (Button) findViewById(R.id.google_vision_button);

        View.OnClickListener googleVisionButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDisplayActivityGoogleVision();
            }
        };
        mGoogleVisionButton.setOnClickListener(googleVisionButtonClickListener);

        // Set up the user interaction to manually show or hide the system UI.
        /*mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delayedSwitchActivity(100);
            }
        });*/
        mContentView.setOnClickListener(googleVisionButtonClickListener);
    }

    @Override
    protected void onPostCreate(Bundle SavedInstanceState) {
        super.onPostCreate(SavedInstanceState);
        mControlsView.setVisibility(View.GONE);
        hide();
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        Thread hideThread = new Thread(mHidePart2Runnable);
        hideThread.start();
    }

    private void delayedSwitchActivity(int delayMillis) {
        mDisplayHandler.removeCallbacks(mDisplayRunnable);
        mDisplayHandler.postDelayed(mDisplayRunnable, delayMillis);
    }

    private void startDisplayActivityGoogleVision() {
        Intent shootActivityIntent = new Intent(this, ShootActivity.class);
        shootActivityIntent.putExtra(Intent.EXTRA_REFERRER_NAME, "Google_Vision");
        startActivity(shootActivityIntent);
    }

    private void startDisplayActivity(){
        Intent shootActivityIntent = new Intent(this, ShootActivity.class);
        startActivity(shootActivityIntent);
    }
}
