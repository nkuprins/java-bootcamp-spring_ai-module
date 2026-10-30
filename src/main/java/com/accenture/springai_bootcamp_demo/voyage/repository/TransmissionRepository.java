package com.accenture.springai_bootcamp_demo.voyage.repository;

import com.accenture.springai_bootcamp_demo.voyage.entity.Transmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransmissionRepository extends JpaRepository<Transmission, Long> {

    List<Transmission> findByVoyageIdOrderByIdAsc(String voyageId);

    List<Transmission> findByVoyageIdAndDeliveredFalseAndArrivalTurnLessThanEqual(String voyageId, int turn);

    List<Transmission> findByVoyageIdAndDeliveredTrueAndArrivalTurnGreaterThan(String voyageId, int turn);
}
