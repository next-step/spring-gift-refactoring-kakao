package gift.wish;

import gift.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WishErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "WISH_001", "위시를 찾을 수 없습니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "WISH_002", "본인의 위시만 삭제할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
