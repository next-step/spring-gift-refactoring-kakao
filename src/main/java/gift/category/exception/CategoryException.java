package gift.category.exception;

import gift.common.BaseException;

public class CategoryException extends BaseException {
    public CategoryException(CategoryErrorCode errorCode) {
        super(errorCode);
    }
}
