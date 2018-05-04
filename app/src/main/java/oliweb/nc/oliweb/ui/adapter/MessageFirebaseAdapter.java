package oliweb.nc.oliweb.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
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

import java.sql.Date;
import java.sql.Timestamp;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.ui.glide.GlideApp;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_USER_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class MessageFirebaseAdapter extends FirebaseRecyclerAdapter<MessageFirebase, MessageFirebaseAdapter.MessageFirebaseViewHolder> {

    private static final String TAG = MessageFirebaseAdapter.class.getName();

    private static final int TYPE_OWNER = 100;
    private static final int TYPE_CLIENT = 200;

    public MessageFirebaseAdapter(@NonNull FirebaseRecyclerOptions<MessageFirebase> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull MessageFirebaseViewHolder holder, int position, @NonNull MessageFirebase model) {
        holder.message.setText(model.getMessage());
        Timestamp timestamp = new Timestamp(model.getTimestamp());
        holder.timestamp.setText(DateConverter.simpleUiMessageDateFormat.format(new Date(timestamp.getTime())));
        retreivePhoto(holder, model);
    }

    @Override
    public MessageFirebaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View rootView;
        if (viewType == TYPE_OWNER) {
            rootView = inflater.inflate(R.layout.adapter_message_element_owner, parent, false);
        } else {
            rootView = inflater.inflate(R.layout.adapter_message_element_client, parent, false);
        }
        return new MessageFirebaseAdapter.MessageFirebaseViewHolder(rootView);
    }

    @Override
    public int getItemViewType(int position) {
        MessageFirebase messageFirebase = getItem(position);
        return (messageFirebase.getUidAuthor().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) ? TYPE_OWNER : TYPE_CLIENT;
    }

    @Override
    public void onError(@NonNull DatabaseError error) {
        Log.d(TAG, "Grosse erreur dans le ChatFirebaseAdapter");
        super.onError(error);
    }

    private void retreivePhoto(@NonNull MessageFirebaseAdapter.MessageFirebaseViewHolder holder, @NonNull MessageFirebase model) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_USER_REF);
        ref.child(model.getUidAuthor()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UtilisateurFirebase utilisateurFirebase = dataSnapshot.getValue(UtilisateurFirebase.class);
                if (utilisateurFirebase != null) {
                    GlideApp.with(holder.imageAuthor)
                            .load(utilisateurFirebase.getPhotoPath())
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

    public static class MessageFirebaseViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message_author_photo)
        ImageView imageAuthor;

        @BindView(R.id.message_message)
        TextView message;

        @BindView(R.id.message_timestamp)
        TextView timestamp;

        @BindView(R.id.cardview_message)
        CardView cardview;

        public MessageFirebaseViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}