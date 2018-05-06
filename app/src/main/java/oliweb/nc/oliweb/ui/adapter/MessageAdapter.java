package oliweb.nc.oliweb.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.ui.glide.GlideApp;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = MessageAdapter.class.getName();

    private static final int TYPE_OWNER = 100;
    private static final int TYPE_CLIENT = 200;

    private List<MessageEntity> messageEntities;

    public MessageAdapter() {
        this.messageEntities = new ArrayList<>();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View rootView;
        if (viewType == TYPE_OWNER) {
            rootView = inflater.inflate(R.layout.adapter_message_element_owner, parent, false);
        } else {
            rootView = inflater.inflate(R.layout.adapter_message_element_client, parent, false);
        }
        return new MessageViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        MessageViewHolder holder = (MessageViewHolder) viewHolder;
        MessageEntity model = messageEntities.get(position);
        holder.message.setText(model.getMessage());
        if (model.getTimestamp() != null) {
            Timestamp timestamp = new Timestamp(model.getTimestamp());
            holder.timestamp.setText(DateConverter.simpleUiMessageDateFormat.format(new java.sql.Date(timestamp.getTime())));
        }
        retreivePhoto(holder, model);
    }

    public void setMessageEntities(final List<MessageEntity> list) {
        if (messageEntities == null) {
            messageEntities = list;
            notifyItemRangeInserted(0, list.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return messageEntities.size();
                }

                @Override
                public int getNewListSize() {
                    return list.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return messageEntities.get(oldItemPosition).getIdMessage().equals(list.get(newItemPosition).getIdMessage());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    MessageEntity newMessage = list.get(newItemPosition);
                    MessageEntity oldMessage = messageEntities.get(oldItemPosition);
                    return newMessage.getMessage().equals(oldMessage.getMessage())
                            && newMessage.getUidAuthor().equals(oldMessage.getUidAuthor())
                            && (newMessage.getTimestamp() != null && oldMessage.getTimestamp() != null && newMessage.getTimestamp().equals(oldMessage.getTimestamp()));
                }
            });
            this.messageEntities = list;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessageEntity messageEntity = messageEntities.get(position);
        return (messageEntity.getUidAuthor().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) ? TYPE_OWNER : TYPE_CLIENT;
    }

    @Override
    public int getItemCount() {
        return messageEntities.size();
    }

    private void retreivePhoto(@NonNull MessageViewHolder holder, @NonNull MessageEntity model) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);
        ref.child(model.getUidAuthor()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UtilisateurEntity utilisateurFirebase = dataSnapshot.getValue(UtilisateurEntity.class);
                if (utilisateurFirebase != null) {
                    GlideApp.with(holder.imageAuthor)
                            .load(utilisateurFirebase.getPhotoUrl())
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.ic_person_grey_900_48dp)
                            .error(R.drawable.ic_error_grey_900_48dp)
                            .into(holder.imageAuthor);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Do nothing
            }
        });
    }


    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message_author_photo)
        ImageView imageAuthor;

        @BindView(R.id.message_message)
        TextView message;

        @BindView(R.id.message_timestamp)
        TextView timestamp;

        @BindView(R.id.cardview_message)
        CardView cardview;

        MessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
