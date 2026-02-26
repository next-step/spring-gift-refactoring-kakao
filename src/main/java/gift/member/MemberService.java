package gift.member;

import gift.auth.JwtProvider;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public String register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        Member member = memberRepository.save(new Member(email, password));
        return jwtProvider.createToken(member.getEmail());
    }

    @Transactional(readOnly = true)
    public String login(String email, String password) {
        Member member = memberRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (member.getPassword() == null || !member.getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid email or password.");
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

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException("Member not found. id=" + id));
    }

    @Transactional
    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        return memberRepository.save(new Member(email, password));
    }

    @Transactional
    public void update(Long id, String email, String password) {
        Member member = findById(id);
        member.update(email, password);
        memberRepository.save(member);
    }

    @Transactional
    public void chargePoint(Long id, int amount) {
        Member member = findById(id);
        member.chargePoint(amount);
        memberRepository.save(member);
    }

    @Transactional
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
