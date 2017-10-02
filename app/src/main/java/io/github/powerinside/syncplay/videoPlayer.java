package io.github.powerinside.syncplay;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.mitment.syncplay.syncPlayClientInterface;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

public class videoPlayer extends FragmentActivity implements SurfaceHolder.Callback,
        UserListDialogFragment.Listener, MediaSourceDialogFragment.Listener {
    private Handler nothingHandler;

    String server;
    private int REQUEST_TAKE_GALLERY_VIDEO = 1;
    private static final int READ_EXTERNAL_STORAGE_FOR_MEDIA = 2;
    private static final int REQUEST_INVITE = 1;
    private Intent pickerProvider;
    private Handler userListHandler;
    private boolean isFullS;
    private ToggleButton is_ready;

    @Override
    public void onMediaSourceClicked(int position) {
        if (position == 0) {
            final EditText txtUrl = new EditText(this);
            txtUrl.setHint("http://www.example.com/somefile.mp4");
            final Context thisContext = this;
            new AlertDialog.Builder(this)
                    .setTitle("Enter video URL")
                    .setMessage("Enter a URL to a video file")
                    .setView(txtUrl)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String url = txtUrl.getText().toString();
                            try {
                                URL theUrl = new URL(url);
                                if (theUrl.getHost().toLowerCase().equals("youtube.com")) {
                                    new AlertDialog.Builder(thisContext)
                                            .setTitle("Sorry")
                                            .setMessage("Youtube links are not supported.")
                                            .setPositiveButton("ok", null)
                                            .show();
                                } else {
                                    // all good, load up.
                                        Uri androidUri = new Uri.Builder()
                                                .scheme(theUrl.getProtocol())
                                                .authority(theUrl.getHost())
                                                .appendEncodedPath(theUrl.getPath().substring(1))
                                                .build();
                                    try {
                                        mService.setURL(androidUri.toString());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    final URL uri=new URL(androidUri.toString());

                                    class getRemoteFileStuff extends AsyncTask<URL, Void, Void> {

                                        @Override
                                        protected Void doInBackground(URL... urls) {
                                            try
                                            {
                                                HttpURLConnection ucon;
                                                ucon= (HttpURLConnection) urls[0].openConnection();
                                                ucon.connect();
                                                final String contentLengthStr=ucon.getHeaderField("Content-Length");
                                                long size;
                                                try {
                                                    size = (long) Long.parseLong(contentLengthStr);
                                                } catch (NumberFormatException e) {
                                                    size = 0;
                                                }
                                                /**
                                                 * For accurately estimating duration of online videos,
                                                 * the below library could be useful...
                                                 * https://github.com/wseemann/FFmpegMediaMetadataRetriever
                                                 */
                                                if (ucon.getResponseCode() == 200) {
                                                    mService.mSyncPlayClient.set_file(0, size, urls[0].toString());
                                                } else {
                                                    Toast.makeText(getApplicationContext(), "URL can't be loaded!",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            catch(final IOException e1)
                                            {
                                            }
                                            return null;
                                        }
                                    }
                                    new getRemoteFileStuff().execute(uri);


                                }
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                                    new AlertDialog.Builder(thisContext)
                                            .setTitle("Invalid URL!")
                                            .setMessage(e.getMessage())
                                            .setPositiveButton("ok", null)
                                            .show();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
        } else if (position == 1) {
            pickerProvider = new Intent()
                    .setType("video/*, audio/*")
                    .setAction(Intent.ACTION_GET_CONTENT);
            int permissionCheck = ContextCompat.checkSelfPermission(videoPlayer.this, android.Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(Intent.createChooser(pickerProvider, "Pick a video"), REQUEST_TAKE_GALLERY_VIDEO);
            } else {

                        Toast.makeText(videoPlayer.this, "Allow the 'read external storage' permission so we can open up the file picker to choose a media file to syncplay", Toast.LENGTH_SHORT).show();
                        if (ActivityCompat.shouldShowRequestPermissionRationale(videoPlayer.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            Toast.makeText(videoPlayer.this, "Allow the 'read external storage' permission so we can open up the file picker to choose a media file to syncplay", Toast.LENGTH_SHORT).show();
                            ActivityCompat.requestPermissions(videoPlayer.this,
                                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                    1);
                        } else {
                            ActivityCompat.requestPermissions(videoPlayer.this,
                                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                    READ_EXTERNAL_STORAGE_FOR_MEDIA);
                        }
            }
        }
    }

    Handler mHandler;
    String passwd;
    String room;
    String username;
    Handler OSDHandler;
    Handler exoPlayPauseHandler;
    ProgressDialog connecting;
    boolean mBound = false;
    MediaService mService;
    ServiceConnection mConnection;
    private SurfaceView videoSurface;
    private TextView subtitle;
    private SurfaceHolder surfaceholder;
    private ExoControllerView controller;
    TextView nothingLoaded;

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        mService.mSyncPlayClient.disconnect();
        if (connecting != null) {
            connecting.dismiss();
        }
        stopService(new Intent(this, MediaService.class));
        if (mConnection != null) {
            unbindService(mConnection);
        }
        mConnection = null;
        try {
            mService.mSyncPlayClient.disconnect();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUserClicked(int position) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mBound) {
            if (mService.mMediaPlayer != null)
                mService.mMediaPlayer.release();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBound) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBound) {
            mService.setHolder(null);
            mService.updateNotification();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        Bundle mbundle = i.getExtras();
        Boolean skipOnCreate = false;

        try {
            server = mbundle.getString("server");
            passwd = mbundle.getString("passwd");
            room = mbundle.getString("room");
            username = mbundle.getString("username");
        } catch (NullPointerException e) {
            skipOnCreate = true;
        }

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String mString = (String) msg.obj;
                Toast.makeText(getApplicationContext(), mString, Toast.LENGTH_SHORT).show();
            }
        };
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_vid_player);
        nothingLoaded = (TextView) findViewById(R.id.nothingOpenedText);
        is_ready = (ToggleButton) findViewById(R.id.ready);
        nothingLoaded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!controller.isShown()) {
                    controller.show();
                } else {
                    controller.hide();
                }
            }
        });

        if (is_ready != null) {
            is_ready.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mService.mSyncPlayClient.setReady(isChecked);
                }
            });
        }

        ImageButton fullScn = (ImageButton) findViewById(R.id.fullscreen);
        fullScn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFullS) {
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                    isFullS = true;
                    ((ImageButton) view).setImageResource(R.drawable.ic_media_fullscreen_shrink);
                } else {
                    isFullS = false;
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN);
                    ((ImageButton) view).setImageResource(R.drawable.ic_media_fullscreen_stretch);
                }
            }
        });
        final UserListDialogFragment mFragment = UserListDialogFragment.newInstance(null);
        final MediaSourceDialogFragment mMediaSourceFragment = MediaSourceDialogFragment.newInstance();
        controller = (ExoControllerView) findViewById(R.id.exo_controller_view1);
        controller.setBottomSlideFragment(mFragment, mMediaSourceFragment,
                getSupportFragmentManager());
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl();

        final SimpleExoPlayer player =
                ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

        videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
        SurfaceHolder videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);

        final TextView osd = (TextView) findViewById(R.id.OSD);
        final Runnable rOSD = new Runnable() {
            @Override
            public void run() {
                osd.setVisibility(View.INVISIBLE);
            }
        };

        OSDHandler = new Handler() {
            // TODO: Make delayed post
            @Override
            public void handleMessage(Message msg) {
                osd.setVisibility(View.VISIBLE);
                osd.setText((String) msg.obj);

                this.removeCallbacks(rOSD);
                this.postDelayed(rOSD, 3000); // TODO: Take delay from xml or sharedprefs
            }
        };

        exoPlayPauseHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                player.setPlayWhenReady((Boolean) msg.obj);
            }
        };

        nothingHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                nothingLoaded.setVisibility(View.VISIBLE);
            }
        };
        subtitle = (TextView) findViewById(R.id.subtitle);
        subtitle.setText("");

        userListHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Stack<syncPlayClientInterface.userFileDetails> details =
                        (Stack<syncPlayClientInterface.userFileDetails>) msg.obj;
                if (details.size() > 0) {
                    final UserListDialogFragment mFragment = UserListDialogFragment.newInstance(details);
                    controller.setBottomSlideFragment(mFragment, null,
                            getSupportFragmentManager()); //or set
                }
            }
        };

        final Boolean finalSkipOnCreate = skipOnCreate;
        final viewMod mViewMod = new viewMod() {
            @Override
            public void userFragmentShow(Boolean flag) {
                if (flag) {
                    mFragment.show(getSupportFragmentManager(), "user-list");
                }
            }

            @Override
            public void progressShow(Boolean flag) {
                if (flag) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connecting.show();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connecting.hide();
                        }
                    });
                }
            }
        };
        ServiceConnection mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                MediaService.MediaBinder binder = (MediaService.MediaBinder) service;
                mService = binder.getService();
                mBound = true;
                mService.setSubtitle(subtitle);
                mService.setHolder(surfaceholder);
                mService.setUserListFragment(mFragment, getSupportFragmentManager());
                mService.setViewMod(mViewMod);
                mService.setNothingHandler(nothingHandler);
                mService.setUserListHandler(userListHandler);
                mService.setExoPlayPauseHandler(exoPlayPauseHandler);
                mService.prepareAsyncSocket(server, username, passwd, room, mHandler, connecting,
                        videoPlayer.this, null, OSDHandler);
                if (!finalSkipOnCreate) {
                    mService.executeAsyncSocket();
                }
                mService.bindMediaPlayertoController(controller);
                mService.mMediaPlayer.setVideoListener(new SimpleExoPlayer.VideoListener() {
                    @Override
                    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                    }

                    @Override
                    public void onRenderedFirstFrame() {
                        nothingLoaded.setVisibility(View.GONE);
                        is_ready.setEnabled(true);
                    }
                });
                mService.mMediaPlayer.addListener(new ExoPlayer.EventListener() {
                    @Override
                    public void onTimelineChanged(Timeline timeline, Object manifest) {

                    }

                    @Override
                    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

                    }

                    @Override
                    public void onLoadingChanged(boolean isLoading) {

                    }

                    @Override
                    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

                    }

                    /**
                     * Called when the value of {@link #getRepeatMode()} changes.
                     *
                     * @param repeatMode The {@link RepeatMode} used for playback.
                     */

                    @Override
                    public void onRepeatModeChanged(int repeatMode) {

                    }


                    @Override
                    public void onPlayerError(ExoPlaybackException error) {

                    }

                    @Override
                    public void onPositionDiscontinuity() {
                        //This will have to do for "seek"
                        mService.mSyncPlayClient.seeked();
                    }

                    /**
                     * Called when the current playback parameters change. The playback parameters may change due to
                     * a call to {@link ExoPlayer#setPlaybackParameters(PlaybackParameters)}, or the player itself
                     * may change them (for example, if audio playback switches to passthrough mode, where speed
                     * adjustment is no longer possible).
                     *
                     * @param playbackParameters The playback parameters.
                     */
                    @Override
                    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
                mService.mMediaPlayer.release();
            }
        };

        Intent intent = new Intent(this, MediaService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if (!skipOnCreate) {
            connecting = new ProgressDialog(this);
            connecting.setTitle(R.string.progress_connecting);
            connecting.setMessage(getString(R.string.progress_wait));
            connecting.show();
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!controller.isShown()) {
            controller.show();
        }
        return false;
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
        surfaceholder = holder;
        if (mBound) {
            mService.setHolder(holder);
            mService.mMediaPlayer.setVideoSurfaceHolder(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mBound && mService != null) {
            if (mService.mMediaPlayer != null) {
                mService.mMediaPlayer.setVideoSurfaceHolder(holder);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    protected String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Uri selectedFile = data.getData();
                if (selectedFile != null) {
                    mService.setUri(selectedFile);

                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(getApplicationContext(), selectedFile);
                    String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long duration = Long.parseLong(time );

                    int size = 0;
                    Cursor cursor = getContentResolver()
                            .query(selectedFile, null, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        size = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (!cursor.isNull(size)) {
                            // Technically the column stores an int, but cursor.getString()
                            // will do the conversion automatically.
                            size = cursor.getInt(size);
                        }
                    }
                    mService.mSyncPlayClient.set_file(duration / 1000, size, getFileName(selectedFile));
                    mService.preparePlayer();
                } else if (resultCode != REQUEST_INVITE) {
                    Toast.makeText(this, "Error loading the file.", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case READ_EXTERNAL_STORAGE_FOR_MEDIA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(Intent.createChooser(pickerProvider, "Pick a video"), REQUEST_TAKE_GALLERY_VIDEO);
                }
            }
        }
    }
}
