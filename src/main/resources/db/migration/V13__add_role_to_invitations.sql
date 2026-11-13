ALTER TABLE invitations
ADD COLUMN role VARCHAR(30);

ALTER TABLE invitations
ADD CONSTRAINT chk_permissions_role
    CHECK (
        role IN (
            'GRANDE_CONSELHO',
            'CONSELHO_DE_BETHEL',
            'MEMBRO_ATIVO',
            'MEMBRO_DE_MAIORIDADE'
        )
    );