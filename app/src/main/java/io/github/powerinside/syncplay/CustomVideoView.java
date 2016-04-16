package io.github.powerinside.syncplay;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by irfan on 4/13/16.
 */
public class CustomVideoView extends VideoView {
    private VideoViewListener mListener;

    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setVideoViewListener(VideoViewListener listener) {
        mListener = listener;
    }

    @Override
    public void seekTo(int msec) {
        super.seekTo(msec);
        if (mListener != null) {
            mListener.onTimeBarSeekChanged(msec);
        }
    }

    @Override
    public void start() {
        super.start();
        if (mListener != null) {
            mListener.onPlay();
        }
    }

    @Override
    public void pause() {
        super.pause();
        if (mListener != null) {
            mListener.onPause();
        }
    }

    public interface VideoViewListener {
        void onPlay();

        void onPause();

        void onTimeBarSeekChanged(int currentTime);
    }
}
