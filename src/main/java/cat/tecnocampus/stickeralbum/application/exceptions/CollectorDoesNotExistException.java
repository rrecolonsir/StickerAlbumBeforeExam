package cat.tecnocampus.stickeralbum.application.exceptions;

public class CollectorDoesNotExistException extends RuntimeException {
    public CollectorDoesNotExistException(Long id) {
        super("Collector wih id " + id + " does not exist");
    }
}
