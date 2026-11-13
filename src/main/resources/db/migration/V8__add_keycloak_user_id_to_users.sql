ALTER TABLE users
ADD COLUMN keycloak_user_id VARCHAR(255);

ALTER TABLE users
ADD CONSTRAINT uk_users_keycloak_user_id
UNIQUE (keycloak_user_id);
