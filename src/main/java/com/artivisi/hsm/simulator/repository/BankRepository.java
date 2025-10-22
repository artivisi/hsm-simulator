package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankRepository extends JpaRepository<Bank, UUID> {

    Optional<Bank> findByBankCode(String bankCode);

    List<Bank> findByBankType(Bank.BankType bankType);

    List<Bank> findByStatus(Bank.BankStatus status);

    List<Bank> findByBankTypeAndStatus(Bank.BankType bankType, Bank.BankStatus status);

    @Query("SELECT COUNT(b) FROM Bank b WHERE b.status = 'ACTIVE'")
    long countActiveBanks();

    @Query("SELECT COUNT(b) FROM Bank b WHERE b.bankType = :bankType AND b.status = 'ACTIVE'")
    long countActiveBanksByType(Bank.BankType bankType);

    boolean existsByBankCode(String bankCode);

    List<Bank> findByCountryCode(String countryCode);
}
