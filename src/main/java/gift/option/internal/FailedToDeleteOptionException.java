package gift.option.internal;

import gift.global.BadRequestException;

public class FailedToDeleteOptionException extends BadRequestException {

    public FailedToDeleteOptionException(String responseMessage) {
        super(responseMessage);
    }

    public static FailedToDeleteOptionException byInsufficientRemainingOptions() {
        return new FailedToDeleteOptionException(
                "옵션이 1개인 상품은 옵션을 삭제할 수 없습니다."
        );
    }
}
