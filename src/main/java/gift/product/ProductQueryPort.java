package gift.product;

import gift.global.NotFoundException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 정보를 조회할 수 있는 계약
 */
public interface ProductQueryPort {

    /**
     * {@code @ManyToOne} FK 참조용 상품 Entity 를 제공한다.
     * <p>
     * 구현체에 {@code @Transactional(propagation = MANDATORY)} 적용. 호출자의 트랜잭션 컨텍스트 안에서만 사용해야 한다.
     *
     * @param id 상품 id
     * @throws NotFoundException                id 에 해당하는 상품이 없으면
     * @throws IllegalTransactionStateException 호출자에 트랜잭션이 존재하지 않으면
     * @see Transactional
     */
    Product getReference(Long id) throws NotFoundException, IllegalTransactionStateException;

    /**
     * 상품이 존재하는지 검증한다.
     *
     * @param id 상품 id
     * @throws NotFoundException id 에 해당하는 상품이 없으면
     */
    void validateExists(Long id) throws NotFoundException;
}
