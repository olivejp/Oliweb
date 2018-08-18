package oliweb.nc.oliweb.repository.local.task;

public class DataReturn<U> {
    private int nb;
    private TypeTask typeTask;
    private U[] ids;
    private boolean successful;
    private Throwable throwable;

    public DataReturn() {
    }

    public int getNb() {
        return nb;
    }

    public void setNb(int nb) {
        this.nb = nb;
    }

    public TypeTask getTypeTask() {
        return typeTask;
    }

    public void setTypeTask(TypeTask typeTask) {
        this.typeTask = typeTask;
    }

    public U[] getIds() {
        return ids;
    }

    public void setIds(U[] ids) {
        this.ids = ids;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
