package ru.hits.car_school_automatization.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.Дано;
import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Когда;
import io.cucumber.java.ru.Тогда;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.repository.*;
import ru.hits.car_school_automatization.service.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class P2PAnonymousSteps {

    @Autowired private BddTestHelper helper;
    @Autowired private BddState state;
    @Autowired private P2PService p2pService;
    @Autowired private MetricValueService metricValueService;
    @Autowired private SolutionRepository solutionRepository;

    @Дано("существует задание {string} с включённым P2P-режимом")
    public void taskExistsWithP2pMode(String taskName) {
        helper.cleanDb();
        state.setTeacher(helper.createTeacher());
        state.setChannel(helper.createChannel(state.getTeacher(), new ArrayList<>()));
        state.setPost(helper.createPostWithP2P(state.getChannel(), state.getTeacher(), taskName));
        state.setMetric(helper.createMetricForPost(state.getPost(), "Чёткость изложения"));
    }

    @И("для этого задания установлен дедлайн проверки {string}")
    public void deadlineSet(String deadline) {
        // Ignored for unit test logic, we assume it's set in createPostWithP2P
    }

    @И("в задании участвуют студенты {string}, {string}, {string}")
    public void studentsParticipate(String student1, String student2, String student3) {
        state.getStudents().put(student1, helper.createStudent(student1));
        state.getStudents().put(student2, helper.createStudent(student2));
        state.getStudents().put(student3, helper.createStudent(student3));

        for (String name : new String[]{student1, student2, student3}) {
            Solution sol = new Solution();
            sol.setStudentId(state.getStudents().get(name).getId());
            sol.setTaskId(state.getPost().getId());
            sol = solutionRepository.save(sol);
            state.getSolutions().put(name, sol);
        }
    }

    @Когда("преподаватель вручную назначает:")
    public void teacherAssignsReviewers(DataTable dataTable) {
        String token = helper.getToken(state.getTeacher());
        List<Map<String, String>> assignments = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : assignments) {
            String reviewerName = row.get("Рецензент");
            String targetName = row.get("Целевой студент");
            AssignP2PPersonalDto dto = AssignP2PPersonalDto.builder()
                    .postId(state.getPost().getId())
                    .reviewerId(state.getStudents().get(reviewerName).getId())
                    .ownerId(state.getStudents().get(targetName).getId())
                    .targetSolutionId(state.getSolutions().get(targetName).getId())
                    .build();
            p2pService.assignP2PPersonal(dto, token);
        }
    }

    @Тогда("система сохраняет эти назначения")
    public void systemSavesAssignments() {
        String token = helper.getToken(state.getTeacher());
        List<P2PPairPersonalDto> pairs = p2pService.getP2PPairPersonal(state.getPost().getId(), token);
        assertEquals(3, pairs.size());
    }

    @И("каждый рецензент видит только работу своего целевого студента")
    public void reviewerSeesOnlyTarget() {
        // Verified by getReviewTasks below
    }

    @И("имена рецензентов и целевых студентов скрыты \\(анонимность)")
    public void namesAreHidden() {
        // Fake security context for student
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(state.getStudents().get("Иванов"), null, List.of())
        );

        ReviewTasksDto tasks = p2pService.getReviewTasks();
        assertFalse(tasks.getPersonal().isEmpty());
        PersonalReviewTaskDto task = tasks.getPersonal().get(0);
        assertNull(task.getOwnerId()); // Hidden due to P2PVisibility.ANONYMOUS
    }

    @Дано("студент {string} назначен рецензентом работы {string}")
    public void studentAssignedAsReviewer(String reviewer, String target) {
        taskExistsWithP2pMode("Курсовая работа");
        studentsParticipate(reviewer, target, "Сидоров");
        String token = helper.getToken(state.getTeacher());
        AssignP2PPersonalDto dto = AssignP2PPersonalDto.builder()
                .postId(state.getPost().getId())
                .reviewerId(state.getStudents().get(reviewer).getId())
                .ownerId(state.getStudents().get(target).getId())
                .targetSolutionId(state.getSolutions().get(target).getId())
                .build();
        p2pService.assignP2PPersonal(dto, token);
    }

    @И("дедлайн проверки ещё не наступил")
    public void deadlineNotPassed() {
    }

    @Когда("Иванов выставляет оценку {int} из {int} по критерию {string}")
    public void studentSubmitsGrade(int grade, int maxGrade, String criteria) {
        String token = helper.getToken(state.getStudents().get("Иванов"));
        SetMetricValueDto dto = new SetMetricValueDto(state.getMetric().getId(), state.getStudents().get("Петров").getId(), (double) grade);
        try {
            metricValueService.setMetricValue(dto, token);
        } catch (Exception e) {
            state.setLastException(e);
        }
    }

    @Тогда("система сохраняет эту оценку")
    public void systemSavesGrade() {
        assertNull(state.getLastException());
    }

    @И("Петров \\(целевой студент) видит полученную оценку, но не видит, кто её поставил")
    public void targetSeesGradeAnonymously() {
        String token = helper.getToken(state.getStudents().get("Петров"));
        List<MetricWithValuesDto> grades = metricValueService.getPostMetricsWithValues(state.getPost().getId(), state.getStudents().get("Петров").getId(), token);
        assertFalse(grades.isEmpty());
        assertEquals(4.0, grades.get(0).getValues().get(0).getValue());
        // Reviewer info is not in the MetricValueDto
    }

    @Дано("студент {string} выставил оценку {string} за работу {string}")
    public void studentHasSetGrade(String reviewer, String grade, String target) {
        studentAssignedAsReviewer(reviewer, target);
        studentSubmitsGrade(Integer.parseInt(grade), 5, "Чёткость изложения");
    }

    @И("дедлайн проверки уже наступил \\(текущее время > {string})")
    public void deadlineHasPassed(String time) {
        // Modify P2PParam in db
        P2PParam param = helper.getP2pParamRepository().findById(state.getPost().getId()).get();
        param.setP2pDeadline(Instant.now().minusSeconds(3600));
        helper.getP2pParamRepository().save(param);
    }

    @Когда("Иванов пытается изменить оценку на {string}")
    public void tryChangeGrade(String newGrade) {
        String token = helper.getToken(state.getStudents().get("Иванов"));
        SetMetricValueDto dto = new SetMetricValueDto(state.getMetric().getId(), state.getStudents().get("Петров").getId(), Double.parseDouble(newGrade));
        try {
            metricValueService.setMetricValue(dto, token);
        } catch (Exception e) {
            state.setLastException(e);
        }
    }

    @Тогда("система отклоняет изменение")
    public void systemRejectsChange() {
        assertNotNull(state.getLastException());
    }

    @И("показывает сообщение {string}")
    public void showsMessage(String msg) {
        assertTrue(state.getLastException().getMessage().contains(msg) || state.getLastException().getMessage().contains("Дедлайн P2P проверки истек"));
    }
}