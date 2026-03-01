package gift.order.internal;

import gift.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderMemberRepository extends JpaRepository<Member, Long> {

}
