---
name: setup-error-handling
description: Spring Boot 프로젝트에 도메인별 에러 처리 구조를 세팅한다. 사용자가 "에러 처리", "예외 처리", "에러 코드", "ErrorCode", "ExceptionHandler", "에러 구조화" 등을 언급하면 이 스킬을 사용한다.
---

# 도메인별 에러 처리 구조 세팅

$ARGUMENTS 도메인에 대한 에러 처리 구조를 세팅한다. 아래 단계를 순서대로 수행한다.

---

## 구조 개요

```
gift/error/                         ← 공통 에러 인프라
├── ErrorCode.java                  ← 인터페이스 (code, message, httpStatus)
├── ErrorResponse.java              ← 응답 DTO + 팩토리 메서드
├── BusinessException.java          ← 추상 베이스 예외
├── GlobalExceptionHandler.java     ← @RestControllerAdvice
├── CommonErrorCode.java            ← 공통 에러 코드 enum
└── CommonException.java            ← 공통 예외

gift/{domain}/                      ← 각 도메인 패키지 내부
├── {Domain}ErrorCode.java          ← 도메인별 에러 코드 enum
└── {Domain}Exception.java          ← 도메인별 예외
```

도메인별 ErrorCode/Exception은 **해당 도메인 패키지** 안에 배치한다.
이 프로젝트는 도메인별 패키지 구조(`gift/category/`, `gift/member/`, ...)를 따르고 있으므로,
에러 코드도 동일한 패키지에 두어 응집도를 유지한다.

---

## 1단계: 프로젝트 분석

- 기존 에러 처리 구조를 확인한다:
  - `@ExceptionHandler` 위치와 처리 대상 (`IllegalArgumentException`, `NoSuchElementException` 등)
  - Controller별 예외 처리 방식 차이 (API Controller vs Admin Controller)
  - Service에서 throw하는 예외 종류와 메시지
- base package: `gift`
- 에러 인프라 패키지: `gift.error`
- $ARGUMENTS에서 도메인 이름들을 추출한다

### 현재 프로젝트의 에러 처리 현황

| 위치 | 예외 | HTTP 응답 |
|---|---|---|
| Service | `NoSuchElementException` | Controller별 다름 (404 또는 미처리) |
| Service | `IllegalArgumentException` | Controller별 `@ExceptionHandler` → 400 |
| Controller | `null` 체크 후 직접 404 반환 | `ResponseEntity.notFound()` |
| Controller | 인증 실패 시 직접 401 반환 | `ResponseEntity.status(401)` |
| AdminController | `NoSuchElementException` throw | Spring 기본 에러 페이지 |

---

## 2단계: 공통 에러 인프라 작성

`gift/error/` 패키지에 아래 파일을 생성한다.

### ErrorCode 인터페이스

모든 에러 코드 enum이 구현할 계약.

```java
package gift.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
```

### ErrorResponse DTO

API 에러 응답의 JSON 구조. 팩토리 메서드 `from(ErrorCode)` 포함.

```java
package gift.error;

public class ErrorResponse {
    private final String code;
    private final String message;

    public ErrorResponse(final String code, final String message) {
        this.code = code;
        this.message = message;
    }

    public static ErrorResponse from(final ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
```

### BusinessException 추상 클래스

모든 도메인 예외의 부모. ErrorCode를 감싸고 메시지를 RuntimeException에 전달한다.

```java
package gift.error;

public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BusinessException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

### GlobalExceptionHandler

`BusinessException`을 잡아서 ErrorCode의 httpStatus와 ErrorResponse로 변환한다.
Spring 프레임워크 예외(`MethodArgumentNotValidException`, `MissingRequestHeaderException`,
`HttpMessageNotReadableException`)도 함께 처리한다.

```java
package gift.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException e) {
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.from(CommonErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(final MissingRequestHeaderException e) {
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.from(CommonErrorCode.INVALID_REQUEST));
    }
}
```

**주의**: `@RestControllerAdvice`는 `@RestController`에만 적용된다.
Admin Controller(`@Controller`)의 예외는 기존 방식(Thymeleaf 에러 페이지 등)을 유지한다.

---

## 3단계: CommonErrorCode + CommonException 작성

모든 도메인에 공통으로 필요한 에러 코드.

```java
package gift.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "입력값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CommonErrorCode(final String code, final String message, final HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() { return code; }
    @Override
    public String getMessage() { return message; }
    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }
}
```

```java
package gift.error;

public class CommonException extends BusinessException {
    public CommonException(final CommonErrorCode errorCode) {
        super(errorCode);
    }
}
```

---

## 4단계: 도메인별 ErrorCode + Exception 작성

$ARGUMENTS의 각 도메인에 대해 **해당 도메인 패키지** 안에 생성한다.

### 규칙

- **ErrorCode enum**: `{Domain}ErrorCode implements ErrorCode` — 해당 도메인의 에러 상수를 나열
- **Exception 클래스**: `{Domain}Exception extends BusinessException` — 생성자가 해당 도메인의 ErrorCode만 받도록 타입 제한
- 에러 코드명은 `SCREAMING_SNAKE_CASE`
- 각 도메인에 최소 하나의 `{DOMAIN}_NOT_FOUND` 에러 코드 포함
- 기존 Service/Entity에서 throw하는 예외 메시지를 분석하여 에러 코드를 도출한다

### 도메인별 에러 코드 도출 가이드

현재 프로젝트의 기존 예외를 분석하여 에러 코드를 결정한다:

| 기존 예외 | 위치 | 도출 에러 코드 |
|---|---|---|
| `NoSuchElementException("상품이 존재하지 않습니다")` | ProductService | `PRODUCT_NOT_FOUND` (404) |
| `NoSuchElementException("카테고리가 존재하지 않습니다")` | ProductService | `CATEGORY_NOT_FOUND` (404) |
| `NoSuchElementException("Member not found")` | MemberService | `MEMBER_NOT_FOUND` (404) |
| `IllegalArgumentException("Email is already registered")` | MemberService | `EMAIL_ALREADY_REGISTERED` (400) |
| `IllegalArgumentException("Invalid email or password")` | MemberService | `INVALID_CREDENTIALS` (400) |
| `IllegalArgumentException("이미 존재하는 옵션명입니다")` | OptionService | `DUPLICATE_OPTION_NAME` (400) |
| `IllegalArgumentException("옵션이 1개인 상품은...")` | OptionService | `CANNOT_DELETE_LAST_OPTION` (400) |
| `IllegalArgumentException("차감할 수량이...")` | Option entity | `INSUFFICIENT_STOCK` (400) |
| `IllegalArgumentException("포인트가 부족합니다")` | Member entity | `INSUFFICIENT_POINT` (400) |

### 예시: Product 도메인

```java
// gift/product/ProductErrorCode.java
package gift.product;

import gift.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ProductErrorCode(final String code, final String message, final HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() { return code; }
    @Override
    public String getMessage() { return message; }
    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }
}
```

```java
// gift/product/ProductException.java
package gift.product;

import gift.error.BusinessException;

public class ProductException extends BusinessException {
    public ProductException(final ProductErrorCode errorCode) {
        super(errorCode);
    }
}
```

---

## 5단계: 검증

```bash
./gradlew compileJava      # 컴파일 확인
./gradlew checkstyleMain   # 스타일 위반 0건
./gradlew test             # 기존 테스트 통과
```

이 단계에서는 **구조만 생성**한다.
기존 Controller의 `@ExceptionHandler`와 Service의 예외를 교체하는 작업은 별도 커밋으로 진행한다.

---

## 6단계: 사용법 안내

### 예외 던지기

```java
// Service에서
throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND);
```

### 응답 형태

```json
{
    "code": "PRODUCT_NOT_FOUND",
    "message": "상품을 찾을 수 없습니다."
}
```

### 새 에러 코드 추가 시

1. 해당 도메인의 `{Domain}ErrorCode` enum에 새 상수를 추가한다
2. Service에서 `new {Domain}Exception({Domain}ErrorCode.NEW_ERROR)`를 던진다
3. `GlobalExceptionHandler`가 자동으로 처리한다 (추가 코드 불필요)

### Admin Controller 주의사항

`@RestControllerAdvice`는 `@RestController`에만 적용된다.
Admin Controller(`@Controller`)에서는 기존 방식(Thymeleaf 에러 페이지, redirect)을 유지하거나,
필요 시 `@ControllerAdvice`를 별도로 구성한다.
