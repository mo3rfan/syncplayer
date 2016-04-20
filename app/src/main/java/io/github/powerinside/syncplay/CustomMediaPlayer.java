package io.github.powerinside.syncplay;

import android.media.MediaPlayer;
import android.widget.MediaController;

/**
 * Created by irfan on 4/20/16.
 */
public class CustomMediaPlayer extends MediaPlayer {

    private MediaPlayerListener mListener;

    public void setMediaPlayerListener(MediaPlayerListener listener) {
        mListener = listener;
    }

    public interface MediaPlayerListener {
        void onPlay();
        void onPause();
        void onTimeBarSeekChanged(int currentTime);
    }
    @Override
    public void start() throws IllegalStateException {
        super.start();
        if (mListener != null) {
            mListener.onPlay();
        }
    }

    @Override
    public void pause() throws IllegalStateException {
        super.pause();
        if (mListener != null) {
            mListener.onPause();
        }
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        super.seekTo(msec);
        if (mListener != null) {
            mListener.onTimeBarSeekChanged(msec);
        }
    }
}
