# 6-2. 디미터 법칙 위반 해소 — 서비스/클라이언트 레이어 체인 접근 제거

## 작업 목적 및 배경

6-1에서 DTO/서비스의 엔티티 체인 접근을 제거했으나, `KakaoMessageClient`, `KakaoNotificationService`, `AuthService`에 디미터 법칙 위반이 남아 있었다. 이번 작업에서 서비스/클라이언트 레이어의 체인 접근을 모두 제거한다.

## 위반 현황 (4건)

| 파일 | 체인 접근 | 유형 |
|------|----------|------|
| `KakaoMessageClient.java:46` | `order.getOption().getName()` | 엔티티 체인 접근 |
| `KakaoNotificationService.java:22` | `option.getProduct()` → 내부 객체를 꺼내 외부에 전달 | 내부 객체 노출 |
| `KakaoMessageClient.java:34` | `order.getMessage().isBlank()` | String 메서드 체인 |
| `AuthService.java:33` | `member.getPassword().equals(password)` | 내부 상태 노출 후 비교 |

## 변경 사항 요약

### 1단계: 엔티티에 위임/행위 메서드 추가

- **`src/main/java/gift/model/Order.java`** — `optionName()`, `hasMessage()` 추가
- **`src/main/java/gift/model/Option.java`** — `productName()` 추가
- **`src/main/java/gift/model/Member.java`** — `passwordMatches(String)` 추가

### 2단계: KakaoMessageClient 시그니처 및 내부 변경

- `sendToMe(String, Order, Product)` → `sendToMe(String, Order, Option)`
- `buildTemplate(Order, Product)` → `buildTemplate(Order, Option)`
- `product.getName()` → `option.productName()`
- `product.getPrice() * order.getQuantity()` → `option.calculatePrice(order.getQuantity())`
- `order.getOption().getName()` → `order.optionName()`
- `order.getMessage() != null && !order.getMessage().isBlank()` → `order.hasMessage()`

### 3단계: 호출부 변경

- **`KakaoNotificationService.java`** — `option.getProduct()` 제거, `option`을 직접 전달
- **`AuthService.java`** — `member.getPassword() == null || !member.getPassword().equals(password)` → `!member.passwordMatches(password)`

## 적용 규칙

- **디미터 법칙(Law of Demeter)**: 객체의 내부 구조를 외부에 노출하지 않고, 위임 메서드를 통해 필요한 정보만 제공
- 기존 getter는 유지 (다른 곳에서 사용 가능성)
- `Option.calculatePrice()` 내부의 `product.getPrice()`는 자기 필드 접근이므로 위반 아님

## 수정 파일 목록

- `src/main/java/gift/model/Order.java`
- `src/main/java/gift/model/Option.java`
- `src/main/java/gift/model/Member.java`
- `src/main/java/gift/client/KakaoMessageClient.java`
- `src/main/java/gift/service/KakaoNotificationService.java`
- `src/main/java/gift/service/AuthService.java`

## 검증

- `./gradlew test` 전체 테스트 통과 확인
