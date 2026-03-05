package gift.member;

import gift.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER_002", "이미 등록된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "MEMBER_003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "MEMBER_004", "올바른 이메일 형식이 아닙니다."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "MEMBER_005", "금액은 1 이상이어야 합니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "MEMBER_006", "포인트가 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
