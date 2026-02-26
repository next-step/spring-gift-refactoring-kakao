package gift.member;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
        return memberRepository.save(new Member(email, password));
    }

    public Member login(String email, String password) {
        final Member member = memberRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (member.getPassword() == null || !member.getPassword().equals(password)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return member;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다. id=" + id));
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public Member createMember(String email, String password) {
        return memberRepository.save(new Member(email, password));
    }

    @Transactional
    public Member updateMember(Long id, String email, String password) {
        final Member member = findById(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    @Transactional
    public Member chargePoint(Long id, int amount) {
        final Member member = findById(id);
        member.chargePoint(amount);
        return memberRepository.save(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }
}
