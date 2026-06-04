package ru.hits.car_school_automatization.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.Дано;
import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Когда;
import io.cucumber.java.ru.Тогда;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class P2PAnonymousSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions lastResponse;

    @Дано("существует задание {string} с включённым P2P-режимом")
    public void taskExistsWithP2pMode(String taskName) throws Exception {
        // Мы пока не создаем новые классы для P2P, но пишем тест через MockMvc.
        // Ожидаем, что в будущем будет API для создания/получения задания.
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + taskName + "\", \"p2pEnabled\": true}"));
    }

    @И("для этого задания установлен дедлайн проверки {string}")
    public void deadlineSet(String deadline) throws Exception {
        mockMvc.perform(patch("/api/tasks/1/deadline")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deadline\": \"" + deadline + "\"}"));
    }

    @И("в задании участвуют студенты {string}, {string}, {string}")
    public void studentsParticipate(String student1, String student2, String student3) throws Exception {
        mockMvc.perform(post("/api/tasks/1/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"" + student1 + "\", \"" + student2 + "\", \"" + student3 + "\"]"));
    }

    @Когда("преподаватель вручную назначает:")
    public void teacherAssignsReviewers(DataTable dataTable) throws Exception {
        List<Map<String, String>> assignments = dataTable.asMaps(String.class, String.class);
        String json = "[";
        for (int i = 0; i < assignments.size(); i++) {
            Map<String, String> row = assignments.get(i);
            json += "{\"reviewer\": \"" + row.get("Рецензент") + "\", \"target\": \"" + row.get("Целевой студент") + "\"}";
            if (i < assignments.size() - 1) json += ",";
        }
        json += "]";

        lastResponse = mockMvc.perform(post("/api/p2p/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Тогда("система сохраняет эти назначения")
    public void systemSavesAssignments() throws Exception {
        lastResponse.andExpect(status().isOk());
    }

    @И("каждый рецензент видит только работу своего целевого студента")
    public void reviewerSeesOnlyTarget() throws Exception {
        mockMvc.perform(get("/api/p2p/assignments/reviewer/Иванов"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetName").doesNotExist()) // Анонимность
                .andExpect(jsonPath("$[0].targetId").exists());
    }

    @И("имена рецензентов и целевых студентов скрыты \\(анонимность)")
    public void namesAreHidden() throws Exception {
        mockMvc.perform(get("/api/p2p/assignments/reviewer/Иванов"))
                .andExpect(jsonPath("$[0].reviewerName").doesNotExist())
                .andExpect(jsonPath("$[0].targetName").doesNotExist());
    }

    @Дано("студент {string} назначен рецензентом работы {string}")
    public void studentAssignedAsReviewer(String reviewer, String target) throws Exception {
        // Mock API call to create assignment
    }

    @И("дедлайн проверки ещё не наступил")
    public void deadlineNotPassed() {
        // mock time or just comment
    }

    @Когда("Иванов выставляет оценку {int} из {int} по критерию {string}")
    public void studentSubmitsGrade(int grade, int maxGrade, String criteria) throws Exception {
        lastResponse = mockMvc.perform(post("/api/p2p/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reviewer\": \"Иванов\", \"criteria\": \"" + criteria + "\", \"grade\": " + grade + "}"));
    }

    @Тогда("система сохраняет эту оценку")
    public void systemSavesGrade() throws Exception {
        lastResponse.andExpect(status().isOk());
    }

    @И("Петров \\(целевой студент) видит полученную оценку, но не видит, кто её поставил")
    public void targetSeesGradeAnonymously() throws Exception {
        mockMvc.perform(get("/api/p2p/grades/target/Петров"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grade").value(4))
                .andExpect(jsonPath("$[0].reviewerName").doesNotExist());
    }

    @Дано("студент {string} выставил оценку {string} за работу {string}")
    public void studentHasSetGrade(String reviewer, String grade, String target) {
        // mock setup
    }

    @И("дедлайн проверки уже наступил \\(текущее время > {string})")
    public void deadlineHasPassed(String time) {
        // mock time
    }

    @Когда("Иванов пытается изменить оценку на {string}")
    public void tryChangeGrade(String newGrade) throws Exception {
        lastResponse = mockMvc.perform(put("/api/p2p/grades/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grade\": " + newGrade + "}"));
    }

    @Тогда("система отклоняет изменение")
    public void systemRejectsChange() throws Exception {
        lastResponse.andExpect(status().isForbidden());
    }

    @И("показывает сообщение {string}")
    public void showsMessage(String msg) throws Exception {
        lastResponse.andExpect(jsonPath("$.message").value(msg));
    }
}
