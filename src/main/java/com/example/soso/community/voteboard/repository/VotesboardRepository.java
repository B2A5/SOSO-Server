package com.example.soso.community.voteboard.repository;

import com.example.soso.community.voteboard.domain.entity.Votesboard;
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
public interface VotesboardRepository extends JpaRepository<Votesboard, Long>, VotesboardRepositoryCustom {

    /**
     * ID로 삭제되지 않은 투표 게시글 조회
     */
    @Query("SELECT v FROM Votesboard v WHERE v.id = :id AND v.deleted = false")
    Optional<Votesboard> findByIdAndDeletedFalse(@Param("id") Long id);

    /**
     * 커서 기반 페이지네이션 - 전체 조회 (삭제되지 않은 게시글)
     */
    @Query("SELECT v FROM Votesboard v WHERE v.deleted = false AND v.id < :cursor ORDER BY v.id DESC")
    List<Votesboard> findAllByCursorWithoutStatus(@Param("cursor") Long cursor, Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 첫 페이지 (삭제되지 않은 게시글)
     */
    @Query("SELECT v FROM Votesboard v WHERE v.deleted = false ORDER BY v.id DESC")
    List<Votesboard> findAllWithoutStatus(Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 진행 중 투표 조회
     */
    @Query("SELECT v FROM Votesboard v WHERE v.deleted = false AND v.endTime > :now AND v.id < :cursor ORDER BY v.id DESC")
    List<Votesboard> findInProgressByCursor(@Param("cursor") Long cursor, @Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 진행 중 투표 첫 페이지
     */
    @Query("SELECT v FROM Votesboard v WHERE v.deleted = false AND v.endTime > :now ORDER BY v.id DESC")
    List<Votesboard> findInProgress(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 완료된 투표 조회
     */
    @Query("SELECT v FROM Votesboard v WHERE v.deleted = false AND v.endTime <= :now AND v.id < :cursor ORDER BY v.id DESC")
    List<Votesboard> findCompletedByCursor(@Param("cursor") Long cursor, @Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 커서 기반 페이지네이션 - 완료된 투표 첫 페이지
     */
    @Query("SELECT v FROM Votesboard v WHERE v.deleted = false AND v.endTime <= :now ORDER BY v.id DESC")
    List<Votesboard> findCompleted(@Param("now") LocalDateTime now, Pageable pageable);

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
    @Query("SELECT COUNT(v) FROM Votesboard v WHERE v.deleted = false AND v.endTime > :now")
    long countInProgress(@Param("now") LocalDateTime now);

    /**
     * 완료된 투표 게시글 수 조회
     */
    @Query("SELECT COUNT(v) FROM Votesboard v WHERE v.deleted = false AND v.endTime <= :now")
    long countCompleted(@Param("now") LocalDateTime now);
}
