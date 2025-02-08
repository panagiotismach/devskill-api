package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.models.TrendingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;


@Repository
public interface TrendingRepositoryRepository extends JpaRepository<TrendingRepository, Long> {

    @Query("SELECT t FROM TrendingRepository t WHERE DATE(t.createdAt) = DATE(:date)")
    Page<TrendingRepository> findAllByToday(@Param("date") LocalDateTime date, Pageable pageable);

}

