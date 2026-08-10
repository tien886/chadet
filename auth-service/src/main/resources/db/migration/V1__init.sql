CREATE TABLE users (
    id       UUID PRIMARY KEY,
    gmail    VARCHAR(320) NOT NULL UNIQUE,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);