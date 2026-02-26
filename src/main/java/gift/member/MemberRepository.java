package gift.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/* 회원 엔티티 리포지토리 */
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}
