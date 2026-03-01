package gift.auth;

import gift.global.NotFoundException;

/**
 * 사용자 ID 로 {@code JWT} 를 요청하는 계약
 */
public interface JwtPort {

    /**
     * 사용자 ID 로 {@code JWT} 를 요청
     *
     * @param memberId 사용자 ID
     * @return {@code JWT}
     * @throws NotFoundException ID 에 해당하는 사용자가 존재하지 않을 때
     */
    String issueMemberJwt(Long memberId) throws NotFoundException;

}
