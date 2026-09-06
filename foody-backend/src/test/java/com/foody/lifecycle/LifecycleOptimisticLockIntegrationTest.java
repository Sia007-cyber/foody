package com.foody.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.AbstractContainerBaseTest;
import com.foody.orders.entity.Order;
import com.foody.orders.entity.OrderStatus;
import com.foody.reservations.entity.Reservation;
import com.foody.reservations.entity.ReservationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.OptimisticLockException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real-MySQL verification using separate threads, transactions and persistence contexts. */
class LifecycleOptimisticLockIntegrationTest extends AbstractContainerBaseTest {

    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbcTemplate;
    @PersistenceContext EntityManager entityManager;

    @Test
    void orderSequentialUpdatesIncrementVersion() {
        long id = insertOrder();

        update(Order.class, id, (order, ignored) -> order.setStatus(OrderStatus.ACCEPTED));
        update(Order.class, id, (order, ignored) -> order.setStatus(OrderStatus.PREPARING));

        assertThat(orderState(id)).containsExactly("PREPARING", 2L);
    }

    @Test
    void ownerAcceptedVsCustomerCancelled_hasOneWinnerAndOneConflict() throws Exception {
        long id = insertOrder();

        RaceResult result = race(Order.class, id,
                (order, branch) -> order.setStatus(branch == 0 ? OrderStatus.ACCEPTED : OrderStatus.CANCELLED));

        assertOneWinnerAndConflict(result);
        assertThat(orderState(id).get(0)).isIn("ACCEPTED", "CANCELLED");
        assertThat(orderState(id).get(1)).isEqualTo(1L);
    }

    @Test
    void twoOwnerOrderTransitions_haveOneWinnerAndOneConflict() throws Exception {
        long id = insertOrder();

        RaceResult result = race(Order.class, id,
                (order, branch) -> order.setStatus(branch == 0 ? OrderStatus.ACCEPTED : OrderStatus.REJECTED));

        assertOneWinnerAndConflict(result);
        assertThat(orderState(id).get(0)).isIn("ACCEPTED", "REJECTED");
        assertThat(orderState(id).get(1)).isEqualTo(1L);
    }

    @Test
    void reservationSequentialUpdatesIncrementVersion() {
        long id = insertReservation();

        update(Reservation.class, id,
                (reservation, ignored) -> reservation.setStatus(ReservationStatus.CONFIRMED));
        update(Reservation.class, id,
                (reservation, ignored) -> reservation.setStatus(ReservationStatus.COMPLETED));

        assertThat(reservationState(id)).containsExactly("COMPLETED", 2L);
    }

    @Test
    void ownerConfirmedVsCustomerCancelled_hasOneWinnerAndOneConflict() throws Exception {
        long id = insertReservation();

        RaceResult result = race(Reservation.class, id, (reservation, branch) -> reservation.setStatus(
                branch == 0 ? ReservationStatus.CONFIRMED : ReservationStatus.CANCELLED));

        assertOneWinnerAndConflict(result);
        assertThat(reservationState(id).get(0)).isIn("CONFIRMED", "CANCELLED");
        assertThat(reservationState(id).get(1)).isEqualTo(1L);
    }

    @Test
    void twoOwnerReservationTransitions_haveOneWinnerAndOneConflict() throws Exception {
        long id = insertReservation();

        RaceResult result = race(Reservation.class, id, (reservation, branch) -> reservation.setStatus(
                branch == 0 ? ReservationStatus.CONFIRMED : ReservationStatus.REJECTED));

        assertOneWinnerAndConflict(result);
        assertThat(reservationState(id).get(0)).isIn("CONFIRMED", "REJECTED");
        assertThat(reservationState(id).get(1)).isEqualTo(1L);
    }

    private <T> RaceResult race(Class<T> entityType, long id, BiConsumer<T, Integer> mutation)
            throws Exception {
        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch startWrites = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Throwable>> futures = List.of(0, 1).stream()
                    .map(branch -> executor.submit(() -> transition(
                            entityType, id, branch, mutation, bothLoaded, startWrites)))
                    .toList();

            assertThat(bothLoaded.await(10, TimeUnit.SECONDS)).isTrue();
            startWrites.countDown();

            return new RaceResult(
                    futures.get(0).get(10, TimeUnit.SECONDS),
                    futures.get(1).get(10, TimeUnit.SECONDS));
        } finally {
            startWrites.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private <T> Throwable transition(Class<T> entityType, long id, int branch,
                                     BiConsumer<T, Integer> mutation,
                                     CountDownLatch bothLoaded, CountDownLatch startWrites) {
        try {
            transactionTemplate().executeWithoutResult(status -> {
                T entity = entityManager.find(entityType, id);
                bothLoaded.countDown();
                await(startWrites);
                mutation.accept(entity, branch);
                entityManager.flush();
            });
            return null;
        } catch (Throwable error) {
            return error;
        }
    }

    private <T> void update(Class<T> entityType, long id, BiConsumer<T, Integer> mutation) {
        transactionTemplate().executeWithoutResult(status -> {
            T entity = entityManager.find(entityType, id);
            mutation.accept(entity, 0);
            entityManager.flush();
        });
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new JpaTransactionManager(entityManagerFactory));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for concurrent transition");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while coordinating concurrent transition", error);
        }
    }

    private static void assertOneWinnerAndConflict(RaceResult result) {
        assertThat(java.util.stream.Stream.of(result.first(), result.second())
                .filter(error -> error == null).count())
                .isEqualTo(1);
        Throwable loser = result.first() != null ? result.first() : result.second();
        assertThat(loser).isInstanceOfAny(
                OptimisticLockingFailureException.class, OptimisticLockException.class);
    }

    private long insertOrder() {
        return insert("""
                INSERT INTO orders
                    (customer_user_id, business_id, fulfillment_type, status, total_amount)
                VALUES (1, 1, 'PICKUP', 'PENDING', 10.00)
                """);
    }

    private long insertReservation() {
        return insert("""
                INSERT INTO reservations
                    (business_id, customer_user_id, reservation_date, reservation_time, guest_count, status)
                VALUES (1, 1, ?, '19:00:00', 2, 'PENDING')
                """, LocalDate.now().plusDays(1));
    }

    private long insert(String sql, Object... parameters) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    private List<Object> orderState(long id) {
        return jdbcTemplate.queryForObject("SELECT status, version FROM orders WHERE id = ?", (rs, row) ->
                List.of(rs.getString("status"), rs.getLong("version")), id);
    }

    private List<Object> reservationState(long id) {
        return jdbcTemplate.queryForObject("SELECT status, version FROM reservations WHERE id = ?", (rs, row) ->
                List.of(rs.getString("status"), rs.getLong("version")), id);
    }

    private record RaceResult(Throwable first, Throwable second) {}
}
