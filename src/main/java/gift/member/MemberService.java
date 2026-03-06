package gift.member;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public Member register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new MemberException(MemberErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        return memberRepository.save(new Member(email, password));
    }

    @Transactional(readOnly = true)
    public Member login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new MemberException(MemberErrorCode.INVALID_CREDENTIALS));

        if (member.getPassword() == null || !member.getPassword().equals(password)) {
            throw new MemberException(MemberErrorCode.INVALID_CREDENTIALS);
        }
        return member;
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public void update(Long id, String email, String password) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.update(email, password);
    }

    @Transactional
    public void chargePoint(Long id, int amount) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.chargePoint(amount);
    }

    @Transactional
    public void deleteById(Long id) {
        memberRepository.deleteById(id);
    }
}
