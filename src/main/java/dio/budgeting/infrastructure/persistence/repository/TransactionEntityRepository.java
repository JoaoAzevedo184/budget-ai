package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.TransactionType;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionEntityRepository extends JpaRepository<TransactionEntity, UUID> {

    List<TransactionEntity> findByDateBetween(LocalDate from, LocalDate to);

    /**
     * Soma os valores de uma categoria (e tipo EXPENSE) dentro de um período.
     * COALESCE garante 0 quando não há registros, em vez de null.
     */
    @Query("""
           SELECT COALESCE(SUM(t.amount), 0)
           FROM TransactionEntity t
           WHERE t.category = :category
             AND t.type = :type
             AND t.date BETWEEN :from AND :to
           """)
    BigDecimal sumByCategoryAndPeriod(@Param("category") String category,
                                      @Param("type") TransactionType type,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);
}
