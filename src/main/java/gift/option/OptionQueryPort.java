package gift.option;

import gift.global.NotFoundException;
import gift.product.ProductDto;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 옵션 정보를 조회할 수 있는 계약
 */
public interface OptionQueryPort {

    /**
     * {@code @ManyToOne} FK 참조용 옵션 Entity 를 제공한다.
     * <p>
     * 구현체에 {@code @Transactional(propagation = MANDATORY)} 적용. 호출자의 트랜잭션 컨텍스트 안에서만 사용해야 한다.
     *
     * @param id 옵션 id
     * @throws NotFoundException                id 에 해당하는 옵션이 없으면
     * @throws IllegalTransactionStateException 호출자에 트랜잭션이 존재하지 않으면
     * @see Transactional
     */
    Option getReference(Long id) throws NotFoundException, IllegalTransactionStateException;

    /**
     * 옵션에 연관된 상품 정보를 제공한다.
     *
     * @param optionId 옵션 id
     * @throws NotFoundException optionId 에 해당하는 옵션이 없으면
     */
    ProductDto getAssociatedProduct(Long optionId) throws NotFoundException;
}
