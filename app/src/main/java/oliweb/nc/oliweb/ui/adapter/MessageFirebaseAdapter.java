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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_USER_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class MessageFirebaseAdapter extends FirebaseRecyclerAdapter<MessageFirebase, MessageFirebaseAdapter.MessageFirebaseViewHolder> {

    private static final String TAG = MessageFirebaseAdapter.class.getName();

    public MessageFirebaseAdapter(@NonNull FirebaseRecyclerOptions<MessageFirebase> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull MessageFirebaseViewHolder holder, int position, @NonNull MessageFirebase model) {
        holder.message.setText(model.getMessage());
        holder.timestamp.setText(Utility.howLongFromNow(model.getTimestamp()));
        retreivePhoto(holder, model);
    }

    @Override
    public MessageFirebaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_message_list, parent, false);
        return new MessageFirebaseAdapter.MessageFirebaseViewHolder(rootView);
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

        @BindView(R.id.message_last_message)
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
