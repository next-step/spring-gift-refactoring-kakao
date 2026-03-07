package gift.config;

import gift.member.MemberController;
import gift.option.OptionController;
import gift.order.OrderController;
import gift.product.ProductController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
    MemberController.class,
    ProductController.class,
    OptionController.class,
    OrderController.class
})
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
