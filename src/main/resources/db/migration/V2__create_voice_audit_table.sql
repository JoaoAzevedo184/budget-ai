-- V2__create_voice_audit_table.sql
-- Histórico de comandos de voz processados (auditoria).
-- SQL compatível com H2 (dev/teste) e PostgreSQL (prod).
--   - UUID: tipo nativo em ambos.
--   - transcript/reply: TEXT (tamanho livre; comandos e respostas variam).
--   - processed_at: TIMESTAMP (data + hora do processamento).

CREATE TABLE voice_audit (
                             id           UUID        NOT NULL,
                             transcript   TEXT        NOT NULL,
                             reply        TEXT        NOT NULL,
                             processed_at TIMESTAMP   NOT NULL,
                             CONSTRAINT pk_voice_audit PRIMARY KEY (id)
);

-- Índice para ordenar/filtrar o histórico pelo momento do processamento.
CREATE INDEX idx_voice_audit_processed_at ON voice_audit (processed_at);
