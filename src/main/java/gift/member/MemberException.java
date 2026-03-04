package gift.member;

import gift.error.BusinessException;

public class MemberException extends BusinessException {
    public MemberException(final MemberErrorCode errorCode) {
        super(errorCode);
    }
}
