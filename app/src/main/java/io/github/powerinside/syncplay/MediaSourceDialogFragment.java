package io.github.powerinside.syncplay;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     MediaSourceDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 * <p>You activity (or fragment) needs to implement {@link MediaSourceDialogFragment.Listener}.</p>
 */
public class MediaSourceDialogFragment extends BottomSheetDialogFragment {

    // TODO: Customize parameter argument names
    private static final String ARG_ITEM_COUNT = "item_count";
    private Listener mListener;

    // TODO: Customize parameters
    public static MediaSourceDialogFragment newInstance() {
        final MediaSourceDialogFragment fragment = new MediaSourceDialogFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_source_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(new MediaSourceAdapter(2));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Fragment parent = getParentFragment();
        if (parent != null) {
            mListener = (Listener) parent;
        } else {
            mListener = (Listener) context;
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    public interface Listener {
        void onMediaSourceClicked(int position);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        final ImageButton imageButton;
        final TextView buttonText;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            // TODO: Customize the item layout
            super(inflater.inflate(R.layout.fragment_media_source_dialog_item, parent, false));

            imageButton = itemView.findViewById(R.id.chooseButton);
            buttonText = itemView.findViewById(R.id.chooseButtonText);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onMediaSourceClicked(getAdapterPosition());
                        dismiss();
                    }
                }
            });
        }
    }

    private class MediaSourceAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final int mItemCount;

        MediaSourceAdapter(int itemCount) {
            mItemCount = itemCount;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position == 0) {
                holder.imageButton.setImageResource(R.drawable.globe256);
                holder.buttonText.setText("URL");
            } else if (position == 1) {
                holder.imageButton.setImageResource(R.drawable.folder72);
                holder.buttonText.setText("Local file");
            }
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }
}