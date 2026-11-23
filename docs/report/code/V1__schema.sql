CREATE TABLE coordinates
(
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    x       REAL             NOT NULL,
    y       DOUBLE PRECISION NOT NULL,
    version BIGINT           NOT NULL DEFAULT 1
);

CREATE TABLE location
(
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    x       REAL             NOT NULL,
    y       DOUBLE PRECISION NOT NULL,
    name    TEXT             NOT NULL,
    version BIGINT           NOT NULL DEFAULT 1
);

CREATE TABLE person
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT        NOT NULL,
    eye_color   VARCHAR(16),
    hair_color  VARCHAR(16) NOT NULL,
    location_id BIGINT REFERENCES location (id),
    weight      REAL        NOT NULL,
    passport_id TEXT,
    nationality VARCHAR(16),
    version     BIGINT      NOT NULL DEFAULT 1,

    CONSTRAINT person_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT person_weight_positive CHECK (weight > 0),
    CONSTRAINT person_eye_color_enum CHECK (eye_color IN ('GREEN', 'WHITE', 'BROWN')),
    CONSTRAINT person_hair_color_enum CHECK (hair_color IN ('GREEN', 'WHITE', 'BROWN')),
    CONSTRAINT person_nationality_enum CHECK (nationality IN ('USA', 'FRANCE', 'SPAIN', 'VATICAN', 'THAILAND'))
);

CREATE TABLE study_group
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                 TEXT        NOT NULL,
    coordinates_id       BIGINT      NOT NULL REFERENCES coordinates (id),
    creation_date        TIMESTAMPTZ NOT NULL DEFAULT now(),
    students_count       INTEGER     NOT NULL,
    expelled_students    INTEGER     NOT NULL,
    transferred_students BIGINT      NOT NULL,
    form_of_education    VARCHAR(32) NOT NULL,
    should_be_expelled   BIGINT      NOT NULL,
    average_mark         BIGINT      NOT NULL,
    semester_enum        VARCHAR(16) NOT NULL,
    group_admin_id       BIGINT REFERENCES person (id),
    version              BIGINT      NOT NULL DEFAULT 1,

    CONSTRAINT study_group_id_positive CHECK (id > 0),
    CONSTRAINT study_group_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT study_group_students_count_positive CHECK (students_count > 0),
    CONSTRAINT study_group_expelled_students_positive CHECK (expelled_students > 0),
    CONSTRAINT study_group_transferred_students_positive CHECK (transferred_students > 0),
    CONSTRAINT study_group_should_be_expelled_positive CHECK (should_be_expelled > 0),
    CONSTRAINT study_group_average_mark_positive CHECK (average_mark > 0),
    CONSTRAINT study_group_form_of_education_enum
        CHECK (form_of_education IN ('DISTANCE_EDUCATION', 'FULL_TIME_EDUCATION', 'EVENING_CLASSES')),
    CONSTRAINT study_group_semester_enum
        CHECK (semester_enum IN ('THIRD', 'FOURTH', 'SIXTH', 'SEVENTH', 'EIGHTH'))
);

CREATE INDEX study_group_coordinates_idx ON study_group (coordinates_id);
CREATE INDEX study_group_group_admin_idx ON study_group (group_admin_id);
CREATE INDEX person_location_idx ON person (location_id);
