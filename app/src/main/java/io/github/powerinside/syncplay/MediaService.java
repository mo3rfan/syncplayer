package io.github.powerinside.syncplay;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.mitment.syncplay.syncPlayClient;
import com.mitment.syncplay.syncPlayClientInterface;

import java.io.IOException;
import java.util.Map;
import java.util.Stack;

public class MediaService extends Service implements VideoControllerView.MediaPlayerControl {

    final IBinder mBinder = new MediaBinder();
    SimpleExoPlayer mMediaPlayer = null;
    NotificationCompat.Builder nbuilder;
    String server;
    String username;
    String passwd;
    String room;
    DefaultBandwidthMeter bandwidthMeter;
    DataSource.Factory dataSourceFactory;
    ExtractorsFactory extractorsFactory;
    syncPlayClient mSyncPlayClient;
    Handler mHandler;
    ExoControllerView controller;
    ProgressDialog connecting;
    Uri syncplayuri;
    Handler OSDHandler;
    Activity videoPlayer;
    boolean isFullS = false;
    private int NOTIFICATION_ID = 1;
    private UserListDialogFragment userListFragment;
    private FragmentManager fragmentManager;
    private viewMod mViewMod;
    private Handler nothingHandler;
    private Handler userListHandler;
    private Handler exoPlayPauseHandler;


    @Override
    public void onCreate() {
        super.onCreate();
        nbuilder = new NotificationCompat.Builder(MediaService.this);
    }

    @Override
    public void start() {
        mMediaPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        mMediaPlayer.setPlayWhenReady(false);
    }

    @Override
    public int getDuration() {
        return (int) mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return (int) mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
        mSyncPlayClient.seeked();
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.getPlayWhenReady();
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
        return isFullS;
    }

    @Override
    public void toggleFullScreen() {
        if (!isFullS) {
            videoPlayer.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            isFullS = true;
        } else {
            isFullS = false;
            videoPlayer.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {

        bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);

        dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "SyncPlayer"), bandwidthMeter);

        extractorsFactory = new DefaultExtractorsFactory();

        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl();

        SimpleExoPlayer player =
                ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector, loadControl);
        mMediaPlayer = player;

        setCompletionListener();
        nbuilder.setSmallIcon(R.mipmap.ic_launcher);
        return mBinder;
    }

    private void setCompletionListener() {

    }

    public void bindMediaPlayertoController(ExoControllerView vc) {
        controller = vc;
        controller.setPlayer(mMediaPlayer);
    }

    public void setHolder(SurfaceHolder holder) {
    }

    public void setUri(Uri uri) {
        this.syncplayuri = uri;
    }

    public void prepareAsyncSocket(String server, String username, String passwd, String room, Handler mHandler,
                                   ProgressDialog connecting, Activity videoPlayer, Uri syncplayuri
            , Handler OSDHandler) {

        this.mHandler = mHandler;
        this.connecting = connecting;
        this.videoPlayer = videoPlayer;
        this.OSDHandler = OSDHandler;

        if (this.server == null) {
            this.username = username;
            this.passwd = passwd;
            this.room = room;
            this.syncplayuri = syncplayuri;
            this.server = server;
        }
    }

    public void setURL(String url) throws IOException {
        Uri myUri = Uri.parse(url);
        syncplayuri = myUri;
        preparePlayer();
    }

    public void setNothingHandler(Handler handler) {
        nothingHandler = handler;
    }

    public void setUserListHandler(Handler handler) { userListHandler = handler; }
    public void preparePlayer() {
        MediaSource videoSource = new ExtractorMediaSource(syncplayuri,
                dataSourceFactory, extractorsFactory, null, null);
        mMediaPlayer.prepare(videoSource);
        mSyncPlayClient.setPlayerState(new syncPlayClientInterface.playerDetails() {
            @Override
            public long getPosition() {
                return mMediaPlayer.getCurrentPosition() / 1000L;
            }

            @Override
            public Boolean isPaused() {
                return !mMediaPlayer.getPlayWhenReady();
            }
        });
    }

    public void setViewMod(viewMod mViewMod) {
        this.mViewMod = mViewMod;
    }

    syncPlayClientInterface mSyncPlayClientInterface = new syncPlayClientInterface() {
        @Override
        public void debugMessage(String msg) {
            Log.d("SyncPlayer", msg);
        }

        @Override
        public void onConnected(String motd) {
            Log.d("SyncPlayer", "Connected: " + motd);
            mViewMod.progressShow(false);
            nothingHandler.sendEmptyMessage(0);
        }

        @Override
        public void onError(String errMsg) {
            Log.d("SyncPlayer", "Error: " + errMsg);
            mViewMod.progressShow(false);
            mViewMod.userFragmentShow(false);
            stopSelf();
        }

        @Override
        public void onUser(String username, Map<String, Boolean> event, String room) {
            Message msg = new Message();
            String extras = "";
            if (event.containsKey("joined")) {
                extras = " joined";
            }
            if (event.containsKey("left")) {
                extras = " left";
            }
            msg.obj = "User " + username + extras;
            OSDHandler.sendMessage(msg);
            //TODO: Send a "LIST" to refresh userlist.
        }

        @Override
        public void onUser(String setBy, Boolean paused, double position, Boolean doSeek) {
            Message msg1 = new Message();
            msg1.obj = !paused;
            exoPlayPauseHandler.sendMessage(msg1);
            if (doSeek) {
                mMediaPlayer.seekTo((long) position);
            }
            Message msg = new Message();
            String extras = "";
            if (paused) {
                extras = " paused at " + position;
            }
            if (doSeek) {
                extras = " seeks to " + position;
            }
            msg.obj = "User " + setBy + extras;
            OSDHandler.sendMessage(msg);
        }

        @Override
        public void onUserList(Stack<userFileDetails> details) {
            Message msg = new Message();
            msg.obj = details;
            userListHandler.sendMessage(msg);
        }

        @Override
        public void onFileUpdate(syncPlayClientInterface.userFileDetails mFileDetails) {
            nbuilder.setContentText(mFileDetails.getFilename());
        }
    };

    public void executeAsyncSocket() {
        mSyncPlayClient = new syncPlayClient(username, room, this.server, null, mSyncPlayClientInterface);
        Thread thread = new Thread(mSyncPlayClient);
        thread.start();
    }

    public void updateNotification() {
        Intent vidplayer = new Intent(this, videoPlayer.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, vidplayer, 0);
        nbuilder.setContentTitle(getString(R.string.app_name));
        nbuilder.setContentIntent(pi);
        nbuilder.setSmallIcon(R.mipmap.ic_launcher);
        startForeground(NOTIFICATION_ID, nbuilder.build());
    }

    public void setSubtitle(final TextView subtitle) {
        //TODO: Subtitle support
    }

    public void setUserListFragment(UserListDialogFragment mFragment, FragmentManager fM) {
        this.userListFragment = mFragment;
        this.fragmentManager = fM;
    }

    public void setExoPlayPauseHandler(Handler exoPlayPauseHandler) {
        this.exoPlayPauseHandler = exoPlayPauseHandler;
    }

    public class MediaBinder extends Binder {
        MediaService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MediaService.this;
        }
    }
}
