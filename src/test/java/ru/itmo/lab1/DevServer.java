package ru.itmo.lab1;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;

/**
 * Локальный запуск без Docker и без установленного PostgreSQL: ./gradlew bootTestRun
 * Поднимает встроенный PostgreSQL на порту 54321 (данные не сохраняются между запусками).
 */
public class DevServer {
    public static void main(String[] args) throws Exception {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder().setPort(54321).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (Exception ignored) {
            }
        }));

        // Системные свойства приоритетнее application.yml
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "");
        // Профиль dev вместо server: DataSource по url встроенной БД, а не из JNDI
        System.setProperty("spring.profiles.active", "dev");
        SpringApplication.run(StudyGroupsApplication.class, args);
    }
}
