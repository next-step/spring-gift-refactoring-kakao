package gift.member.internal;

import gift.global.NotFoundException;
import gift.member.Member;
import gift.member.MemberCommandPort;
import gift.member.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberCommandAdaptor implements MemberCommandPort {

    private final MemberRepository memberRepo;

    @Override
    @Transactional
    public void deductPoint(Long id, int amount) {
        Member member = this.findOrThrowNotFound(id);

        member.deductPoint(amount);
    }

    @Override
    @Transactional
    public Long create(MemberInfo info) {
        String email = info.email();
        String password = info.password();
        String kakaoAccessToken = info.kakaoAccessToken();

        Member member = Member.builder()
                .email(email)
                .password(password)
                .kakaoAccessToken(kakaoAccessToken)
                .build();

        return memberRepo.save(member)
                .getId();
    }

    @Override
    @Transactional
    public void updateKakaoAccessToken(Long id, String newToken) {
        Member member = this.findOrThrowNotFound(id);

        member.updateKakaoAccessToken(newToken);
    }

    private Member findOrThrowNotFound(Long id) {
        return memberRepo.findById(id)
                .orElseThrow(NotFoundException::memberNotFound);
    }
}
