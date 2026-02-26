package gift.member;

import gift.auth.JwtProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public String register(MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        Member member = memberRepository.save(new Member(request.email(), request.password()));
        return jwtProvider.createToken(member.getEmail());
    }

    public String login(MemberRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return jwtProvider.createToken(member.getEmail());
    }

    public Member getMember(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Member not found. id=" + id));
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Transactional
    public Member createMember(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        return memberRepository.save(new Member(email, password));
    }

    @Transactional
    public Member updateMember(Long id, String email, String password) {
        Member member = getMember(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    @Transactional
    public Member chargePoint(Long id, int amount) {
        Member member = getMember(id);
        member.chargePoint(amount);
        return memberRepository.save(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    @Transactional
    public Member findOrCreateByKakao(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        return memberRepository.save(member);
    }

    public String createToken(String email) {
        return jwtProvider.createToken(email);
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }
}
