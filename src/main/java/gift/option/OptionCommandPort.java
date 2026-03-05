package gift.option;

import gift.global.NotFoundException;

/**
 * 옵션의 상태를 변경할 수 있는 계약
 * <p>
 * 모든 method 는 {@code @Transactional(propagation = Propagation.REQUIRED)} {@code (기본값)} 으로 구성되어 자체적인
 * tx 를 갖거나 이전 tx 에 붙을수 있음에 유의.
 */
public interface OptionCommandPort {

    /**
     * 옵션의 수량을 {@code amount} 만큼 차감
     *
     * @param id     옵션 id
     * @param amount 차감할 수량
     * @throws NotFoundException        id 에 해당하는 옵션이 없을 때
     * @throws IllegalArgumentException 옵션의 현재 수량이 {@code amount} 보다 적을때
     */
    void subtractQuantity(Long id, int amount) throws NotFoundException, IllegalArgumentException;
}
