---
name: behavior-verification
description: 작동 변경 검증 스킬. 사용자가 "작동 변경", "행위 검증", "상태 검증", "증거 테스트", "상태 재조회", "동작 검증" 등을 언급하면 이 스킬을 사용한다. 작동 변경 시 변경 전 명세 작성법과 상태 재조회 테스트 패턴을 정의한다.
---

# 작동 변경 검증 가이드

작동(behavior)을 변경하는 모든 커밋은 **변경 사실을 증명하는 테스트**를 동반해야 한다.
"예외가 발생하니까 됐다"가 아니라, **"상태가 바뀌었음을 확인했다"**가 기준이다.

---

## 1. 변경 전 3줄 명세

작동을 변경하기 **전에** 반드시 아래 3줄을 정의한다:

```
1. 무엇을 바꾸는가:  (변경되는 동작을 한 문장으로)
2. 무엇을 안 바꾸는가: (영향받지 않는 기존 동작)
3. 어떻게 증명하는가: (테스트에서 확인할 방법)
```

### 예시

```
1. 무엇을 바꾸는가:  주문 시 옵션 재고를 차감한다
2. 무엇을 안 바꾸는가: 주문 생성 흐름 자체는 동일
3. 어떻게 증명하는가: 주문 후 옵션 조회 API로 재고가 감소했는지 확인
```

```
1. 무엇을 바꾸는가:  위시리스트 추가 시 중복 상품을 거부한다
2. 무엇을 안 바꾸는가: 최초 추가는 정상 동작
3. 어떻게 증명하는가: 중복 추가 시도 후 위시리스트 조회로 1건만 존재하는지 확인
```

---

## 2. 상태 재조회 테스트 패턴

### 핵심 원칙
- **Then 절은 상태 조회**여야 한다
- 변경 API 호출 후, 조회 API로 결과를 재확인한다
- 예외 발생 여부만 확인하는 것은 **불충분**하다

### 패턴: Given-When-Then with State Re-query

```java
@Test
@DisplayName("주문하면 옵션 재고가 차감된다")
void orderReducesOptionQuantity() throws Exception {
    // Given: 현재 재고 확인
    var beforeResponse = mockMvc.perform(get("/api/products/{productId}/options", productId)
            .header("Authorization", "Bearer " + token))
        .andReturn().getResponse().getContentAsString();
    int beforeQuantity = /* parse quantity from response */;

    // When: 주문 실행
    mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isCreated());

    // Then: 재고 재조회로 차감 확인
    mockMvc.perform(get("/api/products/{productId}/options", productId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].quantity").value(beforeQuantity - orderQuantity));
}
```

### 패턴: 부수 효과 검증

```java
@Test
@DisplayName("위시리스트 상품을 주문하면 위시리스트에서 제거된다")
void orderRemovesFromWishlist() throws Exception {
    // Given: 위시리스트에 상품 추가
    mockMvc.perform(post("/api/wishes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wishRequest)))
        .andExpect(status().isCreated());

    // When: 해당 상품 주문
    mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isCreated());

    // Then: 위시리스트 조회로 제거 확인
    mockMvc.perform(get("/api/wishes")
            .header("Authorization", "Bearer " + token)
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
}
```

---

## 3. 안티패턴

### Bad: 예외만 확인

```java
// 이것만으로는 불충분
@Test
void duplicateWish_throwsException() throws Exception {
    mockMvc.perform(post("/api/wishes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wishRequest)))
        .andExpect(status().isCreated());

    // 예외 발생만 확인 — 실제로 중복이 방지되었는지 모름
    mockMvc.perform(post("/api/wishes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wishRequest)))
        .andExpect(status().isBadRequest());
}
```

### Good: 상태 재조회로 확인

```java
@Test
void duplicateWish_isRejectedAndOriginalRemains() throws Exception {
    // Given: 위시 추가
    mockMvc.perform(post("/api/wishes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wishRequest)))
        .andExpect(status().isCreated());

    // When: 중복 추가 시도
    mockMvc.perform(post("/api/wishes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wishRequest)))
        .andExpect(status().isBadRequest());

    // Then: 조회로 1건만 존재하는지 확인
    mockMvc.perform(get("/api/wishes")
            .header("Authorization", "Bearer " + token)
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
}
```

---

## 4. 체크리스트

작동 변경 커밋 전 확인:

- [ ] 3줄 명세를 정의했는가?
- [ ] 테스트의 Then 절이 상태 조회인가?
- [ ] 예외만 확인하는 테스트는 없는가?
- [ ] 변경하지 않는 부분의 기존 테스트가 여전히 통과하는가?
