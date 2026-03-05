package gift.auth;

import gift.global.UnauthorizedException;
import java.util.Optional;

/**
 * {@code Authorization} 헤더 value 로 부터 사용자 ID 를 요청하는 계약
 */
public interface AuthenticationPort {

    /**
     * {@code Authorization} 에서 사용자 ID 를 요구
     *
     * @param authorization {@code HTTP} 요청의 {@code Authorization} 헤더 값
     * @return 유효한 {@code authorization} 이 아니면 {@code Optional.empty()}
     */
    Optional<Long> getMemberIdFrom(String authorization);

    /**
     * {@code Authorization} 에서 사용자 ID 를 요구한다. 인증 실패 시 예외를 던진다.
     *
     * @param authorization {@code HTTP} 요청의 {@code Authorization} 헤더 값
     * @return 인증된 사용자 ID
     * @throws UnauthorizedException 유효한 {@code authorization} 이 아니면
     */
    Long getMemberIdFrom(String authorization) throws UnauthorizedException;
}
