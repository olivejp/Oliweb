package oliweb.nc.oliweb.utility;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.annotation.Nullable;

/**
 * Created by orlanth23 on 08/08/2018.
 */
public class CustomLiveData<T> extends MutableLiveData<T> implements LiveDataOnce<T> {
    @Override
    public void observeOnce(Observer<T> observer) {
        observeForever(new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                observer.onChanged(t);
                removeObserver(this);
            }
        });
    }
}