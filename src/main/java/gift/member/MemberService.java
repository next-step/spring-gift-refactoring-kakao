package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    public TokenResponse register(MemberRequest request) {
        Member member = create(request.email(), request.password());
        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    public TokenResponse login(MemberRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Member not found. id=" + id));
    }

    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        return memberRepository.save(new Member(email, password));
    }

    public Member update(Long id, String email, String password) {
        Member member = findById(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    public void chargePoint(Long id, int amount) {
        Member member = findById(id);
        member.chargePoint(amount);
        memberRepository.save(member);
    }

    public void deductPoint(Long id, int amount) {
        Member member = findById(id);
        member.deductPoint(amount);
        memberRepository.save(member);
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }

    public Member registerOrUpdateKakaoMember(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        return memberRepository.save(member);
    }
}
