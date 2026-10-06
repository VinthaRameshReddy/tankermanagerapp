package com.tankermanager.repository;

import com.tankermanager.entity.AuthChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, Long> {
    @Query("SELECT c FROM AuthChallenge c JOIN FETCH c.user WHERE c.challengeId = :challengeId AND c.used = false")
    Optional<AuthChallenge> findByChallengeIdAndUsedFalse(@Param("challengeId") String challengeId);
}
