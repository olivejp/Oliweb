package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.annotation.NonNull;

import java.util.List;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

/**
 * Created by 2761oli on 05/09/2018.
 */
public class AdvancedSearchActivityViewModel extends AndroidViewModel {

    private CategorieRepository categorieRepository;

    public AdvancedSearchActivityViewModel(@NonNull Application application) {
        super(application);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        categorieRepository = component.getCategorieRepository();
    }

    public Single<List<CategorieEntity>> getListCategorie() {
        return categorieRepository.getListCategorie();
    }

    public Single<List<String>> getListCategorieLibelle() {
        return categorieRepository.getListCategorieLibelle();
    }
}
