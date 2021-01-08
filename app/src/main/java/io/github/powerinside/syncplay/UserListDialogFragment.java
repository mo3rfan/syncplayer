package io.github.powerinside.syncplay;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mitment.syncplay.syncPlayClientInterface;

import java.util.Stack;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     UserListDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 * <p>You activity (or fragment) needs to implement {@link UserListDialogFragment.Listener}.</p>
 */
public class UserListDialogFragment extends BottomSheetDialogFragment {

    // TODO: Customize parameter argument names
    private static final String ARG_ITEM_COUNT = "user_count";
    private Listener mListener;

    // TODO: Customize parameters
    public static UserListDialogFragment newInstance(Stack<syncPlayClientInterface.userFileDetails> users) {
        final UserListDialogFragment fragment = new UserListDialogFragment();
        final Bundle args = new Bundle();
        if(users != null && users.size() > 0) {
            for (int i = 0; i < users.size(); i++) { //
                try {
                    Bundle user = new Bundle();
                    user.putString("username", users.get(i).getUsername());
                    user.putLong("duration", users.get(i).getDuration());
                    user.putLong("size", users.get(i).getSize());
                    user.putString("filename", users.get(i).getFilename());
                    args.putBundle(String.valueOf(i), user);
                } catch (ArrayIndexOutOfBoundsException outOfBounds) {
                    //
                }
            }
            args.putInt(ARG_ITEM_COUNT, users.size());
            fragment.setArguments(args);
        } else {
            args.putInt(ARG_ITEM_COUNT, 1);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new UserAdapter(getArguments().getInt(ARG_ITEM_COUNT)));
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
        void onUserClicked(int position);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        final TextView userName;
        final TextView duration;
        final TextView fileSize;
        final TextView fileName;

        final TextView userNameLabel;
        final TextView durationLabel;
        final TextView fileSizeLabel;
        final TextView fileNameLabel;

        final ViewGroup userContainer;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            // TODO: Customize the item layout
            super(inflater.inflate(R.layout.fragment_user_list_dialog_item, parent, false));
            userName = (TextView) itemView.findViewById(R.id.userName);
            userNameLabel = (TextView) itemView.findViewById(R.id.userLabel);
            duration = (TextView) itemView.findViewById(R.id.duration);
            durationLabel = (TextView) itemView.findViewById(R.id.durationLabel);
            fileSize = (TextView) itemView.findViewById(R.id.fileSize);
            fileSizeLabel = (TextView) itemView.findViewById(R.id.fileSizeLabel);
            fileName = (TextView) itemView.findViewById(R.id.fileName);
            fileNameLabel = (TextView) itemView.findViewById(R.id.fileNameLabel);

            userContainer = (ViewGroup) itemView.findViewById(R.id.userContainer);

            userName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onUserClicked(getAdapterPosition());
                        dismiss();
                    }
                }
            });
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final int mItemCount;

        UserAdapter(int itemCount) {
            mItemCount = itemCount;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (getArguments().getBundle("0") == null) {

                holder.duration.setText("");
                holder.durationLabel.setText("");
                holder.fileSize.setText("");
                holder.fileSizeLabel.setText("");
                holder.fileName.setText("");
                holder.fileNameLabel.setText("");

                holder.userName.setText("No one else here");
                holder.userNameLabel.setText("");
            } else {
                Bundle user = getArguments().getBundle(String.valueOf(position));
                holder.userName.setText(user.getString("username"));
                if (user.getLong("duration") == 0) {
                    holder.duration.setText("");
                } else {
                    holder.duration.setText(String.valueOf(user.getLong("duration")));
                }
                if (user.getLong("size") == 0) {
                    holder.fileSize.setText("");
                } else {
                    holder.fileSize.setText(String.valueOf(user.getLong("size")));
                }
                holder.fileName.setText(user.getString("filename"));
            }
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }

    }

}
