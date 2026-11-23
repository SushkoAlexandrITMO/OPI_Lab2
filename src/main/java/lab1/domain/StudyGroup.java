package ru.itmo.lab1.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.ZonedDateTime;

@Entity
@Table(name = "study_group")
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Название группы не может быть пустым")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Координаты обязательны")
    @ManyToOne(optional = false)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @Convert(converter = ZonedDateTimeConverter.class)
    @Column(name = "creation_date", nullable = false, updatable = false)
    private ZonedDateTime creationDate;

    // >= 0: после "Отчислить всех студентов" становится 0
    @PositiveOrZero(message = "Количество студентов не может быть отрицательным")
    @Column(name = "students_count", nullable = false)
    private int studentsCount;

    @Positive(message = "Количество отчисленных должно быть больше 0")
    @Column(name = "expelled_students", nullable = false)
    private int expelledStudents;

    @NotNull(message = "Количество переведённых обязательно")
    // >= 0: после "Отчислить всех студентов" становится 0
    @PositiveOrZero(message = "Количество переведённых не может быть отрицательным")
    @Column(name = "transferred_students", nullable = false)
    private Long transferredStudents;

    @NotNull(message = "Форма обучения обязательна")
    @Enumerated(EnumType.STRING)
    @Column(name = "form_of_education", nullable = false)
    private FormOfEducation formOfEducation;

    @NotNull(message = "Поле shouldBeExpelled обязательно")
    @Positive(message = "Поле shouldBeExpelled должно быть больше 0")
    @Column(name = "should_be_expelled", nullable = false)
    private Long shouldBeExpelled;

    @NotNull(message = "Средняя оценка обязательна")
    @Positive(message = "Средняя оценка должна быть больше 0")
    @Column(name = "average_mark", nullable = false)
    private Long averageMark;

    @NotNull(message = "Семестр обязателен")
    @Enumerated(EnumType.STRING)
    @Column(name = "semester_enum", nullable = false)
    private Semester semesterEnum;

    @ManyToOne
    @JoinColumn(name = "group_admin_id")
    private Person groupAdmin;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        creationDate = ZonedDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public int getStudentsCount() {
        return studentsCount;
    }

    public void setStudentsCount(int studentsCount) {
        this.studentsCount = studentsCount;
    }

    public int getExpelledStudents() {
        return expelledStudents;
    }

    public void setExpelledStudents(int expelledStudents) {
        this.expelledStudents = expelledStudents;
    }

    public Long getTransferredStudents() {
        return transferredStudents;
    }

    public void setTransferredStudents(Long transferredStudents) {
        this.transferredStudents = transferredStudents;
    }

    public FormOfEducation getFormOfEducation() {
        return formOfEducation;
    }

    public void setFormOfEducation(FormOfEducation formOfEducation) {
        this.formOfEducation = formOfEducation;
    }

    public Long getShouldBeExpelled() {
        return shouldBeExpelled;
    }

    public void setShouldBeExpelled(Long shouldBeExpelled) {
        this.shouldBeExpelled = shouldBeExpelled;
    }

    public Long getAverageMark() {
        return averageMark;
    }

    public void setAverageMark(Long averageMark) {
        this.averageMark = averageMark;
    }

    public Semester getSemesterEnum() {
        return semesterEnum;
    }

    public void setSemesterEnum(Semester semesterEnum) {
        this.semesterEnum = semesterEnum;
    }

    public Person getGroupAdmin() {
        return groupAdmin;
    }

    public void setGroupAdmin(Person groupAdmin) {
        this.groupAdmin = groupAdmin;
    }

    public Long getVersion() {
        return version;
    }
}
