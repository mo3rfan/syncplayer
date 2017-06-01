package io.github.powerinside.syncplay;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.exoplayer2.ui.PlaybackControlView;


public class ExoControllerView extends PlaybackControlView {
    UserListDialogFragment mBottomSlideFragment;
    FragmentManager mFragmentManager;
    MediaSourceDialogFragment mMediaSourceDialogFragment;
    private ImageButton mUserListButton;
    private ImageButton mFullscreenButton;
    private View mPauseButton;
    private ImageButton mOpenDialogButton;
    private View mFfwdButton;

    public ExoControllerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExoControllerView(Context context) {
        super(context);
    }

    public ExoControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setBottomSlideFragment(UserListDialogFragment fragment, MediaSourceDialogFragment mSDF,
                                       FragmentManager fManager) {
        mBottomSlideFragment = fragment;
        mFragmentManager = fManager;
        if (mSDF != null) {
            mMediaSourceDialogFragment = mSDF;
        }
        setStuff();
    }

    private void setStuff() {
        mUserListButton = findViewById(R.id.userListButton);
        if (mUserListButton != null) {
            mUserListButton.requestFocus();
            mUserListButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBottomSlideFragment.show(mFragmentManager, getContext().getString(R.string.userList));
                }
            });
        }
        mOpenDialogButton = findViewById(R.id.openDialogButton);
        if (mOpenDialogButton != null) {
            mOpenDialogButton.requestFocus();
            mOpenDialogButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    mMediaSourceDialogFragment.show(mFragmentManager, "pick-media");
                }
            });
        }
    }
}
