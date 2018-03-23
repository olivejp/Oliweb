package oliweb.nc.oliweb.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ChatFirebaseAdapter extends FirebaseRecyclerAdapter<ChatFirebase, ChatFirebaseAdapter.ChatFirebaseViewHolder> {

    /**
     * Initialize a {@link RecyclerView.Adapter} that listens to a Firebase query. See
     * {@link FirebaseRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public ChatFirebaseAdapter(@NonNull FirebaseRecyclerOptions<ChatFirebase> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatFirebaseViewHolder holder, int position, @NonNull ChatFirebase model) {
        holder.lastMessage.setText(model.getLastMessage());
        holder.lastMessageTimestamp.setText(Utility.howLongFromNow(model.getUpdateTimestamp()));
    }

    @Override
    public ChatFirebaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_chat_element, parent, false);
        return new ChatFirebaseViewHolder(rootView);
    }

    public static class ChatFirebaseViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.chat_last_message)
        TextView lastMessage;

        @BindView(R.id.chat_last_message_timestamp)
        TextView lastMessageTimestamp;

        @BindView(R.id.chat_author_photo)
        ImageView imagePhotoAuthor;

        public ChatFirebaseViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
