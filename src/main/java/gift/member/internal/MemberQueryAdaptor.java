package gift.member.internal;

import gift.global.NotFoundException;
import gift.member.Member;
import gift.member.MemberQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberQueryAdaptor implements MemberQueryPort {

    private final MemberRepository memberRepo;

    @Override
    public Long getIdByEmail(String email) {
        return memberRepo.findByEmail(email)
                .map(Member::getId)
                .orElseThrow(NotFoundException::memberNotFound);
    }

    @Override
    public String getEmail(Long id) {
        return memberRepo.findById(id)
                .orElseThrow(NotFoundException::memberNotFound)
                .getEmail();
    }

    @Override
    public String getKakaoAccessToken(Long id) {
        return memberRepo.findById(id)
                .orElseThrow(NotFoundException::memberNotFound)
                .getKakaoAccessToken();
    }
}
