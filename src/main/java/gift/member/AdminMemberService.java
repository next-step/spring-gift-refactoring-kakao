package gift.member;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminMemberService {
    private final MemberRepository memberRepository;

    public AdminMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public Member save(Member member) {
        return memberRepository.save(member);
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    public Member findByIdOrThrow(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
    }

    public void deleteById(Long id) {
        memberRepository.deleteById(id);
    }
}
