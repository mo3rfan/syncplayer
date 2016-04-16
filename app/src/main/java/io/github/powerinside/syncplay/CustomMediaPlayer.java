package io.github.powerinside.syncplay;

import android.media.MediaPlayer;

public class CustomMediaPlayer extends MediaPlayer {

    public boolean attached = false;
    private MediaPlayerListener mListener;

    public void setMediaPlayerListener(MediaPlayerListener listener) {
        mListener = listener;
    }

    @Override
    public void start() throws IllegalStateException {
        if (attached)
            super.start();
        if (mListener != null) {
            mListener.onPlay();
        }
    }

    @Override
    public void pause() throws IllegalStateException {
        if (attached)
            super.pause();
        if (mListener != null) {
            mListener.onPause();
        }
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        if (attached)
            super.seekTo(msec);
        if (mListener != null) {
            mListener.onTimeBarSeekChanged(msec);
        }
    }

    public interface MediaPlayerListener {
        void onPlay();

        void onPause();

        void onTimeBarSeekChanged(int currentTime);
    }

}
