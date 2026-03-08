package gift.member;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new MemberNotFoundException(id));
    }

    @Transactional
    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        return memberRepository.save(new Member(email, password));
    }

    @Transactional
    public Member update(Long id, String email, String password) {
        Member member = memberRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new MemberNotFoundException(id));
        member.update(email, password);
        return memberRepository.save(member);
    }

    @Transactional
    public void chargePoint(Long id, int amount) {
        Member member = memberRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new MemberNotFoundException(id));
        member.chargePoint(amount);
        memberRepository.save(member);
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }

    @Transactional
    public TokenResponse register(MemberRequest request) {
        Member member = create(request.email(), request.password());
        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    @Transactional
    public Member findOrCreateByEmail(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(MemberRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!member.isPasswordValid(request.password())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
