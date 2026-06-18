-- V1__create_transactions_table.sql
-- Cria a tabela de transações.
-- SQL escrito para funcionar tanto em H2 (dev/teste) quanto em PostgreSQL (prod).
--   - UUID: tipo nativo em ambos os bancos.
--   - NUMERIC(19,2): padrão para valores monetários (BigDecimal scale 2).
--   - type: VARCHAR (a entidade usa @Enumerated(EnumType.STRING), grava INCOME/EXPENSE).

CREATE TABLE transactions (
                              id               UUID            NOT NULL,
                              description      VARCHAR(120)    NOT NULL,
                              amount           NUMERIC(19, 2)  NOT NULL,
                              type             VARCHAR(20)     NOT NULL,
                              category         VARCHAR(255)    NOT NULL,
                              transaction_date DATE            NOT NULL,
                              CONSTRAINT pk_transactions PRIMARY KEY (id)
);

-- Índice para acelerar as consultas por período e por categoria+período
-- (findByDateBetween e sumByCategoryAndPeriod).
CREATE INDEX idx_transactions_date     ON transactions (transaction_date);
CREATE INDEX idx_transactions_cat_date ON transactions (category, transaction_date);