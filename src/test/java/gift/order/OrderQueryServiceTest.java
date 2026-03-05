package gift.order;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

    @Test
    @DisplayName("회원의 주문 목록을 페이지네이션으로 조회한다")
    void findByMemberId() {
        var pageable = PageRequest.of(0, 10);
        var order = mock(Order.class);
        var page = new PageImpl<>(List.of(order), pageable, 1);
        given(orderRepository.findByMemberId(1L, pageable)).willReturn(page);

        Page<Order> result = orderQueryService.findByMemberId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("주문이 없으면 빈 페이지를 반환한다")
    void findByMemberId_Empty() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<Order>(List.of(), pageable, 0);
        given(orderRepository.findByMemberId(999L, pageable)).willReturn(page);

        Page<Order> result = orderQueryService.findByMemberId(999L, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
