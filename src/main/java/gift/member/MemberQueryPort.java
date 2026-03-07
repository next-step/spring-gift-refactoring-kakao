package gift.member;

import gift.global.NotFoundException;

/**
 * 사용자 정보를 조회할 수 있는 계약
 */
public interface MemberQueryPort {

    /**
     * 해당 email 을 가진 사용자 id 를 제공한다.
     *
     * @throws NotFoundException email 을 가진 사용자가 없으면
     */
    Long getIdByEmail(String email) throws NotFoundException;

    /**
     * 사용자의 email 을 제공한다.
     *
     * @param id 사용자 id
     * @throws NotFoundException id 에 해당하는 사용자가 없으면
     */
    String getEmail(Long id) throws NotFoundException;

    /**
     * 사용자의 kakao access 토큰을 제공한다.
     *
     * @param id 사용자 id
     * @throws NotFoundException id 에 해당하는 사용자가 없으면
     */
    String getKakaoAccessToken(Long id) throws NotFoundException;
}
