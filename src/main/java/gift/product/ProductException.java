package gift.product;

import gift.error.BusinessException;

public class ProductException extends BusinessException {
    public ProductException(final ProductErrorCode errorCode) {
        super(errorCode);
    }
}
