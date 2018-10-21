package oliweb.nc.oliweb.ui.fragment;

import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.ChatAdapter;

import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_COME_FROM_CHAT_FRAGMENT;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.TAG_DETAIL_FRAGMENT;
import static oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel.TypeRechercheChat.PAR_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel.TypeRechercheChat.PAR_UTILISATEUR;

/**
 * Created by 2761oli on 23/03/2018.
 */
public class ListChatFragment extends Fragment {

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
                openAnnonceDetail((ChatEntity) v.getTag());
                return true;
            } else {
                return false;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.chat_popup_menu, popup.getMenu());
        popup.show();
    };

    private View.OnClickListener onClickListener = v -> callListMessage((Long) v.getTag());

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
            viewModel.setTypeRechercheChat(PAR_UTILISATEUR);
        }
    }

    @NonNull
    private List<ChatAdapter.ListItem> prepareListItems(List<ChatEntity> listChats) {
        if (listChats == null || listChats.isEmpty())
            return Collections.emptyList();

        List<String> listTitreDejaTraite = new ArrayList<>();
        List<ChatAdapter.ListItem> listItems = new ArrayList<>();

        for (ChatEntity chatEntity : listChats) {
            if (!listTitreDejaTraite.contains(chatEntity.getTitreAnnonce())) {
                ChatAdapter.HeaderItem header = new ChatAdapter.HeaderItem();
                header.setChatEntity(chatEntity);
                listItems.add(header);
                listTitreDejaTraite.add(chatEntity.getTitreAnnonce());
            }

            ChatAdapter.EventItem eventItem = new ChatAdapter.EventItem();
            eventItem.setChatEntity(chatEntity);
            listItems.add(eventItem);

        }

        return listItems;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_chat, container, false);

        ButterKnife.bind(this, view);

        // Init du Adapter
        ChatAdapter chatAdapter = new ChatAdapter(viewModel.getFirebaseUserUid(), onClickListener, onPopupClickListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(appCompatActivity);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Selon les types de recherche
        if (viewModel.getTypeRechercheChat() == PAR_ANNONCE) {
            viewModel.getChatsByUidAnnonceWithOrderByTitreAnnonce().observe(appCompatActivity, listAnnoncesWithChats ->
                    chatAdapter.setListChats(prepareListItems(listAnnoncesWithChats))
            );
        } else {
            viewModel.getChatsByUidUserWithOrderByTitreAnnonce().observe(appCompatActivity, listAnnoncesWithChats ->
                    chatAdapter.setListChats(prepareListItems(listAnnoncesWithChats))
            );
        }

        // Récupération d'une map avec tous les UID des personnes qui correspondent avec moi.
        viewModel.getLiveDataPhotoUrlUsers().observe(appCompatActivity, mapPhotoUrlByUser -> {
            chatAdapter.setMapUrlByUtilisateur(mapPhotoUrlByUser);
            chatAdapter.notifyDataSetChanged();
        });
        return view;
    }

    private void openAnnonceDetail(ChatEntity chatEntity) {
        viewModel.findLiveFirebaseByUidAnnonce(chatEntity.getUidAnnonce()).observeOnce(annonceDto -> {
            if (annonceDto != null) {
                AnnonceFull annonceFull = AnnonceConverter.convertDtoToAnnonceFull(annonceDto);
                Intent intent = new Intent();
                intent.setClass(appCompatActivity, AnnonceDetailActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(ARG_ANNONCE, annonceFull);
                bundle.putBoolean(ARG_COME_FROM_CHAT_FRAGMENT, true);
                intent.putExtras(bundle);
                startActivity(intent);
            } else {
                Toast.makeText(appCompatActivity, "Oups... cette annonce n'est plus disponible", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void callListMessage(Long idChat) {
        viewModel.rechercheMessageByIdChat(idChat);
        if (getFragmentManager() != null) {
            ListMessageFragment listMessageFragment = new ListMessageFragment();
            if (viewModel.isTwoPane()) {
                appCompatActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_messages, listMessageFragment, TAG_DETAIL_FRAGMENT)
                        .commit();
            } else {
                FragmentTransaction ft = appCompatActivity.getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_from_right, android.R.anim.fade_out, R.anim.slide_in_from_left, android.R.anim.fade_out);
                ft.replace(R.id.frame_chats, listMessageFragment, TAG_DETAIL_FRAGMENT);
                ft.addToBackStack(null);
                ft.commit();
            }
        }
    }
}
