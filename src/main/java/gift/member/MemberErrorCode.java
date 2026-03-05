package gift.member;

import gift.common.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum MemberErrorCode implements BaseErrorCode {
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 등록된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "충전 금액은 1 이상이어야 합니다."),
    INVALID_DEDUCT_AMOUNT(HttpStatus.BAD_REQUEST, "차감 금액은 1 이상이어야 합니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "포인트가 부족합니다.");

    private final HttpStatus httpStatus;
    private final String message;

    MemberErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
