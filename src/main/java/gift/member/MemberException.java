package gift.member;

import gift.common.BaseException;

public class MemberException extends BaseException {
    public MemberException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}
