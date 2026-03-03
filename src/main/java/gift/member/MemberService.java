package gift.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다. id: " + id));
    }

    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
        return memberRepository.save(new Member(email, password));
    }

    public Member login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (member.getPassword() == null || !member.getPassword().equals(password)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return member;
    }

    public Member update(Long id, String email, String password) {
        Member member = findById(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    public Member chargePoint(Long id, int amount) {
        Member member = findById(id);
        member.chargePoint(amount);
        return memberRepository.save(member);
    }

    public Member deductPoint(Long id, int amount) {
        Member member = findById(id);
        member.deductPoint(amount);
        return memberRepository.save(member);
    }

    public Member registerOrUpdateKakaoMember(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        return memberRepository.save(member);
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
