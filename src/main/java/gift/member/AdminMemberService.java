package gift.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {
    private final MemberRepository memberRepository;

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public Member save(Member member) {
        return memberRepository.save(member);
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    public Member findByIdOrThrow(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public void deleteById(Long id) {
        memberRepository.deleteById(id);
    }
}
