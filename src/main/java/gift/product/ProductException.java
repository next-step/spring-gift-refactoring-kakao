package gift.product;

import gift.common.BaseException;

public class ProductException extends BaseException {
    public ProductException(ProductErrorCode errorCode) {
        super(errorCode);
    }
}
