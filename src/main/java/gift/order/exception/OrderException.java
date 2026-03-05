package gift.order.exception;

import gift.common.BaseException;

public class OrderException extends BaseException {
    public OrderException(OrderErrorCode errorCode) {
        super(errorCode);
    }
}
