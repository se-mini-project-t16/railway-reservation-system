package com.railway.reservation.repository;

import com.railway.reservation.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {

    Optional<Train> findByNumber(String number);

    boolean existsByNumber(String number);

    List<Train> findAllByOrderByNumberAsc();

    /** Active trains that call at one of the origin stations and, later on the route, at one of the destinations. */
    @Query("select distinct t from Train t join t.stops a join t.stops b "
            + "where t.active = true and a.station.id in :fromIds and b.station.id in :toIds and a.seq < b.seq")
    List<Train> findServing(@Param("fromIds") Collection<Long> fromIds, @Param("toIds") Collection<Long> toIds);

    @Query("select distinct t from Train t join t.stops s where t.active = true and s.station.id = :stationId")
    List<Train> findCallingAt(@Param("stationId") Long stationId);
}
