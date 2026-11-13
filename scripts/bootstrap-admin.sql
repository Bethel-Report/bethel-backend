-- Administrador inicial de desenvolvimento. Executar explicitamente no banco local.
-- A senha e a identidade de login devem ser configuradas no Keycloak com este e-mail.
-- A data de nascimento e o endereço são dados fictícios para campos obrigatórios.
-- O Bethel base usa dados de desenvolvimento e não altera o vínculo do administrador.
BEGIN;

INSERT INTO bethels (number, name, city, address, email, sponsoring_lodge, active)
SELECT
    (SELECT COALESCE(MAX(number), 0) + 1 FROM bethels),
    'Grande Conselho', 'Não informado', 'Não informado',
    'admin@bethelreport.local', 'Não informado', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM bethels WHERE lower(name) = 'grande conselho'
);

INSERT INTO users (
    email, name, birthday, active, city, address,
    volunteer, masonic_relationship, member, sponsorship
)
SELECT
    'admin@bethelreport.local', 'Administrador do Grande Conselho',
    DATE '2000-01-01', TRUE, 'Não informado', 'Não informado',
    FALSE, FALSE, FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE lower(email) = 'admin@bethelreport.local'
);

INSERT INTO permissions (user_id, role, bethel_id, active)
SELECT id, 'GRANDE_CONSELHO', NULL, TRUE
FROM users
WHERE lower(email) = 'admin@bethelreport.local'
ON CONFLICT (user_id) DO UPDATE
SET role = EXCLUDED.role, bethel_id = NULL, active = TRUE;

UPDATE users SET active = TRUE WHERE lower(email) = 'admin@bethelreport.local';

COMMIT;
