package com.railway.reservation.repository;

import com.railway.reservation.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByTrainNo(String trainNo);
    boolean existsByTrainNo(String trainNo);
    List<Train> findBySourceStationIgnoreCaseAndDestinationStationIgnoreCase(String sourceStation, String destinationStation);

    @Query("SELECT DISTINCT t FROM Train t WHERE " +
           "(LOWER(t.sourceStation) IN :srcAliases OR LOWER(t.sourceStation) = LOWER(:src)) AND " +
           "(LOWER(t.destinationStation) IN :dstAliases OR LOWER(t.destinationStation) = LOWER(:dst))")
    List<Train> findBySourceAndDestinationAliases(
            @Param("src") String src,
            @Param("srcAliases") List<String> srcAliases,
            @Param("dst") String dst,
            @Param("dstAliases") List<String> dstAliases);
}

