package com.foody.orders.service;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.menus.entity.Menu;
import com.foody.menus.service.MenuService;
import com.foody.notifications.entity.NotificationType;
import com.foody.notifications.service.NotificationService;
import com.foody.orders.dto.CreateOrderRequest;
import com.foody.orders.dto.OrderItemRequest;
import com.foody.orders.dto.OrderResponse;
import com.foody.orders.entity.FulfillmentType;
import com.foody.orders.entity.Order;
import com.foody.orders.entity.OrderItem;
import com.foody.orders.entity.OrderStatus;
import com.foody.orders.repository.OrderRepository;
import com.foody.products.entity.Product;
import com.foody.products.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrderServiceImpl implements OrderService {

    // Business-owner-driven transitions, per the Phase 1 state machine:
    // PENDING -> ACCEPTED -> PREPARING -> READY -> COMPLETED
    //         -> REJECTED
    // (CANCELLED is customer-only, handled separately in cancelOrder.)
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_BUSINESS_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED),
            OrderStatus.ACCEPTED, Set.of(OrderStatus.PREPARING),
            OrderStatus.PREPARING, Set.of(OrderStatus.READY),
            OrderStatus.READY, Set.of(OrderStatus.COMPLETED)
    );

    private final OrderRepository orderRepository;
    private final BusinessService businessService;
    private final MenuService menuService;
    private final ProductService productService;
    private final NotificationService notificationService;

    OrderServiceImpl(OrderRepository orderRepository, BusinessService businessService,
                     MenuService menuService, ProductService productService,
                     NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.businessService = businessService;
        this.menuService = menuService;
        this.productService = productService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Long customerUserId, CreateOrderRequest request) {
        Business business = businessService.findByIdAndStatus(request.businessId(), BusinessStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + request.businessId()));

        if (request.fulfillmentType() == FulfillmentType.DELIVERY
                && (request.deliveryAddress() == null || request.deliveryAddress().isBlank())) {
            throw new InvalidRequestException("deliveryAddress is required for DELIVERY orders");
        }

        Order order = new Order();
        order.setCustomerUserId(customerUserId);
        order.setBusinessId(business.getId());
        order.setFulfillmentType(request.fulfillmentType());
        order.setStatus(OrderStatus.PENDING);
        // Only persisted for DELIVERY orders — a PICKUP order has no meaningful address.
        order.setDeliveryAddress(
                request.fulfillmentType() == FulfillmentType.DELIVERY ? request.deliveryAddress() : null);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = buildOrderItem(business.getId(), itemRequest);
            order.addItem(item);
            total = total.add(item.getSubtotal());
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        notificationService.notify(business.getOwnerUserId(), NotificationType.NEW_ORDER,
                "سفارش جدید",
                "یک سفارش جدید به شماره #" + saved.getId() + " برای کسب‌وکار شما ثبت شد.",
                "ORDER", saved.getId());

        return OrderResponse.from(saved);
    }

    private OrderItem buildOrderItem(Long businessId, OrderItemRequest itemRequest) {
        Product product = productService.findById(itemRequest.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));

        Menu menu = menuService.findById(product.getMenuId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found for product: " + product.getId()));

        if (!menu.getBusinessId().equals(businessId)) {
            throw new InvalidRequestException(
                    "Product " + product.getId() + " does not belong to business " + businessId);
        }
        if (!Boolean.TRUE.equals(product.getIsAvailable())) {
            throw new InvalidRequestException("Product " + product.getId() + " is not currently available");
        }

        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(itemRequest.quantity());
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderForCustomer(Long orderId, Long customerUserId) {
        return OrderResponse.from(findOwnedOrder(orderId, customerUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long customerUserId) {
        return orderRepository.findByCustomerUserIdOrderByCreatedAtDesc(customerUserId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long customerUserId) {
        Order order = findOwnedOrder(orderId, customerUserId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Order " + orderId + " can no longer be cancelled (status: " + order.getStatus() + ")");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        businessService.findById(saved.getBusinessId()).ifPresent(business ->
                notificationService.notify(business.getOwnerUserId(), NotificationType.ORDER_STATUS_CHANGED,
                        "لغو سفارش",
                        "سفارش #" + saved.getId() + " توسط مشتری لغو شد.",
                        "ORDER", saved.getId()));

        return OrderResponse.from(saved);
    }

    private Order findOwnedOrder(Long orderId, Long customerUserId) {
        // Not-found-or-not-owned collapse to the same 404: avoids leaking whether an
        // order ID exists at all to a customer who doesn't own it.
        return orderRepository.findByIdAndCustomerUserId(orderId, customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getBusinessOrders(Long ownerUserId, OrderStatus statusFilter) {
        Business business = requireOwnedBusiness(ownerUserId);
        List<Order> orders = statusFilter != null
                ? orderRepository.findByBusinessIdAndStatusOrderByCreatedAtDesc(business.getId(), statusFilter)
                : orderRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId());
        return orders.stream().map(OrderResponse::from).toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long ownerUserId, Long orderId, OrderStatus newStatus) {
        Business business = requireOwnedBusiness(ownerUserId);
        Order order = orderRepository.findByIdAndBusinessId(orderId, business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        Set<OrderStatus> allowedNext = VALID_BUSINESS_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot move order " + orderId + " from " + order.getStatus() + " to " + newStatus);
        }
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        notificationService.notify(saved.getCustomerUserId(), NotificationType.ORDER_STATUS_CHANGED,
                "به‌روزرسانی سفارش",
                "وضعیت سفارش #" + saved.getId() + " به " + statusLabel(newStatus) + " تغییر کرد.",
                "ORDER", saved.getId());

        return OrderResponse.from(saved);
    }

    private String statusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "در انتظار تایید";
            case ACCEPTED -> "تایید شده";
            case PREPARING -> "در حال آماده‌سازی";
            case READY -> "آماده تحویل";
            case COMPLETED -> "تکمیل شده";
            case REJECTED -> "رد شده";
            case CANCELLED -> "لغو شده";
        };
    }

    private Business requireOwnedBusiness(Long ownerUserId) {
        return businessService.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No business found for this owner"));
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return orderRepository.count();
    }
}
