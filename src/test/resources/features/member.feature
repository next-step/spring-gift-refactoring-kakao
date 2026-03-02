# language: ko
기능: 회원 관리

  시나리오: 회원가입을 하면 토큰이 발급된다
    만일 "test@example.com" 이메일과 "password" 비밀번호로 회원가입한다
    그러면 회원가입이 성공한다
    그리고 응답에 토큰이 포함되어 있다

  시나리오: 이미 등록된 이메일로 회원가입하면 실패한다
    조건 "test@example.com" 이메일로 가입된 회원이 있다
    만일 "test@example.com" 이메일과 "password" 비밀번호로 회원가입한다
    그러면 회원가입이 실패한다

  시나리오: 올바른 정보로 로그인하면 토큰이 발급된다
    조건 "test@example.com" 이메일과 "password" 비밀번호로 가입된 회원이 있다
    만일 "test@example.com" 이메일과 "password" 비밀번호로 로그인한다
    그러면 로그인이 성공한다
    그리고 응답에 토큰이 포함되어 있다

  시나리오: 존재하지 않는 이메일로 로그인하면 실패한다
    만일 "nonexistent@example.com" 이메일과 "password" 비밀번호로 로그인한다
    그러면 로그인이 실패한다
