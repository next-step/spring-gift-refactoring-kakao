package gift.global;

import org.springframework.http.HttpStatus;

public class NotFoundException extends CustomException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public static NotFoundException categoryNotFound() {
        return new NotFoundException("Category not found");
    }

    public static NotFoundException memberNotFound() {
        return new NotFoundException("Member not found");
    }
}
