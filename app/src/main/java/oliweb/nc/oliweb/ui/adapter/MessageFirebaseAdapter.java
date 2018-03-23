package oliweb.nc.oliweb.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import oliweb.nc.oliweb.firebase.dto.MessageFirebase;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class MessageFirebaseAdapter extends FirebaseRecyclerAdapter<MessageFirebase, MessageFirebaseAdapter.MessageFirebaseViewHolder> {

    /**
     * Initialize a {@link RecyclerView.Adapter} that listens to a Firebase query. See
     * {@link FirebaseRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public MessageFirebaseAdapter(@NonNull FirebaseRecyclerOptions<MessageFirebase> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull MessageFirebaseViewHolder holder, int position, @NonNull MessageFirebase model) {

    }

    @Override
    public MessageFirebaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    public static class MessageFirebaseViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView nameText;

        public MessageFirebaseViewHolder(View itemView) {
            super(itemView);
            nameText = (TextView) itemView.findViewById(android.R.id.text1);
            messageText = (TextView) itemView.findViewById(android.R.id.text2);
        }
    }
}
