package gift.member;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {
  private final MemberRepository memberRepository;

  public MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  public List<Member> findAll() {
    return memberRepository.findAll();
  }

  public Optional<Member> findById(Long id) {
    return memberRepository.findById(id);
  }

  public boolean existsByEmail(String email) {
    return memberRepository.existsByEmail(email);
  }

  @Transactional
  public Member register(String email, String password) {
    if (memberRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("이미 등록된 이메일입니다.");
    }
    return memberRepository.save(new Member(email, password));
  }

  public Member login(String email, String password) {
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

    if (member.getPassword() == null || !member.getPassword().equals(password)) {
      throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    return member;
  }

  @Transactional
  public Member update(Long id, String email, String password) {
    Member member =
        memberRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + id));
    member.update(email, password);
    return member;
  }

  @Transactional
  public Member chargePoint(Long id, int amount) {
    Member member =
        memberRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + id));
    member.chargePoint(amount);
    return member;
  }

  @Transactional
  public void delete(Long id) {
    memberRepository.deleteById(id);
  }

  @Transactional
  public Member findOrCreateAndUpdateKakaoToken(String email, String kakaoAccessToken) {
    Member member = memberRepository.findByEmail(email).orElseGet(() -> new Member(email));
    member.updateKakaoAccessToken(kakaoAccessToken);
    return memberRepository.save(member);
  }
}
