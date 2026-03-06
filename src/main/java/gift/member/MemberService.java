package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MemberService {
  private final MemberRepository memberRepository;
  private final JwtProvider jwtProvider;

  public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
    this.memberRepository = memberRepository;
    this.jwtProvider = jwtProvider;
  }

  public List<Member> findAllMembers() {
    return memberRepository.findAll();
  }

  public Optional<Member> findMemberById(Long id) {
    return memberRepository.findById(id);
  }

  public boolean existsByEmail(String email) {
    return memberRepository.existsByEmail(email);
  }

  public Member createMember(String email, String password) {
    return memberRepository.save(new Member(email, password));
  }

  public Member updateMember(Long id, String email, String password) {
    Member member =
        memberRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + id));
    member.update(email, password);
    return memberRepository.save(member);
  }

  public void chargePoint(Long id, int amount) {
    Member member =
        memberRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. id=" + id));
    member.chargePoint(amount);
    memberRepository.save(member);
  }

  public void deleteMember(Long id) {
    memberRepository.deleteById(id);
  }

  public TokenResponse register(MemberRequest request) {
    if (memberRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("이미 등록된 이메일입니다.");
    }

    Member member = memberRepository.save(new Member(request.email(), request.password()));
    String token = jwtProvider.createToken(member.getEmail());
    return new TokenResponse(token);
  }

  public TokenResponse login(MemberRequest request) {
    Member member =
        memberRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

    if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
      throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    String token = jwtProvider.createToken(member.getEmail());
    return new TokenResponse(token);
  }
}
