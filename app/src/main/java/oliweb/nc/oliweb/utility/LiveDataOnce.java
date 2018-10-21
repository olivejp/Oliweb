package oliweb.nc.oliweb.utility;

import androidx.lifecycle.Observer;

/**
 * Created by orlanth23 on 08/08/2018.
 */
public interface LiveDataOnce<T> {
    void observeOnce(Observer<T> observer);
}
