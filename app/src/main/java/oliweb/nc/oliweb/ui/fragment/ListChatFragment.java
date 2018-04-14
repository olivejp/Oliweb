package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.ChatFirebaseAdapter;
import oliweb.nc.oliweb.utility.Constants;

import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.TAG_DETAIL_FRAGMENT;
import static oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel.TypeRechercheChat.PAR_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel.TypeRechercheChat.PAR_UTILISATEUR;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_CHATS_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListChatFragment extends Fragment {

    private AppCompatActivity appCompatActivity;
    private DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_CHATS_REF);
    private ChatFirebaseAdapter adapter;

    private MyChatsActivityViewModel viewModel;

    @BindView(R.id.recycler_list_chats)
    RecyclerView recyclerView;

    /**
     * OnClickListener qui ouvrira le détail d'une annonce pour le chat concerné
     */
    private View.OnClickListener onPopupClickListener = v -> {
        PopupMenu popup = new PopupMenu(appCompatActivity, v);
        popup.setOnMenuItemClickListener(item -> {
            int i = item.getItemId();
            if (i == R.id.chat_open_annonce) {
                openAnnonceDetail((ChatFirebase) v.getTag());
                return true;
            } else {
                return false;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.chat_popup_menu, popup.getMenu());
        popup.show();
    };

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(appCompatActivity).get(MyChatsActivityViewModel.class);

        if (viewModel.getTypeRechercheChat() == null) {
            viewModel.rechercheChatByUidUtilisateur(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_chat, container, false);

        ButterKnife.bind(this, view);

        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(appCompatActivity, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        Query query;
        if (viewModel.getTypeRechercheChat() == PAR_ANNONCE) {
            query = chatReference.orderByChild("uidAnnonce").equalTo(viewModel.getSelectedAnnonce().getUUID());
            loadQuery(query);
        } else if (viewModel.getTypeRechercheChat() == PAR_UTILISATEUR) {
            query = chatReference.orderByChild("members/" + viewModel.getSelectedUidUtilisateur()).equalTo(true);
            loadQuery(query);
        }
        return view;
    }

    private void loadQuery(Query query) {
        FirebaseRecyclerOptions<ChatFirebase> options = new FirebaseRecyclerOptions.Builder<ChatFirebase>()
                .setQuery(query, ChatFirebase.class)
                .build();

        adapter = new ChatFirebaseAdapter(options,
                v -> callListMessage((String) v.getTag()),
                onPopupClickListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(appCompatActivity);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    private void openAnnonceDetail(ChatFirebase chatFirebase) {
        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_ANNONCE_REF)
                .child(chatFirebase.getUidAnnonce())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        AnnonceDto annonceDto = dataSnapshot.getValue(AnnonceDto.class);
                        if (annonceDto != null) {
                            AnnoncePhotos annoncePhotos = AnnonceConverter.convertDtoToEntity(annonceDto);
                            Intent intent = new Intent();
                            intent.setClass(appCompatActivity, AnnonceDetailActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putParcelable(ARG_ANNONCE, annoncePhotos);
                            intent.putExtras(bundle);
                            startActivity(intent);
                        } else {
                            Toast.makeText(appCompatActivity, "Oups... cette annonce a été supprimée", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Do nothing
                    }
                });
    }

    private void callListMessage(String uidChat) {
        viewModel.rechercheMessageByUidChat(uidChat);
        if (getFragmentManager() != null) {
            ListMessageFragment listMessageFragment = new ListMessageFragment();
            if (viewModel.isTwoPane()) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_messages, listMessageFragment, TAG_DETAIL_FRAGMENT)
                        .commit();
            } else {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.frame_chats, listMessageFragment, TAG_DETAIL_FRAGMENT)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }
}
