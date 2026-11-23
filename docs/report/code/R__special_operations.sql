-- 1. Один (любой) объект с минимальным expelledStudents
CREATE OR REPLACE FUNCTION sg_min_expelled()
    RETURNS SETOF study_group
    LANGUAGE sql
    STABLE
AS
$$
SELECT *
FROM study_group
ORDER BY expelled_students, id
LIMIT 1;
$$;

-- 2. Группировка по id с количеством элементов в каждой группе
--    (id уникален, поэтому в каждой группе ровно один элемент)
CREATE OR REPLACE FUNCTION sg_count_by_id()
    RETURNS TABLE
            (
                group_id BIGINT,
                cnt      BIGINT
            )
    LANGUAGE sql
    STABLE
AS
$$
SELECT id, COUNT(*)
FROM study_group
GROUP BY id
ORDER BY id;
$$;

-- 3. Группы, у которых groupAdmin больше заданного.
--    Порядок на Person: сначала name, затем weight (лексикографическое сравнение кортежей).
--    Группы без администратора в выборку не попадают.
CREATE OR REPLACE FUNCTION sg_admin_greater_than(p_person_id BIGINT)
    RETURNS SETOF study_group
    LANGUAGE plpgsql
    STABLE
AS
$$
DECLARE
    ref person%ROWTYPE;
BEGIN
    SELECT * INTO ref FROM person WHERE id = p_person_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Человек с id % не найден', p_person_id USING ERRCODE = 'no_data_found';
    END IF;

    RETURN QUERY
        SELECT g.*
        FROM study_group g
                 JOIN person p ON p.id = g.group_admin_id
        WHERE (p.name, p.weight) > (ref.name, ref.weight)
        ORDER BY g.id;
END;
$$;

-- 4. Отчислить всех студентов группы.
--    Все текущие студенты добавляются к отчисленным, studentsCount и transferredStudents обнуляются
--    (ограничения для этих полей ослаблены до >= 0 в V3). В SET используются значения строки до обновления.
--    Возвращает новое значение expelledStudents.
CREATE OR REPLACE FUNCTION sg_expel_all(p_group_id BIGINT)
    RETURNS INTEGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    result INTEGER;
BEGIN
    UPDATE study_group
    SET expelled_students    = expelled_students + students_count,
        students_count       = 0,
        transferred_students = 0,
        version              = version + 1
    WHERE id = p_group_id
    RETURNING expelled_students INTO result;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Группа с id % не найдена', p_group_id USING ERRCODE = 'no_data_found';
    END IF;
    RETURN result;
END;
$$;

-- 5. Добавить студента в группу. Атомарный инкремент, без гонки "прочитать -> +1 -> записать".
CREATE OR REPLACE FUNCTION sg_add_student(p_group_id BIGINT)
    RETURNS INTEGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    result INTEGER;
BEGIN
    UPDATE study_group
    SET students_count = students_count + 1,
        version        = version + 1
    WHERE id = p_group_id
    RETURNING students_count INTO result;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Группа с id % не найдена', p_group_id USING ERRCODE = 'no_data_found';
    END IF;
    RETURN result;
END;
$$;
