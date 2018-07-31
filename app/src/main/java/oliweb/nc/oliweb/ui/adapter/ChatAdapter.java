package oliweb.nc.oliweb.ui.adapter;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.ui.glide.GlideApp;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private View.OnClickListener clickListener;
    private View.OnClickListener popupClickListener;
    private List<ChatEntity> listChats;
    private FirebaseUser firebaseUser;

    public ChatAdapter(@NonNull FirebaseUser firebaseUser, View.OnClickListener clickListener, View.OnClickListener popupClickListener) {
        this.clickListener = clickListener;
        this.popupClickListener = popupClickListener;
        this.listChats = new ArrayList<>();
        this.firebaseUser = firebaseUser;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_chat_element, parent, false);
        return new ChatViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        ChatViewHolder holder = (ChatViewHolder) viewHolder;
        ChatEntity model = listChats.get(position);

        holder.imagePopupMenu.setTag(model);
        holder.constraintLayout.setTag(model.getIdChat());
        holder.lastMessage.setText(model.getLastMessage());
        holder.titreAnnonce.setText(model.getTitreAnnonce());

        if (model.getUpdateTimestamp() != null) {
            holder.lastMessageTimestamp.setText(DateConverter.simpleUiMessageDateFormat.format(new Date(model.getUpdateTimestamp())));
        }

        holder.constraintLayout.setOnClickListener(clickListener);
        holder.imagePopupMenu.setOnClickListener(popupClickListener);
        retreivePhoto(holder, model);
    }

    public void setListChats(final List<ChatEntity> newListChats) {
        if (listChats == null) {
            listChats = newListChats;
            notifyItemRangeInserted(0, newListChats.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return listChats.size();
                }

                @Override
                public int getNewListSize() {
                    return newListChats.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return listChats.get(oldItemPosition).getUidChat().equals(newListChats.get(newItemPosition).getUidChat());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    ChatEntity newChat = newListChats.get(newItemPosition);
                    ChatEntity oldChat = listChats.get(oldItemPosition);
                    return newChat.getUidChat().equals(oldChat.getUidChat())
                            && (newChat.getLastMessage() != null && oldChat.getLastMessage() != null && (newChat.getLastMessage().equals(oldChat.getLastMessage())))
                            && ((newChat.getUpdateTimestamp() != null && oldChat.getUpdateTimestamp() != null) && (newChat.getUpdateTimestamp().equals(oldChat.getUpdateTimestamp())));
                }
            });
            this.listChats = newListChats;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemCount() {
        return listChats.size();
    }

    private void retreivePhoto(@NonNull ChatViewHolder holder, @NonNull ChatEntity model) {
        String uidUser = firebaseUser.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);
        if (model.getUidBuyer().equals(uidUser)) {
            ref = ref.child(model.getUidSeller());
        } else {
            ref = ref.child(model.getUidBuyer());
        }
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UtilisateurEntity user = dataSnapshot.getValue(UtilisateurEntity.class);
                if (user != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                    if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                        GlideApp.with(holder.imagePhotoAuthor)
                                .load(user.getPhotoUrl())
                                .circleCrop()
                                .placeholder(R.drawable.ic_person_grey_900_48dp)
                                .error(R.drawable.ic_error_grey_900_48dp)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(holder.imagePhotoAuthor);
                    } else {
                        GlideApp.with(holder.imagePhotoAuthor).clear(holder.imagePhotoAuthor);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Do nothing
            }
        });
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.chat_last_message)
        TextView lastMessage;

        @BindView(R.id.chat_last_message_timestamp)
        TextView lastMessageTimestamp;

        @BindView(R.id.chat_titre_annonce)
        TextView titreAnnonce;

        @BindView(R.id.chat_author_photo)
        ImageView imagePhotoAuthor;

        @BindView(R.id.constraint_chat)
        ConstraintLayout constraintLayout;

        @BindView(R.id.chat_popup_menu)
        ImageView imagePopupMenu;

        ChatViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
