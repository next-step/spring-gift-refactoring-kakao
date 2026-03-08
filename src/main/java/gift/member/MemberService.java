package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public TokenResponse register(MemberRequest request) {
        create(request.email(), request.password());
        return new TokenResponse(jwtProvider.createToken(request.email()));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(MemberRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!member.authenticate(request.password())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> findAll() {
        return memberRepository.findAll().stream()
            .map(MemberResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse findById(Long id) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다. id=" + id));
        return MemberResponse.from(member);
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public void create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
        memberRepository.save(new Member(email, password));
    }

    @Transactional
    public void update(Long id, String email, String password) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다. id=" + id));
        member.update(email, password);
        memberRepository.save(member);
    }

    @Transactional
    public void chargePoint(Long id, int amount) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다. id=" + id));
        member.chargePoint(amount);
        memberRepository.save(member);
    }

    @Transactional
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
