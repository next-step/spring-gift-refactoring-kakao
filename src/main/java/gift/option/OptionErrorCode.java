package gift.option;

import gift.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OptionErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "OPTION_001", "옵션을 찾을 수 없습니다."),
    INVALID_NAME(HttpStatus.BAD_REQUEST, "OPTION_002", "옵션 이름이 올바르지 않습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "OPTION_003", "옵션 수량이 올바르지 않습니다."),
    DUPLICATE_NAME(HttpStatus.BAD_REQUEST, "OPTION_004", "이미 존재하는 옵션명입니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "OPTION_005", "차감할 수량이 현재 재고보다 많습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
