package com.foody.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.menus.entity.Menu;
import com.foody.menus.service.MenuService;
import com.foody.notifications.service.NotificationService;
import com.foody.orders.dto.CreateOrderRequest;
import com.foody.orders.dto.OrderItemRequest;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.entity.FulfillmentType;
import com.foody.orders.entity.Order;
import com.foody.orders.entity.OrderStatus;
import com.foody.orders.repository.OrderRepository;
import com.foody.products.entity.Product;
import com.foody.products.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock BusinessService businessService;
    @Mock MenuService menuService;
    @Mock ProductService productService;
    @Mock NotificationService notificationService;

    OrderServiceImpl orderService;

    static final Long CUSTOMER_ID = 1L;
    static final Long BUSINESS_ID = 10L;
    static final Long MENU_ID = 20L;
    static final Long PRODUCT_ID = 30L;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository, businessService, menuService, productService, notificationService);
    }

    private Business approvedBusiness() {
        Business business = new Business();
        business.setId(BUSINESS_ID);
        business.setStatus(BusinessStatus.APPROVED);
        return business;
    }

    private Menu menuForBusiness(Long businessId) {
        Menu menu = new Menu();
        menu.setId(MENU_ID);
        menu.setBusinessId(businessId);
        return menu;
    }

    private Product availableProduct() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setMenuId(MENU_ID);
        product.setName("Latte");
        product.setPrice(new BigDecimal("4.50"));
        product.setIsAvailable(true);
        return product;
    }

    @Test
    void createOrder_pickup_computesTotalAndSaves() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));
        when(productService.findById(PRODUCT_ID)).thenReturn(Optional.of(availableProduct()));
        when(menuService.findById(MENU_ID)).thenReturn(Optional.of(menuForBusiness(BUSINESS_ID)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(
                BUSINESS_ID, FulfillmentType.PICKUP,
                List.of(new OrderItemRequest(PRODUCT_ID, 2)), null);

        OrderResponse response = orderService.createOrder(CUSTOMER_ID, request);

        assertThat(response.totalAmount()).isEqualByComparingTo("9.00");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.deliveryAddress()).isNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Latte");
    }

    @Test
    void createOrder_delivery_requiresAddress() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));

        CreateOrderRequest request = new CreateOrderRequest(
                BUSINESS_ID, FulfillmentType.DELIVERY,
                List.of(new OrderItemRequest(PRODUCT_ID, 1)), null);

        assertThatThrownBy(() -> orderService.createOrder(CUSTOMER_ID, request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createOrder_rejectsUnavailableProduct() {
        Product unavailable = availableProduct();
        unavailable.setIsAvailable(false);

        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));
        when(productService.findById(PRODUCT_ID)).thenReturn(Optional.of(unavailable));
        when(menuService.findById(MENU_ID)).thenReturn(Optional.of(menuForBusiness(BUSINESS_ID)));

        CreateOrderRequest request = new CreateOrderRequest(
                BUSINESS_ID, FulfillmentType.PICKUP,
                List.of(new OrderItemRequest(PRODUCT_ID, 1)), null);

        assertThatThrownBy(() -> orderService.createOrder(CUSTOMER_ID, request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createOrder_rejectsProductFromAnotherBusiness() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.of(approvedBusiness()));
        when(productService.findById(PRODUCT_ID)).thenReturn(Optional.of(availableProduct()));
        when(menuService.findById(MENU_ID)).thenReturn(Optional.of(menuForBusiness(999L)));

        CreateOrderRequest request = new CreateOrderRequest(
                BUSINESS_ID, FulfillmentType.PICKUP,
                List.of(new OrderItemRequest(PRODUCT_ID, 1)), null);

        assertThatThrownBy(() -> orderService.createOrder(CUSTOMER_ID, request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createOrder_rejectsWhenBusinessNotApproved() {
        when(businessService.findByIdAndStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .thenReturn(Optional.empty());

        CreateOrderRequest request = new CreateOrderRequest(
                BUSINESS_ID, FulfillmentType.PICKUP,
                List.of(new OrderItemRequest(PRODUCT_ID, 1)), null);

        assertThatThrownBy(() -> orderService.createOrder(CUSTOMER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelOrder_succeedsWhenPending() {
        Order order = new Order();
        order.setId(100L);
        order.setCustomerUserId(CUSTOMER_ID);
        order.setBusinessId(BUSINESS_ID);
        order.setFulfillmentType(FulfillmentType.PICKUP);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("9.00"));

        when(orderRepository.findByIdAndCustomerUserId(100L, CUSTOMER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(100L, CUSTOMER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_rejectsWhenAlreadyAccepted() {
        Order order = new Order();
        order.setId(100L);
        order.setCustomerUserId(CUSTOMER_ID);
        order.setBusinessId(BUSINESS_ID);
        order.setFulfillmentType(FulfillmentType.PICKUP);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setTotalAmount(new BigDecimal("9.00"));

        when(orderRepository.findByIdAndCustomerUserId(100L, CUSTOMER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(100L, CUSTOMER_ID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void getOrderForCustomer_throwsWhenNotOwned() {
        when(orderRepository.findByIdAndCustomerUserId(100L, CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderForCustomer(100L, CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateOrderStatus_allowsPendingToAccepted() {
        Order order = new Order();
        order.setId(100L);
        order.setBusinessId(BUSINESS_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("9.00"));

        when(businessService.findByOwnerUserId(5L)).thenReturn(Optional.of(approvedBusiness()));
        when(orderRepository.findByIdAndBusinessId(100L, BUSINESS_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(5L, 100L, OrderStatus.ACCEPTED);

        assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void updateOrderStatus_rejectsSkippingStages() {
        Order order = new Order();
        order.setId(100L);
        order.setBusinessId(BUSINESS_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("9.00"));

        when(businessService.findByOwnerUserId(5L)).thenReturn(Optional.of(approvedBusiness()));
        when(orderRepository.findByIdAndBusinessId(100L, BUSINESS_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(5L, 100L, OrderStatus.READY))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void getBusinessOrders_filtersOwnerBusinessOnly() {
        when(businessService.findByOwnerUserId(5L)).thenReturn(Optional.of(approvedBusiness()));
        when(orderRepository.findByBusinessIdOrderByCreatedAtDesc(BUSINESS_ID)).thenReturn(List.of());

        List<OrderResponse> result = orderService.getBusinessOrders(5L, null);

        assertThat(result).isEmpty();
    }
}
