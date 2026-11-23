-- После операции "Отчислить всех студентов" studentsCount и transferredStudents становятся 0,
-- поэтому для этих полей ограничение ослаблено с > 0 до >= 0.
ALTER TABLE study_group DROP CONSTRAINT study_group_students_count_positive;
ALTER TABLE study_group
    ADD CONSTRAINT study_group_students_count_non_negative CHECK (students_count >= 0);

ALTER TABLE study_group DROP CONSTRAINT study_group_transferred_students_positive;
ALTER TABLE study_group
    ADD CONSTRAINT study_group_transferred_students_non_negative CHECK (transferred_students >= 0);
