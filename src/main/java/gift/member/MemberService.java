package gift.member;

import gift.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(MemberErrorCode.NOT_FOUND));
    }

    @Transactional
    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new ApplicationException(MemberErrorCode.DUPLICATE_EMAIL);
        }
        return memberRepository.save(new Member(email, password));
    }

    public Member login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!member.matchesPassword(password)) {
            throw new ApplicationException(MemberErrorCode.INVALID_CREDENTIALS);
        }

        return member;
    }

    @Transactional
    public Member update(Long id, String email, String password) {
        Member member = findById(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    @Transactional
    public Member chargePoint(Long id, int amount) {
        Member member = findById(id);
        member.chargePoint(amount);
        return memberRepository.save(member);
    }

    @Transactional
    public Member deductPoint(Long id, int amount) {
        Member member = findById(id);
        member.deductPoint(amount);
        return memberRepository.save(member);
    }

    @Transactional
    public Member registerOrUpdateKakaoMember(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        return memberRepository.save(member);
    }

    @Transactional
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
