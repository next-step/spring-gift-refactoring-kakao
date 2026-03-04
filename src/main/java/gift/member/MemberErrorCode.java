package gift.member;

import gift.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_REGISTERED(
        "EMAIL_ALREADY_REGISTERED", "이미 등록된 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(
        "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_POINT("INSUFFICIENT_POINT", "포인트가 부족합니다.", HttpStatus.BAD_REQUEST),
    INVALID_POINT_AMOUNT(
        "INVALID_POINT_AMOUNT", "금액은 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    MemberErrorCode(final String code, final String message, final HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
