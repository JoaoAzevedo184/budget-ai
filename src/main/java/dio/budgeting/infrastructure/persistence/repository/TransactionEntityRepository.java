package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.TransactionType;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.domain.Pageable;
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
     * Soma os valores de uma categoria (e tipo informado) dentro de um período.
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

    /**
     * Soma total de um tipo (INCOME ou EXPENSE) no período,
     * independente de categoria. Base para o cálculo do saldo.
     */
    @Query("""
           SELECT COALESCE(SUM(t.amount), 0)
           FROM TransactionEntity t
           WHERE t.type = :type
             AND t.date BETWEEN :from AND :to
           """)
    BigDecimal sumByTypeAndPeriod(@Param("type") TransactionType type,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    /**
     * Top categorias por total de um tipo no período, ordenado desc.
     * Retorna uma projeção (categoria + total). Use Pageable para limitar.
     */
    @Query("""
           SELECT t.category AS category, SUM(t.amount) AS total
           FROM TransactionEntity t
           WHERE t.type = :type
             AND t.date BETWEEN :from AND :to
           GROUP BY t.category
           ORDER BY SUM(t.amount) DESC
           """)
    List<CategoryTotalProjection> topCategories(@Param("type") TransactionType type,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to,
                                                Pageable pageable);

    /** Projeção para o resultado agregado de top categorias. */
    interface CategoryTotalProjection {
        String getCategory();

        BigDecimal getTotal();
    }
}
