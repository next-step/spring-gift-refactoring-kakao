package gift.product;

import gift.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001", "상품을 찾을 수 없습니다."),
    INVALID_NAME(HttpStatus.BAD_REQUEST, "PRODUCT_002", "상품 이름이 올바르지 않습니다."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "PRODUCT_003", "상품 가격은 1 이상이어야 합니다."),
    KAKAO_NAME_RESTRICTED(HttpStatus.BAD_REQUEST, "PRODUCT_004", "\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다."),
    LAST_OPTION_DELETE(HttpStatus.BAD_REQUEST, "PRODUCT_005", "옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
