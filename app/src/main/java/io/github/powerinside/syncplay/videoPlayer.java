package io.github.powerinside.syncplay;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.Tracking;

import org.json.JSONException;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class videoPlayer extends Activity {
    AsyncSocket asock;
    private View mContentView;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            asock.sp.close();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            asock.sp.close();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tracking.startUsage(this);
        checkForCrashes();
    }

    @Override
    protected void onPause() {
        Tracking.stopUsage(this);
        super.onPause();
    }

    private void checkForCrashes() {
        CrashManager.register(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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


        setContentView(R.layout.activity_video_player);
        mContentView = findViewById(R.id.videoView2);

        final ProgressDialog connecting = new ProgressDialog(this);
        connecting.setTitle(getString(R.string.progress_connecting));
        connecting.setMessage(getString(R.string.progress_wait));
        connecting.show();

        final CustomVideoView videoView = (CustomVideoView) mContentView;
        MediaController mediaController = new MediaController(this);


        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        videoView.setMinimumHeight(dm.heightPixels);
        videoView.setMinimumWidth(dm.widthPixels);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(syncplayuri);


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
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();

            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("Syncplay", "Hit the last point");
                if (mInterstitialAd.isLoaded()) {
                    // TODO: mute video view
                    Log.d("Syncplay", "Interstitial has loaded");
                    //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    mInterstitialAd.show();
                }
            }
        });

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
                syncplayuri, videoView, OSDHandler);
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

    private class AsyncSocket extends AsyncTask<Void, Void, Void> {
        public syncplaysocket sp;
        String server, username, passwd, room;
        Handler mHandler;
        ProgressDialog connecting;
        Activity activity;
        Uri uri;
        CustomVideoView vv;
        Handler mhandler;

        public AsyncSocket(String server, String username, String passwd,
                           String room, Handler mHandler, ProgressDialog connecting,
                           Activity a, Uri uri, CustomVideoView vv, Handler h) {
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

        public syncplay getsyncplay() {
            return sp;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                this.sp = new syncplaysocket(server, username, passwd,
                        room, mHandler, connecting, activity, getApplicationContext(),
                        uri, vv, mhandler);
                sp.run();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
