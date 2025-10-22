package com.artivisi.hsm.simulator.repository;

import com.artivisi.hsm.simulator.entity.Terminal;
import com.artivisi.hsm.simulator.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, UUID> {

    Optional<Terminal> findByTerminalId(String terminalId);

    List<Terminal> findByBank(Bank bank);

    List<Terminal> findByBankId(UUID bankId);

    List<Terminal> findByTerminalType(Terminal.TerminalType terminalType);

    List<Terminal> findByStatus(Terminal.TerminalStatus status);

    List<Terminal> findByBankAndStatus(Bank bank, Terminal.TerminalStatus status);

    List<Terminal> findByTerminalTypeAndStatus(Terminal.TerminalType terminalType, Terminal.TerminalStatus status);

    @Query("SELECT COUNT(t) FROM Terminal t WHERE t.bank.id = :bankId AND t.status = 'ACTIVE'")
    long countActiveTerminalsByBank(@Param("bankId") UUID bankId);

    @Query("SELECT COUNT(t) FROM Terminal t WHERE t.terminalType = :terminalType AND t.status = 'ACTIVE'")
    long countActiveTerminalsByType(@Param("terminalType") Terminal.TerminalType terminalType);

    boolean existsByTerminalId(String terminalId);

    List<Terminal> findByLocationContainingIgnoreCase(String location);
}
