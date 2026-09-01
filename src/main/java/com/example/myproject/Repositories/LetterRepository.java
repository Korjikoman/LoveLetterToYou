package com.example.myproject.Repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.myproject.Model.Letter;
import com.example.myproject.Projection.ActiveLetterRef;

@Repository
public interface LetterRepository extends JpaRepository <Letter, Long>{
    void deleteByPublicToken(String publicToken);
    Optional<Letter> findByPublicToken(String publicToken);
    List<Letter> findByAuthorEmail(String authorEmail);
    Optional<Letter> findByPublicTokenAndUser_Email(String publicToken, String userEmail);

    Optional<Letter> findByPublicTokenAndExpiresAtAfter(String publicToken, Instant now);
    List<Letter> findAllByUser_EmailAndExpiresAtAfterOrderByIdDesc(String email,Instant now);
    boolean existsByPublicToken(String publicToken);


    @Query(
        """
        SELECT l.id as id,
            l.publicToken as publicToken,
            l.version as version,
            l.expiresAt as expiresAt
        FROM Letter l
        WHERE lower(l.user.email) = lower(:email)
            AND l.expiresAt > :now
            AND (:beforeId IS null OR l.id < :beforeId)
        ORDER BY l.id DESC        
        """
    )
    List<ActiveLetterRef> findActiveLetterRefs(@Param("email") String email, @Param("now") Instant now, @Param("beforeId") Long beforeId, Pageable pageable);

    @Query("""
        SELECT l FROM Letter l
        WHERE l.publicToken in :missingTokens
        AND l.user.email = :email
        AND l.expiresAt > :now
        """)
    List<Letter>findActiveByTokens(@Param("missing") List<String> missingTokens, @Param("email") String email, @Param("now") Instant now);


    Optional<Letter> findByPublicTokenAndAuthorEmailIgnoreCaseAndExpiresAtAfter(String publicToken, String email, Instant now);

    Optional<Letter> findByPublicTokenAndSecurityKeyAndExpiresAtAfter(String publicToken, String securityKey, Instant now);
}
