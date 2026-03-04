package gift.order;

import gift.error.BusinessException;

public class OrderException extends BusinessException {
    public OrderException(final OrderErrorCode errorCode) {
        super(errorCode);
    }
}
