package com.example.soso.community.pollboard.repository;

import com.example.soso.community.pollboard.domain.entity.Poll;
import com.example.soso.users.domain.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 투표 게시글 레포지토리
 */
@Repository
public interface PollRepository extends JpaRepository<Poll, Long>, PollRepositoryCustom {

    /**
     * ID로 삭제되지 않은 투표 게시글 조회
     */
    @Query("SELECT p FROM Poll p WHERE p.id = :id AND p.deleted = false")
    Optional<Poll> findByIdAndDeletedFalse(@Param("id") Long id);

    /**
     * 커서 기반 페이지네이션 - 전체 조회 (삭제되지 않은 게시글)
     */
    @Query("SELECT p FROM Poll p WHERE p.deleted = false AND p.id < :cursor ORDER BY p.id DESC")
    List<Poll> findAllByCursorWithoutStatus(@Param("cursor") Long cursor, Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 첫 페이지 (삭제되지 않은 게시글)
     */
    @Query("SELECT p FROM Poll p WHERE p.deleted = false ORDER BY p.id DESC")
    List<Poll> findAllWithoutStatus(Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 진행 중 투표 조회
     */
    @Query("SELECT p FROM Poll p WHERE p.deleted = false AND p.closedAt > :now AND p.id < :cursor ORDER BY p.id DESC")
    List<Poll> findInProgressByCursor(@Param("cursor") Long cursor, @Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 진행 중 투표 첫 페이지
     */
    @Query("SELECT p FROM Poll p WHERE p.deleted = false AND p.closedAt > :now ORDER BY p.id DESC")
    List<Poll> findInProgress(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 완료된 투표 조회
     */
    @Query("SELECT p FROM Poll p WHERE p.deleted = false AND p.closedAt <= :now AND p.id < :cursor ORDER BY p.id DESC")
    List<Poll> findCompletedByCursor(@Param("cursor") Long cursor, @Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 완료된 투표 첫 페이지
     */
    @Query("SELECT p FROM Poll p WHERE p.deleted = false AND p.closedAt <= :now ORDER BY p.id DESC")
    List<Poll> findCompleted(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 사용자가 작성한 투표 게시글 수 조회
     */
    long countByUserAndDeletedFalse(Users user);

    /**
     * 전체 투표 게시글 수 조회 (삭제되지 않은 것만)
     */
    long countByDeletedFalse();

    /**
     * 진행 중인 투표 게시글 수 조회
     */
    @Query("SELECT COUNT(p) FROM Poll p WHERE p.deleted = false AND p.closedAt > :now")
    long countInProgress(@Param("now") LocalDateTime now);

    /**
     * 완료된 투표 게시글 수 조회
     */
    @Query("SELECT COUNT(p) FROM Poll p WHERE p.deleted = false AND p.closedAt <= :now")
    long countCompleted(@Param("now") LocalDateTime now);
}
