package com.tankermanager.repository;

import com.tankermanager.entity.Trip;
import com.tankermanager.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);
    List<Trip> findByOperatorIdAndStatus(Long operatorId, TripStatus status);
    List<Trip> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<Trip> findByDriverIdAndStatusNotIn(Long driverId, List<TripStatus> statuses);
    Optional<Trip> findByIdAndOperatorId(Long id, Long operatorId);

    @Query("SELECT t FROM Trip t JOIN FETCH t.tanker WHERE t.trackingToken = :token")
    Optional<Trip> findByTrackingToken(@Param("token") String trackingToken);

    Optional<Trip> findByTripCode(String tripCode);
    long countByOperatorIdAndStatus(Long operatorId, TripStatus status);
    long countByTankerIdAndStatus(Long tankerId, TripStatus status);
    long countByDriverIdAndStatus(Long driverId, TripStatus status);

    List<Trip> findByTankerIdAndStatusOrderByQueuePositionAsc(Long tankerId, TripStatus status);

    @Query("SELECT COALESCE(MAX(t.queuePosition), 0) FROM Trip t WHERE t.tanker.id = :tankerId AND t.status = :status")
    Integer maxQueuePosition(@Param("tankerId") Long tankerId, @Param("status") TripStatus status);

    @Query("""
            SELECT COUNT(t) > 0 FROM Trip t
            WHERE t.tanker.id = :tankerId
              AND t.status IN :statuses
              AND (:excludeId IS NULL OR t.id <> :excludeId)
            """)
    boolean existsActiveOnTanker(
            @Param("tankerId") Long tankerId,
            @Param("statuses") Collection<TripStatus> statuses,
            @Param("excludeId") Long excludeId);

    List<Trip> findByOperatorIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Long operatorId, Instant from, Instant to);

    List<Trip> findByOperatorIdAndTankerIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Long operatorId, Collection<Long> tankerIds, Instant from, Instant to);

    List<Trip> findByOperatorIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Long operatorId, Instant from);

    List<Trip> findByOperatorIdAndTankerIdInAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long operatorId, Collection<Long> tankerIds, Instant from);
}
