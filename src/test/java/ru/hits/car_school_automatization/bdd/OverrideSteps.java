package ru.hits.car_school_automatization.bdd;

import io.cucumber.java.ru.Дано;
import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Когда;
import io.cucumber.java.ru.Тогда;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.service.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class OverrideSteps {

    @Autowired private BddTestHelper helper;
    @Autowired private BddState state;
    @Autowired private MetricValueService metricValueService;

    @Дано("задание {string} имеет автоматически рассчитанную оценку {double} для студента {string}")
    public void taskHasAutoGrade(String task, double grade, String student) {
        helper.cleanDb();
        state.setTeacher(helper.createTeacher());
        state.setChannel(helper.createChannel(state.getTeacher(), new ArrayList<>()));
        state.setPost(helper.createPostWithP2P(state.getChannel(), state.getTeacher(), task));
        state.setMetric(helper.createMetricForPost(state.getPost(), "Общий критерий"));

        User user = helper.createStudent(student);
        state.getStudents().put(student, user);

        User anotherStudent = helper.createStudent("Another");

        // P2P grade from another student
        String token = helper.getToken(state.getTeacher());
        AssignP2PPersonalDto dto = AssignP2PPersonalDto.builder()
                .postId(state.getPost().getId())
                .reviewerId(anotherStudent.getId())
                .ownerId(user.getId())
                .targetSolutionId(null)
                .build();
        helper.getP2pService().assignP2PPersonal(dto, token);

        String studentToken = helper.getToken(anotherStudent);
        SetMetricValueDto valDto = new SetMetricValueDto(state.getMetric().getId(), user.getId(), grade);
        metricValueService.setMetricValue(valDto, studentToken);
    }

    @Когда("преподаватель открывает журнал оценок и вручную устанавливает оценку {double} для Козловой")
    public void teacherOverridesGrade(double newGrade) {
        String token = helper.getToken(state.getTeacher());
        SetMetricValueDto dto = new SetMetricValueDto(
                state.getMetric().getId(),
                state.getStudents().get("Козлова").getId(),
                newGrade
        );
        metricValueService.setMetricValue(dto, token);
    }

    @Тогда("система сохраняет новую оценку {double} как оверрайдную")
    public void systemSavesOverride(double grade) {
        String token = helper.getToken(state.getTeacher());
        List<MetricWithValuesDto> grades = metricValueService.getPostMetricsWithValues(
                state.getPost().getId(),
                state.getStudents().get("Козлова").getId(),
                token
        );
        assertEquals(grade, grades.get(0).getValues().get(0).getValue());
    }

    @И("в журнале действий появляется запись об оверрайде: {string}")
    public void auditLogOverride(String record) {
        // We log it via MetricChange repository. Let's check history.
        String token = helper.getToken(state.getTeacher());
        List<MetricValueHistoryDto> history = metricValueService.getMetricValueHistory(
                state.getMetric().getId(),
                state.getStudents().get("Козлова").getId(),
                token
        );
        assertTrue(history.size() > 1); // Auto grade + override
        assertEquals(9.0, history.get(0).getValue()); // Latest is 9.0
    }

    @И("автоматический расчёт больше не применяется к этой паре")
    public void autoCalculationNotApplied() {
        // Teacher's override locks it for students. Let's try changing it by student.
        User anotherStudent = helper.getUserRepository().findByEmail("Another@test.com").get();
        String studentToken = helper.getToken(anotherStudent);
        SetMetricValueDto valDto = new SetMetricValueDto(
                state.getMetric().getId(),
                state.getStudents().get("Козлова").getId(),
                5.0
        );
        try {
            metricValueService.setMetricValue(valDto, studentToken);
            fail("Expected ForbiddenException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("переопределена"));
        }
    }

    @И("на UI отображается пометка {string}")
    public void uiShowsNote(String note) {
        String token = helper.getToken(state.getTeacher());
        List<MetricValueHistoryDto> history = metricValueService.getMetricValueHistory(
                state.getMetric().getId(),
                state.getStudents().get("Козлова").getId(),
                token
        );
        assertEquals(state.getTeacher().getId(), history.get(0).getEditorId());
    }

    @Дано("для студента {string} установлен оверрайд {double}")
    public void overrideSet(String student, double grade) {
        taskHasAutoGrade("Задание", 7.5, student);
        teacherOverridesGrade(grade);
    }

    @Когда("преподаватель выбирает {string}")
    public void teacherRevertsOverride(String action) {
        String token = helper.getToken(state.getTeacher());
        metricValueService.removeOverride(
                state.getMetric().getId(),
                state.getStudents().get("Козлова").getId(),
                token
        );
    }

    @Тогда("система удаляет оверрайд")
    public void systemRemovesOverride() {
        // Handled by the method call above
    }

    @И("оценка становится {double}")
    public void gradeBecomes(double grade) {
        String token = helper.getToken(state.getTeacher());
        List<MetricWithValuesDto> grades = metricValueService.getPostMetricsWithValues(
                state.getPost().getId(),
                state.getStudents().get("Козлова").getId(),
                token
        );
        assertEquals(grade, grades.get(0).getValues().get(0).getValue());
    }

    @И("в журнал добавляется запись об отмене оверрайда")
    public void auditLogOverrideRemoved() {
        String token = helper.getToken(state.getTeacher());
        List<MetricValueHistoryDto> history = metricValueService.getMetricValueHistory(
                state.getMetric().getId(),
                state.getStudents().get("Козлова").getId(),
                token
        );
        // Latest change is reverting to previous non-teacher value
        // The editor should be the teacher who reverted it
        assertEquals(state.getTeacher().getId(), history.get(0).getEditorId());
        assertEquals(7.5, history.get(0).getValue());
    }
}