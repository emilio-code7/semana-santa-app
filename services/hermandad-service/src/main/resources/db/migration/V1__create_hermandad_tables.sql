CREATE TABLE hermandad
(
    id                 UUID                     NOT NULL PRIMARY KEY,
    name               VARCHAR(255)             NOT NULL,
    city               VARCHAR(255)             NOT NULL,
    founded_year       INTEGER                  NOT NULL,
    keycloak_group_id  VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE hermandad_member
(
    id            UUID                     NOT NULL PRIMARY KEY,
    hermandad_id  UUID                     NOT NULL REFERENCES hermandad (id),
    user_id       VARCHAR(255)             NOT NULL,
    role          VARCHAR(50)              NOT NULL,
    joined_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_hermandad_member UNIQUE (hermandad_id, user_id)
);
