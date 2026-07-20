-- V5: Corrige schema para alinhar com as entidades JPA (ImportExecution e Sale)
-- Resolve divergências entre as migrations V1/V2 e os mapeamentos Hibernate.

-- 1. Adiciona coluna 'filename' em import_execution
-- A entidade ImportExecution possui @Column(name = "filename", nullable = false),
-- mas a tabela original (V2) não incluía essa coluna.
ALTER TABLE import_execution
    ADD COLUMN IF NOT EXISTS filename VARCHAR(255) NOT NULL DEFAULT 'unknown';

-- 2. Remove coluna 'execution_id' de import_execution
-- A coluna foi criada na V2 mas não possui correspondência na entidade JPA.
ALTER TABLE import_execution
    DROP COLUMN IF EXISTS execution_id;

-- 3. Renomeia 'total_line' para 'total_lines' em import_execution
-- A entidade JPA mapeia o campo como @Column(name = "total_lines").
ALTER TABLE import_execution
    RENAME COLUMN total_line TO total_lines;

-- 4. Aplica NOT NULL em 'started_date'
-- A entidade possui @Column(nullable = false, updatable = false), mas a V2 criou
-- a coluna sem a constraint NOT NULL.
ALTER TABLE import_execution
    ALTER COLUMN started_date SET NOT NULL;

-- 5. Adiciona coluna 'sale_id' na tabela sale
-- A entidade Sale possui @Column(name = "sale_id", nullable = false),
-- mas a V1 não incluía essa coluna.
ALTER TABLE sale
    ADD COLUMN IF NOT EXISTS sale_id BIGINT NOT NULL DEFAULT 0;
