package ru.itmo.lab1;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.itmo.lab1.config.FlywayConfig;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверяет стратегию из FlywayConfig: правка и переименование уже применённых миграций
 * не должны ломать запуск приложения.
 */
class FlywayRepairTest {

    @TempDir
    Path dir;

    @Test
    void editedAndRenamedMigrationsDoNotFailStartup() throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.start()) {
            Files.writeString(dir.resolve("V1__schema.sql"), "CREATE TABLE t (id INT);");
            Files.writeString(dir.resolve("V2__functions.sql"), "CREATE FUNCTION f() RETURNS INT LANGUAGE sql AS 'SELECT 1';");
            flyway(pg).migrate();

            // Правим применённую V1, превращаем V2 в повторяемую R__ и меняем функцию
            Files.writeString(dir.resolve("V1__schema.sql"), "-- комментарий\nCREATE TABLE t (id INT);");
            Files.delete(dir.resolve("V2__functions.sql"));
            Files.writeString(dir.resolve("R__functions.sql"),
                    "CREATE OR REPLACE FUNCTION f() RETURNS INT LANGUAGE sql AS 'SELECT 2';");

            // Без repair Flyway отказывается запускаться (checksum mismatch / missing migration)
            assertThatThrownBy(() -> flyway(pg).migrate()).isInstanceOf(Exception.class);

            // Стратегия приложения проходит без ошибок и применяет R__
            assertThatCode(() -> new FlywayConfig().repairThenMigrate().migrate(flyway(pg))).doesNotThrowAnyException();
            try (var c = pg.getPostgresDatabase().getConnection();
                 var rs = c.createStatement().executeQuery("SELECT f()")) {
                rs.next();
                org.assertj.core.api.Assertions.assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }
    }

    private Flyway flyway(EmbeddedPostgres pg) {
        return Flyway.configure()
                .dataSource(pg.getPostgresDatabase())
                .locations("filesystem:" + dir.toAbsolutePath())
                .load();
    }
}
