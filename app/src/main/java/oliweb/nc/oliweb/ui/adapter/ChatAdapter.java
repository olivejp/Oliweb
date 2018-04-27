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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.ui.glide.GlideApp;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = ChatAdapter.class.getName();

    private View.OnClickListener clickListener;
    private View.OnClickListener popupClickListener;
    private List<ChatEntity> listChats;

    public ChatAdapter(View.OnClickListener clickListener, View.OnClickListener popupClickListener) {
        this.clickListener = clickListener;
        this.popupClickListener = popupClickListener;
        this.listChats = new ArrayList<>();
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
        holder.constraintLayout.setTag(model.getUidChat());
        holder.lastMessage.setText(model.getLastMessage());
        holder.lastMessageTimestamp.setText(DateConverter.simpleUiMessageDateFormat.format(new Date(model.getUpdateTimestamp())));
        holder.constraintLayout.setOnClickListener(clickListener);
        holder.imagePopupMenu.setOnClickListener(popupClickListener);
        holder.imagePopupMenu.setTag(model);

        FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF).child(model.getUidAnnonce()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                AnnonceDto annonceDto = dataSnapshot.getValue(AnnonceDto.class);
                if (annonceDto != null) {
                    holder.titreAnnonce.setText(annonceDto.getTitre());
                    holder.prixAnnonce.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annonceDto.getPrix()) + " XPF"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Do nothing
            }
        });
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
                            && newChat.getLastMessage().equals(oldChat.getLastMessage())
                            && newChat.getUpdateTimestamp() == (oldChat.getUpdateTimestamp());
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
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);
        if (model.getUidBuyer().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            ref = ref.child(model.getUidSeller());
        } else {
            ref = ref.child(model.getUidBuyer());
        }
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UtilisateurFirebase utilisateurFirebase = dataSnapshot.getValue(UtilisateurFirebase.class);
                if (utilisateurFirebase != null && utilisateurFirebase.getPhotoPath() != null && !utilisateurFirebase.getPhotoPath().isEmpty()) {
                    if (utilisateurFirebase.getPhotoPath() != null && !utilisateurFirebase.getPhotoPath().isEmpty()) {
                        GlideApp.with(holder.imagePhotoAuthor)
                                .load(utilisateurFirebase.getPhotoPath())
                                .circleCrop()
                                .placeholder(R.drawable.ic_person_grey_900_48dp)
                                .error(R.drawable.ic_error_grey_900_48dp)
                                .into(holder.imagePhotoAuthor);
                    } else {
                        GlideApp.with(holder.imagePhotoAuthor).clear(holder.imagePhotoAuthor);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Do nothing
            }
        });
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.chat_last_message)
        TextView lastMessage;

        @BindView(R.id.chat_last_message_timestamp)
        TextView lastMessageTimestamp;

        @BindView(R.id.chat_prix_annonce)
        TextView prixAnnonce;

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
