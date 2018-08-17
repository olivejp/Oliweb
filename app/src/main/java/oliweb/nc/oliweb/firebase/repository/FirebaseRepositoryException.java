package oliweb.nc.oliweb.firebase.repository;

public class FirebaseRepositoryException extends RuntimeException {

    public FirebaseRepositoryException() {
    }

    public FirebaseRepositoryException(String message) {
        super(message);
    }

    public FirebaseRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirebaseRepositoryException(Throwable cause) {
        super(cause);
    }
}
