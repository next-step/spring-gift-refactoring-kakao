package gift.member;

import java.util.List;
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

  public Member getById(Long id) {
    return memberRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
  }

  @Transactional
  public Member register(String email, String password) {
    if (memberRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("Email is already registered.");
    }
    return memberRepository.save(new Member(email, password));
  }

  public Member login(String email, String password) {
    Member member =
        memberRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

    if (member.getPassword() == null || !member.getPassword().equals(password)) {
      throw new IllegalArgumentException("Invalid email or password.");
    }

    return member;
  }

  @Transactional
  public Member update(Long id, String email, String password) {
    Member member =
        memberRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
    member.update(email, password);
    return member;
  }

  @Transactional
  public Member chargePoint(Long id, int amount) {
    Member member =
        memberRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
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
