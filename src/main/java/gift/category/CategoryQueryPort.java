package gift.category;

import gift.global.NotFoundException;
import java.util.List;

/**
 * 카테고리 정보를 조회할 수 있는 계약
 */
public interface CategoryQueryPort {

    /**
     * {@code @ManyToOne} FK 참조용 카테고리 Entity 를 제공한다.
     * <p>
     * 구현체에 {@code @Transactional(propagation = MANDATORY)} 적용. 호출자의 트랜잭션 컨텍스트 안에서만 사용해야 한다.
     *
     * @param id 카테고리 id
     * @throws NotFoundException id 에 해당하는 카테고리가 없으면
     */
    Category getReference(Long id) throws NotFoundException;

    /**
     * 전체 카테고리 목록을 제공한다.
     *
     * @return 카테고리 목록 (없으면 빈 목록)
     */
    List<CategoryDto> findAll();
}
