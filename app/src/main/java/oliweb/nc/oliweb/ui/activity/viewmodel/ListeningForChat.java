package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.support.annotation.NonNull;

import oliweb.nc.oliweb.firebase.dto.ChatFirebase;

/**
 * Created by orlanth23 on 04/04/2018.
 */

public interface ListeningForChat{
    void findChat(@NonNull ChatFirebase chat);
}
