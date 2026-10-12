package com.wultra.wallet.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.wultra.wallet.entity.WalletEntity;

import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w from WalletEntity w where w.id = :id")
    Optional<WalletEntity> findWithLock(UUID id);

    List<WalletEntity> findByUserId(UUID userId);

}
