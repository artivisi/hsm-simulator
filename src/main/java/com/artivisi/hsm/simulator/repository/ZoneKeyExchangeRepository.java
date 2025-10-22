package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.ZoneKeyExchange;
import com.artivisi.hsm.simulator.entity.Bank;
import com.artivisi.hsm.simulator.entity.MasterKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZoneKeyExchangeRepository extends JpaRepository<ZoneKeyExchange, UUID> {

    Optional<ZoneKeyExchange> findByExchangeId(String exchangeId);

    List<ZoneKeyExchange> findBySourceBank(Bank sourceBank);

    List<ZoneKeyExchange> findByDestinationBank(Bank destinationBank);

    List<ZoneKeyExchange> findBySourceBankOrDestinationBank(Bank sourceBank, Bank destinationBank);

    List<ZoneKeyExchange> findByExchangeStatus(ZoneKeyExchange.ExchangeStatus exchangeStatus);

    List<ZoneKeyExchange> findByExchangeType(ZoneKeyExchange.ExchangeType exchangeType);

    List<ZoneKeyExchange> findByZmk(MasterKey zmk);

    List<ZoneKeyExchange> findByExchangedKey(MasterKey exchangedKey);

    @Query("SELECT z FROM ZoneKeyExchange z WHERE z.sourceBank.id = :bankId OR z.destinationBank.id = :bankId")
    List<ZoneKeyExchange> findExchangesByBank(@Param("bankId") UUID bankId);

    @Query("SELECT z FROM ZoneKeyExchange z WHERE (z.sourceBank.id = :sourceBankId AND z.destinationBank.id = :destBankId) " +
           "OR (z.sourceBank.id = :destBankId AND z.destinationBank.id = :sourceBankId)")
    List<ZoneKeyExchange> findExchangesBetweenBanks(@Param("sourceBankId") UUID sourceBankId, 
                                                      @Param("destBankId") UUID destBankId);

    @Query("SELECT z FROM ZoneKeyExchange z WHERE z.exchangeStatus = :status " +
           "AND z.initiatedAt < :before")
    List<ZoneKeyExchange> findByStatusAndInitiatedBefore(
        @Param("status") ZoneKeyExchange.ExchangeStatus status,
        @Param("before") LocalDateTime before);

    boolean existsByExchangeId(String exchangeId);

    @Query("SELECT COUNT(z) FROM ZoneKeyExchange z WHERE z.exchangeStatus = :status")
    long countByStatus(@Param("status") ZoneKeyExchange.ExchangeStatus status);
}
