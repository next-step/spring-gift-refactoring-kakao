package gift.order;

import gift.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_001", "주문 수량은 1 이상이어야 합니다."),
    PRICE_OVERFLOW(HttpStatus.BAD_REQUEST, "ORDER_002", "주문 금액이 너무 큽니다. 관리자에게 문의해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
