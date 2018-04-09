package oliweb.nc.oliweb.ui.adapter;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.ui.glide.GlideApp;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.Constants.FIREBASE_DB_USER_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ChatFirebaseAdapter extends FirebaseRecyclerAdapter<ChatFirebase, ChatFirebaseAdapter.ChatFirebaseViewHolder> {

    private static final String TAG = ChatFirebaseAdapter.class.getName();

    private View.OnClickListener clickListener;
    private View.OnClickListener popupClickListener;

    /**
     * Initialize a {@link RecyclerView.Adapter} that listens to a Firebase query. See
     * {@link FirebaseRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public ChatFirebaseAdapter(@NonNull FirebaseRecyclerOptions<ChatFirebase> options, View.OnClickListener clickListener, View.OnClickListener popupClickListener) {
        super(options);
        this.clickListener = clickListener;
        this.popupClickListener = popupClickListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatFirebaseViewHolder holder, int position, @NonNull ChatFirebase model) {
        holder.constraintLayout.setTag(model.getUid());
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

    @Override
    public ChatFirebaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_chat_element, parent, false);
        return new ChatFirebaseViewHolder(rootView);
    }

    @Override
    public void onError(@NonNull DatabaseError error) {
        Log.d(TAG, "Grosse erreur dans le ChatFirebaseAdapter");
        super.onError(error);
    }

    private void retreivePhoto(@NonNull ChatFirebaseViewHolder holder, @NonNull ChatFirebase model) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);
        if (model.getUidBuyer().equals(FirebaseAuth.getInstance().getUid())) {
            ref = ref.child(model.getUidSeller());
        } else {
            ref = ref.child(model.getUidBuyer());
        }

        ref.keepSynced(true);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UtilisateurFirebase utilisateurFirebase = dataSnapshot.getValue(UtilisateurFirebase.class);
                if (utilisateurFirebase != null) {
                    GlideApp.with(holder.imagePhotoAuthor)
                            .load(utilisateurFirebase.getPhotoPath())
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.ic_person_grey_900_48dp)
                            .error(R.drawable.ic_error_grey_900_48dp)
                            .into(holder.imagePhotoAuthor);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Do nothing
            }
        });
    }

    public static class ChatFirebaseViewHolder extends RecyclerView.ViewHolder {
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

        ChatFirebaseViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
