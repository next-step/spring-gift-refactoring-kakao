package gift.member;

import gift.auth.JwtProvider;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String register(String email, String password) {
        validateEmailNotDuplicated(email);
        Member member = memberRepository.save(new Member(email, password, passwordEncoder));
        return jwtProvider.createToken(member.getEmail());
    }

    public String login(String email, String password) {
        Member member = memberRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.checkPassword(password, passwordEncoder)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return jwtProvider.createToken(member.getEmail());
    }

    @Transactional
    public String loginWithKakao(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email).orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        memberRepository.save(member);
        return jwtProvider.createToken(member.getEmail());
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다. id=" + id));
    }

    @Transactional
    public Member create(String email, String password) {
        validateEmailNotDuplicated(email);
        return memberRepository.save(new Member(email, password, passwordEncoder));
    }

    @Transactional
    public void update(Long id, String email, String password) {
        Member member = findById(id);
        member.update(email, password, passwordEncoder);
    }

    @Transactional
    public void chargePoint(Long id, int amount) {
        Member member = findById(id);
        member.chargePoint(amount);
    }

    @Transactional
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }

    private void validateEmailNotDuplicated(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
    }
}
