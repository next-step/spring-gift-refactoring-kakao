package gift.member.admin;

import gift.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminMemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);
}
