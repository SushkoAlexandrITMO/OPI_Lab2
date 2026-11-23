package ru.itmo.lab1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозная проверка: REST -> сервисы -> EclipseLink -> PostgreSQL (схема и функции из миграций Flyway).
 */
@SpringBootTest
@ActiveProfiles("test") // без профиля server: DataSource строится из url встроенной БД, а не из JNDI
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudyGroupApiIntegrationTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @AfterAll
    static void stop() throws IOException {
        POSTGRES.close();
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static long groupId;
    private static long adminId;
    private static long coordinatesId;

    private static final String NEW_GROUP = """
            {
              "name": "P3110",
              "coordinates": {"x": 1.5, "y": 2.25},
              "studentsCount": 25,
              "expelledStudents": 3,
              "transferredStudents": 2,
              "formOfEducation": "FULL_TIME_EDUCATION",
              "shouldBeExpelled": 1,
              "averageMark": 4,
              "semesterEnum": "THIRD",
              "groupAdmin": {
                "name": "Иван",
                "hairColor": "BROWN",
                "weight": 70.5,
                "location": {"x": 1, "y": 2, "name": "Кронверкский"}
              }
            }
            """;

    @Test
    @Order(1)
    void createsGroupWithNewRelatedObjects() throws Exception {
        JsonNode body = json(mvc.perform(post("/api/study-groups").contentType(MediaType.APPLICATION_JSON).content(NEW_GROUP))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creationDate").exists())
                .andExpect(jsonPath("$.groupAdmin.location.name").value("Кронверкский")));
        groupId = body.get("id").asLong();
        adminId = body.get("groupAdmin").get("id").asLong();
        coordinatesId = body.get("coordinates").get("id").asLong();
        assertThat(groupId).isPositive();
    }

    @Test
    @Order(2)
    void createsGroupLinkedToExistingObjects() throws Exception {
        String request = """
                {"name": "P3111", "coordinatesId": %d, "studentsCount": 10, "expelledStudents": 1,
                 "transferredStudents": 1, "formOfEducation": "EVENING_CLASSES", "shouldBeExpelled": 1,
                 "averageMark": 5, "semesterEnum": "FOURTH", "groupAdminId": %d}
                """.formatted(coordinatesId, adminId);
        mvc.perform(post("/api/study-groups").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coordinates.id").value(coordinatesId))
                .andExpect(jsonPath("$.groupAdmin.id").value(adminId));

        // Группа без администратора — должна оставаться в таблице при сортировке по полям администратора
        String withoutAdmin = request.replace("\"groupAdminId\": " + adminId, "\"groupAdminId\": null")
                .replace("P3111", "M3200");
        mvc.perform(post("/api/study-groups").contentType(MediaType.APPLICATION_JSON).content(withoutAdmin))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(3)
    void rejectsInvalidInputWithFieldErrors() throws Exception {
        String invalid = NEW_GROUP.replace("\"P3110\"", "\"   \"").replace("\"studentsCount\": 25", "\"studentsCount\": 0");
        mvc.perform(post("/api/study-groups").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.studentsCount").exists());

        String badEnum = NEW_GROUP.replace("THIRD", "FIRST");
        mvc.perform(post("/api/study-groups").contentType(MediaType.APPLICATION_JSON).content(badEnum))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.semesterEnum").exists());

        String fractional = NEW_GROUP.replace("\"studentsCount\": 25", "\"studentsCount\": 2.5");
        mvc.perform(post("/api/study-groups").contentType(MediaType.APPLICATION_JSON).content(fractional))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void pagesFiltersAndSorts() throws Exception {
        mvc.perform(get("/api/study-groups").param("filterField", "name").param("filterValue", "p31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(get("/api/study-groups").param("filterField", "formOfEducation").param("filterValue", "even"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(get("/api/study-groups").param("filterField", "name").param("filterValue", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/study-groups").param("sort", "groupAdmin.name").param("direction", "desc")
                        .param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));

        mvc.perform(get("/api/study-groups").param("sort", "hack; drop table"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void rejectsStaleVersion() throws Exception {
        JsonNode current = json(mvc.perform(get("/api/study-groups/" + groupId)).andExpect(status().isOk()));
        long version = current.get("version").asLong();
        String update = """
                {"name": "P3110-upd", "coordinatesId": %d, "studentsCount": 26, "expelledStudents": 3,
                 "transferredStudents": 2, "formOfEducation": "FULL_TIME_EDUCATION", "shouldBeExpelled": 1,
                 "averageMark": 4, "semesterEnum": "THIRD", "groupAdminId": %d, "version": %d}
                """;
        mvc.perform(put("/api/study-groups/" + groupId).contentType(MediaType.APPLICATION_JSON)
                        .content(update.formatted(coordinatesId, adminId, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("P3110-upd"));

        mvc.perform(put("/api/study-groups/" + groupId).contentType(MediaType.APPLICATION_JSON)
                        .content(update.formatted(coordinatesId, adminId, version)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(6)
    void specialOperationsUseDatabaseFunctions() throws Exception {
        mvc.perform(get("/api/special/min-expelled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expelledStudents").value(1));

        mvc.perform(get("/api/special/count-by-id"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].count").value(1));

        mvc.perform(post("/api/special/groups/" + groupId + "/add-student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentsCount").value(27));

        // было 3 отчисленных + 27 студентов → 30 отчисленных, студентов и переведённых 0
        mvc.perform(post("/api/special/groups/" + groupId + "/expel-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expelledStudents").value(30))
                .andExpect(jsonPath("$.studentsCount").value(0))
                .andExpect(jsonPath("$.transferredStudents").value(0));

        // после отчисления в группу снова можно добавить студента
        mvc.perform(post("/api/special/groups/" + groupId + "/add-student"))
                .andExpect(jsonPath("$.studentsCount").value(1));

        mvc.perform(post("/api/special/groups/999999/add-student"))
                .andExpect(status().isNotFound());

        String lighterAdmin = """
                {"name": "Анна", "hairColor": "WHITE", "weight": 50}
                """;
        long anna = json(mvc.perform(post("/api/persons").contentType(MediaType.APPLICATION_JSON).content(lighterAdmin))
                .andExpect(status().isCreated())).get("id").asLong();
        // "Иван" > "Анна" → обе группы с администратором Иваном
        mvc.perform(get("/api/special/admin-greater-than").param("personId", String.valueOf(anna)))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/special/admin-greater-than").param("personId", String.valueOf(adminId)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(7)
    void deletingReferencedObjectsRequiresReassignment() throws Exception {
        mvc.perform(delete("/api/persons/" + adminId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REFERENCED"))
                .andExpect(jsonPath("$.details.references").value(2))
                .andExpect(jsonPath("$.details.unlinkAllowed").value(true));

        mvc.perform(delete("/api/persons/" + adminId).param("unlink", "true"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/study-groups/" + groupId))
                .andExpect(jsonPath("$.groupAdmin").doesNotExist());

        long otherCoordinates = json(mvc.perform(post("/api/coordinates").contentType(MediaType.APPLICATION_JSON)
                .content("{\"x\": 9, \"y\": 9}")).andExpect(status().isCreated())).get("id").asLong();

        mvc.perform(delete("/api/coordinates/" + coordinatesId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.unlinkAllowed").value(false));

        mvc.perform(delete("/api/coordinates/" + coordinatesId).param("replacementId", String.valueOf(otherCoordinates)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/study-groups/" + groupId))
                .andExpect(jsonPath("$.coordinates.id").value(otherCoordinates));
    }

    @Test
    @Order(8)
    void deletesGroup() throws Exception {
        mvc.perform(delete("/api/study-groups/" + groupId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/study-groups/" + groupId)).andExpect(status().isNotFound());
    }

    private JsonNode json(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
