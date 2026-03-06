package gift.member;

import gift.DomainException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Member register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new DomainException("이미 등록된 이메일입니다.");
        }
        final String encodedPassword = passwordEncoder.encode(password);
        return memberRepository.save(new Member(email, encodedPassword));
    }

    public Member login(String email, String password) {
        final Member member =
                memberRepository.findByEmail(email).orElseThrow(() -> new DomainException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (member.getPassword() == null || !passwordEncoder.matches(password, member.getPassword())) {
            throw new DomainException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return member;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public Member updateMember(Long id, String email, String password) {
        final Member member = findById(id);
        final String encodedPassword = passwordEncoder.encode(password);
        member.update(email, encodedPassword);
        return memberRepository.save(member);
    }

    @Transactional
    public Member chargePoint(Long id, int amount) {
        final Member member = findById(id);
        member.chargePoint(amount);
        return memberRepository.save(member);
    }

    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }
}
