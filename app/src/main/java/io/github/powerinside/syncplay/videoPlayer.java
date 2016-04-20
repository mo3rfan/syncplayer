package io.github.powerinside.syncplay;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.appszoom.appszoomsdk.AppsZoom;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.Tracking;

import org.json.JSONException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class videoPlayer extends Activity implements SurfaceHolder.Callback,
        CustomMediaPlayer.OnPreparedListener, VideoControllerView.MediaPlayerControl {
    AsyncSocket asock;
    private SurfaceView videoSurface;
    private VideoControllerView controller;
    private CustomMediaPlayer player;
    private CustomMediaPlayer.OnCompletionListener theEnd;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            asock.getsyncplay().close();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // set notready automatic
        try {
            asock.getsyncplay().set_ready(false, false);
        } catch (NullPointerException e) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // kill socket here
        try {
            asock.getsyncplay().close();
        } catch (NullPointerException e) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tracking.startUsage(this);
        checkForCrashes(); // should these really be here
    }

    @Override
    protected void onPause() {
        Tracking.stopUsage(this);
        mTracker.setScreenName(this.getLocalClassName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // set ready automatic
        try {
            asock.getsyncplay().set_ready(true, true);
        } catch (NullPointerException e) {

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        controller.show();
        final ToggleButton is_ready = (ToggleButton) findViewById(R.id.ready);
        if (is_ready != null) {
            is_ready.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    asock.getsyncplay().set_ready(is_ready.isChecked(), true);
                }
            });
        } else {
            Log.d("SyncPlayer", "Toggle error");
        }
        return false;
    }

    private void checkForCrashes() {
        CrashManager.register(this);
    }
    Tracker mTracker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
           //     WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String mString = (String) msg.obj;
                Toast.makeText(getApplicationContext(), mString, Toast.LENGTH_SHORT).show();
            }
        };

        Intent i = getIntent();

        final Uri syncplayuri = i.getData();
        Bundle mbundle = i.getExtras();
        final String server = mbundle.getString("server");
        final String passwd = mbundle.getString("passwd");
        final String room = mbundle.getString("room");
        final String username = mbundle.getString("username");

        setContentView(R.layout.activity_vid_player);
        videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
        SurfaceHolder videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);

        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        final ProgressDialog connecting = new ProgressDialog(this);
        connecting.setTitle(R.string.progress_connecting);
        connecting.setMessage(getString(R.string.progress_wait));
        connecting.show();

        controller = new VideoControllerView(this);


        player = new CustomMediaPlayer();
        player.setOnPreparedListener(this);

        final TextView subtitle = (TextView) findViewById(R.id.subtitle);

        player.setOnTimedTextListener(new CustomMediaPlayer.OnTimedTextListener() {
            Handler handler = new Handler();
            Runnable clear = new Runnable() {
                @Override
                public void run() {
                    subtitle.setVisibility(View.INVISIBLE);
                }
            };
            @Override
            public void onTimedText(final MediaPlayer mp, final TimedText text) {
                if (text != null) {
                    subtitle.setVisibility(View.VISIBLE);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (subtitle != null) {
                                subtitle.setText(text.getText());
                                if (mp.isPlaying()) {
                                    handler.removeCallbacks(clear);
                                    handler.postDelayed(clear, 3000);
                                }
                            }
                        }
                    });

                }
            }
        });

        try {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(this, syncplayuri);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Display error or close socket?
            finish();
        }

        final InterstitialAd mInterstitialAd;
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad));

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(getString(R.string.testdevice1))
                .addTestDevice(getString(R.string.adtestdevice2))
                .build();

        //TODO: do more ad contigency
        mInterstitialAd.loadAd(adRequest);
/*
        final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
*/
        AppsZoom.start(this);
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();

            }
        });

        theEnd = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                subtitle.setVisibility(View.INVISIBLE);
                Random rand = new Random();
                boolean random_n = rand.nextBoolean();
                if (mInterstitialAd.isLoaded()) {
                    // TODO: mute video view
                    //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    if (random_n)
                        mInterstitialAd.show();
                }
                if (AppsZoom.isAdAvailable()) {
                    if (!random_n)
                        AppsZoom.showAd(videoPlayer.this);
                }
                asock.getsyncplay().set_ready(false, false);
                player.seekTo(0);
            }
        };

        player.setOnCompletionListener(theEnd);

        final TextView osd = (TextView) findViewById(R.id.OSD);
        final Runnable rOSD = new Runnable() {
            @Override
            public void run() {
                osd.setVisibility(View.INVISIBLE);
            }
        };

        Handler OSDHandler = new Handler() {
            // TODO: Make delayed post
            @Override
            public void handleMessage(Message msg) {
                osd.setVisibility(View.VISIBLE);
                osd.setText((String) msg.obj);

                this.removeCallbacks(rOSD);
                this.postDelayed(rOSD, 3000); // TODO: Take delay from xml or sharedprefs
            }
        };


        asock = new AsyncSocket(server, username, passwd,
                room, mHandler, connecting, videoPlayer.this,
                syncplayuri, player, OSDHandler);
        asock.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // l8tr
        super.onSaveInstanceState(outState);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        player.setDisplay(holder);
        player.prepareAsync();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void start() {
        player.start();
    }

    @Override
    public void pause() {
        player.pause();
    }

    @Override
    public int getDuration() {
        return player.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        player.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void toggleFullScreen() {

    }

    // Instead of creating a custom mediaplayer, I could have implemented stuff here?

    @Override
    public void onPrepared(MediaPlayer mp) {
        controller.setMediaPlayer(this);
        controller.setAnchorView((FrameLayout) findViewById(R.id.videoSurfaceContainer));
        //player.start(); // put is ready invok here.
    }

    private class AsyncSocket extends AsyncTask<Void, Void, Void> {
        public syncplaysocket sp;
        String server, username, passwd, room;
        Handler mHandler;
        ProgressDialog connecting;
        Activity activity;
        Uri uri;
        CustomMediaPlayer vv;
        Handler mhandler;

        public AsyncSocket(String server, String username, String passwd,
                           String room, Handler mHandler, ProgressDialog connecting,
                           Activity a, Uri uri, CustomMediaPlayer vv, Handler h) {
            this.server = server;
            this.username = username;
            this.passwd = passwd;
            this.room = room;
            this.mHandler = mHandler;
            this.connecting = connecting;
            this.activity = a;
            this.uri = uri;
            this.vv = vv;
            this.mhandler = h;
        }

        public syncplaysocket getsyncplay() {
            return sp;
        }

        @Override
        protected Void doInBackground(Void... params) {
            AlertDialog.Builder errdialog = new AlertDialog.Builder(videoPlayer.this)
                    .setTitle("Error").setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            try {
                this.sp = new syncplaysocket(server, username, passwd,
                        room, mHandler, connecting, activity, getApplicationContext(),
                        uri, vv, mhandler);
                sp.run();
                sp.set_ready(true, false);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                // Because an image or something was selected.
                errdialog.setMessage(R.string.bad_meta).show();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
