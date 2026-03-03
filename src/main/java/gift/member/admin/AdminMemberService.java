package gift.member.admin;

import gift.member.Member;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final AdminMemberRepository memberRepo;

    public List<MemberDto> getAllMembers() {
        return memberRepo.findAll().stream()
                .map(MemberDto::from)
                .toList();
    }

    public boolean hasEmailRegistered(String email) {
        return memberRepo.existsByEmail(email);
    }

    @Transactional
    public void createMember(String email, String password) {
        Member newEntity = Member.builder()
                .email(email)
                .password(password)
                .build();

        memberRepo.save(newEntity);
    }

    public MemberDto findMember(Long id) {
        Member find = this.findMemberOrThrowEx(id);

        return MemberDto.from(find);
    }

    private Member findMemberOrThrowEx(Long id) {
        return memberRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
    }

    @Transactional
    public void updateMember(Long id, String email, String password) {
        Member find = this.findMemberOrThrowEx(id);

        find.update(email, password);
    }

    @Transactional
    public void chargePoint(Long id, int amount) {
        Member find = this.findMemberOrThrowEx(id);

        find.chargePoint(amount);
    }

    @Transactional
    public void deleteMember(Long id) {
        Member find = this.findMemberOrThrowEx(id);

        memberRepo.delete(find);
    }
}
