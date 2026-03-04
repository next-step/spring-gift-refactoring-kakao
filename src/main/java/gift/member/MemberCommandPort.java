package gift.member;

import gift.global.NotFoundException;

/**
 * 사용자 정보를 변경 또는 추가할 수 있는 계약
 * <p>
 * 모든 method 는 {@code @Transactional(propagation = Propagation.REQUIRED)} {@code (기본값)} 으로 구성되어 자체적인
 * tx 를 갖거나 이전 tx 에 붙을수 있음에 유의.
 */
public interface MemberCommandPort {

    /**
     * 사용자의 point 를 {@code amount} 만큼 감소
     *
     * @param id     사용자 id
     * @param amount 감소시킬 point 양
     * @throws NotFoundException        id 에 해당하는 사용자가 없을 때
     * @throws IllegalArgumentException 사용자가 소유한 point 가 {@code amount} 보다 적을때
     */
    void deductPoint(Long id, int amount) throws NotFoundException, IllegalArgumentException;

    /**
     * 새로운 사용자를 생성
     *
     * @param info 생성할 사용자 정보
     * @return 생성된 사용자 id
     */
    Long create(MemberInfo info);

    /**
     * 사용자의 kakao access 토큰을 {@code newToken} 값으로 수정
     *
     * @param id       사용자 id
     * @param newToken 수정할 토큰
     * @throws NotFoundException id 에 해당하는 사용자가 없으면
     */
    void updateKakaoAccessToken(Long id, String newToken) throws NotFoundException;
}
