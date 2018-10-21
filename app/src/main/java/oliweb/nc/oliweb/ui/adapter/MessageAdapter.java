package oliweb.nc.oliweb.ui.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.ui.glide.GlideApp;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OWNER = 100;
    private static final int TYPE_CLIENT = 200;

    private List<MessageEntity> messageEntities;
    private String firebaseUserUid;
    private Map<String, UserEntity> mapUrlByUtilisateur;

    public MessageAdapter(String firebaseUserUid) {
        this.messageEntities = new ArrayList<>();
        this.firebaseUserUid = firebaseUserUid;
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
        if (StatusRemote.SEND.equals(model.getStatusRemote()) && model.getTimestamp() != null) {
            Timestamp timestamp = new Timestamp(model.getTimestamp());
            holder.timestamp.setText(DateConverter.simpleUiMessageDateFormat.format(new java.sql.Date(timestamp.getTime())));
        } else {
            holder.timestamp.setText(R.string.not_send);
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
                            && (newMessage.getTimestamp() != null && oldMessage.getTimestamp() != null && newMessage.getTimestamp().equals(oldMessage.getTimestamp())
                            && newMessage.getStatusRemote().equals(oldMessage.getStatusRemote()));
                }
            });
            this.messageEntities = list;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessageEntity messageEntity = messageEntities.get(position);
        return (messageEntity.getUidAuthor().equals(firebaseUserUid)) ? TYPE_OWNER : TYPE_CLIENT;
    }

    @Override
    public int getItemCount() {
        return messageEntities.size();
    }

    private void retreivePhoto(@NonNull MessageViewHolder holder, @NonNull MessageEntity model) {
        if (mapUrlByUtilisateur == null || mapUrlByUtilisateur.isEmpty()) {
            holder.imageAuthor.setImageResource(R.drawable.ic_person_grey_900_48dp);
            return;
        }

        UserEntity userEntity = mapUrlByUtilisateur.get(model.getUidAuthor());
        if (userEntity != null) {
            String urlPhoto = userEntity.getPhotoUrl();
            if (urlPhoto != null && !urlPhoto.isEmpty()) {
                GlideApp.with(holder.imageAuthor)
                        .load(urlPhoto)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person_grey_900_48dp)
                        .error(R.drawable.ic_person_grey_900_48dp)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.imageAuthor);
            } else {
                GlideApp.with(holder.imageAuthor).clear(holder.imageAuthor);
            }
        }
    }

    public void setMapUrlByUtilisateur(Map<String, UserEntity> mapUrlByUtilisateur) {
        this.mapUrlByUtilisateur = mapUrlByUtilisateur;
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
