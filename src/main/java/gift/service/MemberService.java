package gift.service;

import gift.model.Member;
import gift.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
    }

    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        String encodedPassword = passwordEncoder.encode(password);
        return memberRepository.save(new Member(email, encodedPassword));
    }

    public void update(Long id, String email, String password) {
        final Member member = findById(id);
        String encodedPassword = passwordEncoder.encode(password);
        member.update(email, encodedPassword);
        memberRepository.save(member);
    }

    public void chargePoint(Long id, int amount) {
        final Member member = findById(id);
        member.chargePoint(amount);
        memberRepository.save(member);
    }

    public void deductPoint(Member member, int amount) {
        member.deductPoint(amount);
        memberRepository.save(member);
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
