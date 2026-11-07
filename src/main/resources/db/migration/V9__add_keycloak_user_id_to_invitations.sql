ALTER TABLE invitations
ADD COLUMN keycloak_user_id VARCHAR(255);

ALTER TABLE invitations
ADD CONSTRAINT uk_invitations_keycloak_user_id
UNIQUE (keycloak_user_id);
