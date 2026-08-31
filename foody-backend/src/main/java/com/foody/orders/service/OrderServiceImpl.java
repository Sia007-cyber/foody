package com.foody.orders.service;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.menus.entity.Menu;
import com.foody.menus.service.MenuService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final BusinessService businessService;
    private final MenuService menuService;
    private final ProductService productService;

    OrderServiceImpl(OrderRepository orderRepository, BusinessService businessService,
                     MenuService menuService, ProductService productService) {
        this.orderRepository = orderRepository;
        this.businessService = businessService;
        this.menuService = menuService;
        this.productService = productService;
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
        return OrderResponse.from(orderRepository.save(order));
    }

    private Order findOwnedOrder(Long orderId, Long customerUserId) {
        // Not-found-or-not-owned collapse to the same 404: avoids leaking whether an
        // order ID exists at all to a customer who doesn't own it.
        return orderRepository.findByIdAndCustomerUserId(orderId, customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
}
