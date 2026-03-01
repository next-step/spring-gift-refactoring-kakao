package gift.auth;

import gift.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthMemberRepository extends JpaRepository<Member, Long> {

}
