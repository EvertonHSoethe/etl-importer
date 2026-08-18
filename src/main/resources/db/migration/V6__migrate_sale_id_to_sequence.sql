-- V6: Migra estratégia de geração de ID da tabela sale de IDENTITY para SEQUENCE.
-- Motivação: allocationSize=500 no JPA requer uma sequence explícita para pré-alocação
-- de IDs em blocos, eliminando round-trips individuais ao banco por registro.

-- 1. Remover IDENTITY da coluna id (isso pode remover a sequence implícita associada)
ALTER TABLE sale ALTER COLUMN id DROP IDENTITY IF EXISTS;

-- 2. Dropar sequence anterior se existir (criada implicitamente pelo IDENTITY)
DROP SEQUENCE IF EXISTS sale_id_seq;

-- 3. Criar sequence com valor inicial baseado no MAX(id) existente
DO $$
DECLARE
    max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM sale;
    EXECUTE format('CREATE SEQUENCE sale_id_seq INCREMENT BY 500 START WITH %s', max_id + 1);
END $$;

-- 4. Vincular sequence à coluna como default
ALTER TABLE sale ALTER COLUMN id SET DEFAULT nextval('sale_id_seq');

-- 5. Vincular sequence à coluna (para DROP CASCADE)
ALTER SEQUENCE sale_id_seq OWNED BY sale.id;
