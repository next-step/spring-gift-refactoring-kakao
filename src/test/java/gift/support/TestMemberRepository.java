package gift.support;

import gift.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestMemberRepository extends JpaRepository<Member, Long> {

}
