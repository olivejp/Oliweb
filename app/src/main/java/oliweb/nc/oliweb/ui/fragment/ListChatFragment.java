package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.ChatAdapter;

import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.TAG_DETAIL_FRAGMENT;
import static oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel.TypeRechercheChat.PAR_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel.TypeRechercheChat.PAR_UTILISATEUR;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListChatFragment extends Fragment {

    private static final String TAG = ListChatFragment.class.getName();
    private AppCompatActivity appCompatActivity;

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

    private View.OnClickListener onClickListener = v -> callListMessage((String) v.getTag());

    @Override
    public void onDestroyView() {
        recyclerView.setAdapter(null);
        super.onDestroyView();
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

        // Conditions de garde
        if (viewModel.getTypeRechercheChat() != PAR_ANNONCE && viewModel.getTypeRechercheChat() != PAR_UTILISATEUR) {
            return view;
        }

        // Init du Adapter
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(appCompatActivity, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
        ChatAdapter chatAdapter = new ChatAdapter(onClickListener, onPopupClickListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(appCompatActivity);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Selon les types de recherche
        if (viewModel.getTypeRechercheChat() == PAR_ANNONCE) {
            viewModel.getFirebaseChatsByUidAnnonce().observe(appCompatActivity, chatAdapter::setListChats);
        } else {
            viewModel.getFirebaseChatsByUidUser().observe(appCompatActivity, chatAdapter::setListChats);
        }
        return view;
    }

    private void openAnnonceDetail(ChatFirebase chatFirebase) {
        viewModel.findFirebaseByUidAnnonce(chatFirebase.getUidAnnonce())
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(annonceDto -> {
                    if (annonceDto != null) {
                        AnnoncePhotos annoncePhotos = AnnonceConverter.convertDtoToAnnoncePhotos(annonceDto);
                        Intent intent = new Intent();
                        intent.setClass(appCompatActivity, AnnonceDetailActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(ARG_ANNONCE, annoncePhotos);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    } else {
                        Toast.makeText(appCompatActivity, "Oups... cette annonce n'est plus disponible", Toast.LENGTH_LONG).show();
                    }
                })
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable))
                .subscribe();
    }

    private void callListMessage(String uidChat) {
        viewModel.rechercheMessageByUidChat(uidChat);
        if (getFragmentManager() != null) {
            ListMessageFragment listMessageFragment = new ListMessageFragment();
            if (viewModel.isTwoPane()) {
                appCompatActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_messages, listMessageFragment, TAG_DETAIL_FRAGMENT)
                        .commit();
            } else {
                FragmentTransaction ft = appCompatActivity.getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
                ft.replace(R.id.frame_chats, listMessageFragment, TAG_DETAIL_FRAGMENT);
                ft.addToBackStack(null);
                ft.commit();
            }
        }
    }
}
