package gift.auth;

import gift.member.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthMemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

}
