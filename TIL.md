# TIL

### Lombok `@Getter`로 엔티티 보일러플레이트 제거
엔티티 클래스에 `@Getter`를 붙이면 수동으로 작성한 getter 메서드를 모두 제거할 수 있다.
```java
@Entity
@Getter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int price;
    // getter 메서드 직접 작성 불필요
}
```


---
### Lombok Gradle 의존성 설정
```groovy
// 두 줄 모두 필요
compileOnly("org.projectlombok:lombok")
annotationProcessor("org.projectlombok:lombok")
```
- `compileOnly`: 컴파일 시점에만 클래스패스에 포함. 런타임에는 불필요.
- `annotationProcessor`: javac의 어노테이션 프로세서에 Lombok을 등록하여 컴파일 시점에 코드 생성.


---
### Spring 어노테이션은 왜 `annotationProcessor`가 필요 없는가?
| | Spring | Lombok |
|---|---|---|
| 동작 시점 | 런타임 | 컴파일 타임 |
| 방식 | 리플렉션 (`@Retention(RUNTIME)`) | javac 어노테이션 프로세서 (JSR 269) |
| Gradle 설정 | `implementation` | `compileOnly` + `annotationProcessor` |

`annotationProcessor`는 javac 플러그인 시스템을 사용하는 라이브러리에만 필요하다.
Spring은 런타임에 리플렉션으로 어노테이션을 읽으므로 이 메커니즘과 무관하다.

