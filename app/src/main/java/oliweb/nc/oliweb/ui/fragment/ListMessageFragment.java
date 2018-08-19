package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.MessageAdapter;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by 2761oli on 23/03/2018.
 */
public class ListMessageFragment extends Fragment {
    private static final String TAG = ListMessageFragment.class.getName();

    private AppCompatActivity appCompatActivity;
    private MessageAdapter adapter;
    private MyChatsActivityViewModel viewModel;
    private boolean initializeAdapterLater = false;

    @BindView(R.id.recycler_list_message)
    RecyclerView recyclerView;

    @BindView(R.id.text_to_send)
    EditText textToSend;

    @BindView(R.id.text_empty_list)
    TextView textViewEmpty;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onDestroyView() {
        Utility.hideKeyboard(appCompatActivity);
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(appCompatActivity).get(MyChatsActivityViewModel.class);
        viewModel.getPhotoUrlsByUidUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_message, container, false);

        ButterKnife.bind(this, view);

        // Init recyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(appCompatActivity);
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MessageAdapter(viewModel.getFirebaseUserUid());
        recyclerView.setAdapter(adapter);

        // Sur l'action du message, on tente d'envoyer le texte
        textToSend.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                clickOnSendButton(textToSend);
                return true;
            }
            return false;
        });

        if (viewModel.getTypeRechercheMessage() == MyChatsActivityViewModel.TypeRechercheMessage.PAR_ANNONCE) {
            viewModel.findLiveChatByUidUserAndUidAnnonce().observeOnce(chatEntity -> {
                if (chatEntity != null) {
                    initializeAdapterByIdChat(chatEntity.getIdChat());
                } else {
                    initializeList(true);
                    initializeAdapterLater = true;
                }
            });
        } else if (viewModel.getTypeRechercheMessage() == MyChatsActivityViewModel.TypeRechercheMessage.PAR_CHAT) {
            initializeAdapterByIdChat(viewModel.getSearchedIdChat());
        }

        return view;
    }

    private void initializeList(boolean listIsEmpty) {
        textViewEmpty.setVisibility(listIsEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(listIsEmpty ? View.GONE : View.VISIBLE);
    }

    private void initializeAdapterByIdChat(Long idChat) {
        initializeAdapterLater = false;
        viewModel.findAllMessageByIdChat(idChat).observe(appCompatActivity, listMessages -> {
            boolean emptyList = listMessages == null || listMessages.isEmpty();
            initializeList(emptyList);
            if (!emptyList) adapter.setMessageEntities(listMessages);
        });

        viewModel.getLiveDataPhotoUrlUsers().observe(appCompatActivity, stringUtilisateurEntityMap -> {
            adapter.setMapUrlByUtilisateur(stringUtilisateurEntityMap);
            adapter.notifyDataSetChanged();
        });
    }

    @OnClick(R.id.button_send_message)
    public void clickOnSendButton(View v) {
        final String messageToSend = textToSend.getText().toString();
        textToSend.setText("");
        if (messageToSend.isEmpty()) return;

        if (viewModel.getTypeRechercheMessage() == MyChatsActivityViewModel.TypeRechercheMessage.PAR_CHAT) {
            viewModel.saveLiveMessage(messageToSend).observeOnce(atomicBoolean -> Log.d(TAG, "Message correctement sauvegardé"));
        }

        if (viewModel.getTypeRechercheMessage() == MyChatsActivityViewModel.TypeRechercheMessage.PAR_ANNONCE) {
            if (adapter == null && viewModel.getAnnonce().getUidUser().equals(viewModel.getFirebaseUserUid())) {
                Toast.makeText(appCompatActivity, "Impossible de s'envoyer des messages", Toast.LENGTH_LONG).show();
            } else {
                viewModel.findOrCreateLiveNewChat().observeOnce(chatEntity -> {
                            if (chatEntity != null) {
                                viewModel.saveLiveMessage(messageToSend).observeOnce(atomicBoolean -> {
                                    Log.d(TAG, "Message correctement sauvegardé");
                                    if (initializeAdapterLater) {
                                        initializeAdapterByIdChat(chatEntity.getIdChat());
                                    }
                                });
                            }
                        }
                );
            }
        }
    }
}
