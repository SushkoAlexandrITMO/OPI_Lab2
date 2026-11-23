package ru.itmo.lab1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * main — локальный запуск со встроенным Tomcat.
 * SpringBootServletInitializer — запуск при развёртывании war на сервере приложений
 */
@SpringBootApplication
public class StudyGroupsApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(StudyGroupsApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(StudyGroupsApplication.class);
    }
}
