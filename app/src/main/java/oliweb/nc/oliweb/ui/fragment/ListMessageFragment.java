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
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.MessageAdapter;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListMessageFragment extends Fragment {
    private static final String TAG = ListMessageFragment.class.getName();

    private String uidUser = FirebaseAuth.getInstance().getCurrentUser().getUid();

    private AppCompatActivity appCompatActivity;
    private MessageAdapter adapter;
    private MyChatsActivityViewModel viewModel;
    private boolean initializeAdapterLater = false;

    private AnnonceEntity annonce;

    @BindView(R.id.recycler_list_message)
    RecyclerView recyclerView;

    @BindView(R.id.text_to_send)
    EditText textToSend;

    @BindView(R.id.button_send_message)
    ImageView imageSend;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onDestroyView() {
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(appCompatActivity).get(MyChatsActivityViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_message, container, false);

        ButterKnife.bind(this, view);

        // Init recyclerView
        LinearLayoutManager linearLayout = new LinearLayoutManager(appCompatActivity);
        linearLayout.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayout);
        adapter = new MessageAdapter();
        recyclerView.setAdapter(adapter);

        // Sur l'action finale du prix on va sauvegarder l'annonce.
        textToSend.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                clickOnSendButton(textToSend);
                return true;
            }
            return false;
        });

        switch (viewModel.getTypeRechercheMessage()) {
            case PAR_ANNONCE:
                annonce = viewModel.getSelectedAnnonce();
                viewModel.findChatByUidUserAndUidAnnonce(uidUser, annonce)
                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .doOnSuccess(chatEntity -> initializeAdapterByIdChat(chatEntity.getIdChat()))
                        .doOnComplete(() -> initializeAdapterLater = true)
                        .subscribe();
                break;
            case PAR_CHAT:
                initializeAdapterByIdChat(viewModel.getSearchedIdChat());
                break;
        }

        return view;
    }

    private void initializeAdapterByIdChat(Long idChat) {
        viewModel.findAllMessageByIdChat(idChat).observe(appCompatActivity, listMessages -> adapter.setMessageEntities(listMessages));
        initializeAdapterLater = false;
    }

    @OnClick(R.id.button_send_message)
    public void clickOnSendButton(View v) {
        final String messageToSend = textToSend.getText().toString();
        textToSend.setText("");
        if (messageToSend.isEmpty()) {
            return;
        }

        switch (viewModel.getTypeRechercheMessage()) {
            case PAR_CHAT:
                viewModel.saveMessage(messageToSend)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .doOnSuccess(atomicBoolean -> {
                            Log.d(TAG, "Message correctement sauvegardé");
                            recyclerView.smoothScrollToPosition(adapter.getItemCount());
                        })
                        .subscribe();
                break;
            case PAR_ANNONCE:
                if (adapter == null && annonce.getUidUser().equals(uidUser)) {
                    Toast.makeText(appCompatActivity, "Impossible de s'envoyer des messages", Toast.LENGTH_LONG).show();
                } else {
                    viewModel.findOrCreateNewChat(uidUser, annonce)
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                            .doOnSuccess(chatEntity ->
                                    viewModel.saveMessage(messageToSend)
                                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                            .doOnSuccess(atomicBoolean -> {
                                                Log.d(TAG, "Message correctement sauvegardé");
                                                if (initializeAdapterLater) {
                                                    initializeAdapterByIdChat(chatEntity.getIdChat());
                                                }
                                            })
                                            .subscribe()
                            )
                            .subscribe();
                }
                break;
        }
    }
}
