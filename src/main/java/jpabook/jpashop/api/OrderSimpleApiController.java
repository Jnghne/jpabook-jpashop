package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.*;

/**
 * XToOne 관계 성능 최적화
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * !!  잘못된 방법 !!
     * <엔티티를 직접 반환하면 안된다.>
     * 1. 엔티티를 직접 반환하면, 양방향인 연관관계일 때 엔티티를 Json으로 반환하는 과정에서 무한루프를 돈다.
     *      => 반대방향의 컬럼에 @JsonIgnore 어노테이션 추가해서 양방향 무한루프 없애기
     *
     * 2. @JsonIgnore를 추가해도 Order 엔티티에 Member, Delivery, OrderItems가 지연로딩을 사용하고 있다. 그러면, 프록시 객체가 할당되어 있는데 반환할떄 오류가 발생한다.
     *     => Hibernate5Module 을 Bean으로 등록하면 지연로딩 필드는 null로 세팅한 후 반환하게 한다.
     * @return
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        // 리턴하고자 하는 필드만 강제로 Lazy Loading !
        for (Order order : all) {
            order.getMember().getName();
            order.getMember().getAddress();
        }
        return all;
    }

    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        return orderRepository.findAllByString(new OrderSearch()).stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();

        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(toList());
        return result;
    }

    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        /**
         * 중요하지 않은 DTO에서 중요한 Order 엔티티를 파라미터로 받는것은 괜찮음.
         * 그러나 그 외에는 엔티티를 파라미터로 직접 넘기지 말고, 값을 뽑아서 넘기자
         */

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate(); // LAZY LOADING 발생
            orderStatus = order.getStatus(); // LAZY LOADING 발생
            address = order.getDelivery().getAddress(); // LAZY LOADING 발생
        }
    }


}
