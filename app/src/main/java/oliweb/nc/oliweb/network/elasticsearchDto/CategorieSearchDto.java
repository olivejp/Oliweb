package oliweb.nc.oliweb.network.elasticsearchDto;

/**
 * Created by orlanth23 on 20/02/2018.
 */

public class CategorieSearchDto {
    private long id;
    private String libelle;

    public CategorieSearchDto(long id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}
