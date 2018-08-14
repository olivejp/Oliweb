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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.ui.glide.GlideApp;

import static oliweb.nc.oliweb.ui.adapter.ChatAdapter.ListItem.TYPE_EVENT;
import static oliweb.nc.oliweb.ui.adapter.ChatAdapter.ListItem.TYPE_HEADER;

/**
 * Created by 2761oli on 23/03/2018.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private View.OnClickListener clickListener;
    private View.OnClickListener popupClickListener;
    private List<ListItem> listChats;
    private String firebaseUserUid;
    private Map<String, UserEntity> mapUrlByUtilisateur;

    public ChatAdapter(@NonNull String firebaseUserUid, View.OnClickListener clickListener, View.OnClickListener popupClickListener) {
        this.clickListener = clickListener;
        this.popupClickListener = popupClickListener;
        this.listChats = new ArrayList<>();
        this.firebaseUserUid = firebaseUserUid;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ListItem.TYPE_HEADER) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_header_element, parent, false);
            return new HeaderViewHolder(rootView);
        } else {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_chat_element, parent, false);
            return new ChatViewHolder(rootView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return listChats.get(position).getType();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        int type = getItemViewType(position);
        if (type == ListItem.TYPE_HEADER) {
            HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
            String titreAnnonce = ((HeaderItem) listChats.get(position)).getTitreAnnonce();
            holder.titre.setText(titreAnnonce);
        } else {
            ChatViewHolder holder = (ChatViewHolder) viewHolder;
            ChatEntity model = ((EventItem) listChats.get(position)).getChatEntity();

            holder.imagePopupMenu.setTag(model);
            holder.constraintLayout.setTag(model.getIdChat());
            holder.lastMessage.setText(model.getLastMessage());

            if (model.getUpdateTimestamp() != null) {
                holder.lastMessageTimestamp.setText(DateConverter.simpleUiMessageDateFormat.format(new Date(model.getUpdateTimestamp())));
            }

            holder.constraintLayout.setOnClickListener(clickListener);
            holder.imagePopupMenu.setOnClickListener(popupClickListener);
            retreivePhoto(holder, model);
        }
    }

    public void setListChats(final List<ListItem> newListChats) {
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
                    if (listChats.get(oldItemPosition).getType() != newListChats.get(newItemPosition).getType())
                        return false;
                    if (TYPE_EVENT == listChats.get(oldItemPosition).getType()) {
                        ChatEntity chatOld = ((EventItem) listChats.get(oldItemPosition)).getChatEntity();
                        ChatEntity chatNew = ((EventItem) newListChats.get(newItemPosition)).getChatEntity();
                        return chatOld.getUidChat().equals(chatNew.getUidChat());
                    }
                    if (TYPE_HEADER == listChats.get(oldItemPosition).getType()) {
                        String titreOld = ((HeaderItem) listChats.get(oldItemPosition)).getTitreAnnonce();
                        String titreNew = ((HeaderItem) newListChats.get(newItemPosition)).getTitreAnnonce();
                        return titreOld.equals(titreNew);
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    if (listChats.get(oldItemPosition).getType() != newListChats.get(newItemPosition).getType())
                        return false;
                    if (TYPE_EVENT == listChats.get(oldItemPosition).getType()) {
                        ChatEntity chatOld = ((EventItem) listChats.get(oldItemPosition)).getChatEntity();
                        ChatEntity chatNew = ((EventItem) newListChats.get(newItemPosition)).getChatEntity();
                        return chatOld.equals(chatNew);
                    }
                    if (TYPE_HEADER == listChats.get(oldItemPosition).getType()) {
                        String titreOld = ((HeaderItem) listChats.get(oldItemPosition)).getTitreAnnonce();
                        String titreNew = ((HeaderItem) newListChats.get(newItemPosition)).getTitreAnnonce();
                        return titreOld.equals(titreNew);
                    }
                    return false;
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

        if (mapUrlByUtilisateur == null || mapUrlByUtilisateur.isEmpty()) {
            holder.imagePhotoAuthor.setImageResource(R.drawable.ic_person_grey_900_48dp);
            return;
        }

        String uidUser = firebaseUserUid;
        UserEntity userEntity = mapUrlByUtilisateur.get(model.getUidBuyer().equals(uidUser) ? model.getUidSeller() : model.getUidBuyer());
        if (userEntity != null) {
            String urlPhoto = userEntity.getPhotoUrl();
            if (urlPhoto != null && !urlPhoto.isEmpty()) {
                GlideApp.with(holder.imagePhotoAuthor)
                        .load(urlPhoto)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person_grey_900_48dp)
                        .error(R.drawable.ic_person_grey_900_48dp)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.imagePhotoAuthor);
            } else {
                GlideApp.with(holder.imagePhotoAuthor).clear(holder.imagePhotoAuthor);
            }
        }

    }

    public void setMapUrlByUtilisateur(Map<String, UserEntity> mapUrlByUtilisateur) {
        this.mapUrlByUtilisateur = mapUrlByUtilisateur;
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.annonce_titre)
        TextView titre;

        HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.chat_last_message)
        TextView lastMessage;

        @BindView(R.id.chat_last_message_timestamp)
        TextView lastMessageTimestamp;

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

    public static abstract class ListItem {

        public static final int TYPE_HEADER = 0;
        public static final int TYPE_EVENT = 1;

        abstract public int getType();
    }

    public static class HeaderItem extends ListItem {

        private String titreAnnonce;

        @Override
        public int getType() {
            return TYPE_HEADER;
        }

        public String getTitreAnnonce() {
            return titreAnnonce;
        }

        public void setTitreAnnonce(String titreAnnonce) {
            this.titreAnnonce = titreAnnonce;
        }
    }

    public static class EventItem extends ListItem {

        private ChatEntity chatEntity;

        @Override
        public int getType() {
            return TYPE_EVENT;
        }

        public ChatEntity getChatEntity() {
            return chatEntity;
        }

        public void setChatEntity(ChatEntity chatEntity) {
            this.chatEntity = chatEntity;
        }
    }
}
