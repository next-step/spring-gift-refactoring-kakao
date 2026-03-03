package gift.member.internal;

import gift.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepo;

    @Transactional
    public Long register(MemberRequest registerRequest) {
        String email = registerRequest.email();
        String password = registerRequest.password();

        if (memberRepo.existsByEmail(email)) {
            throw RegisterFailedException.byRegisteredEmail();
        }

        Member newEntity = Member.builder()
                .email(email)
                .password(password)
                .build();

        return memberRepo.save(newEntity)
                .getId();
    }

    public Long login(MemberRequest loginRequest) {
        String email = loginRequest.email();
        String password = loginRequest.password();

        return memberRepo.findByEmailAndPassword(email, password)
                .orElseThrow(LoginFailedException::byInvalidEmailOrPassword)
                .getId();
    }
}
