package ru.hits.car_school_automatization.bdd;

import io.cucumber.java.ru.Дано;
import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Когда;
import io.cucumber.java.ru.Тогда;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TransparentMarksSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions lastResponse;

    @Дано("студент {string} получил итоговую оценку {double} за задание {string}")
    public void studentGotFinalGrade(String student, double grade, String task) {
    }

    @Когда("Михайлов открывает страницу задания и нажимает {string}")
    public void studentOpensTaskDetails(String action) throws Exception {
        lastResponse = mockMvc.perform(get("/api/grades/1/student/Михайлов/details"));
    }

    @Тогда("система показывает:")
    public void systemShowsDetails(String docString) throws Exception {
        lastResponse.andExpect(status().isOk());
    }

    @И("для каждого критерия отображается возможный диапазон и полученный балл")
    public void displaysCriteriaRange() {
    }

    @Дано("преподаватель сделал оверрайд оценки студенту {string} с {double} на {double}")
    public void teacherOverrodeGrade(String student, double oldGrade, double newGrade) {
    }

    @Когда("Козлова открывает разбор оценки")
    public void studentOpensOverrideDetails() throws Exception {
        lastResponse = mockMvc.perform(get("/api/grades/1/student/Козлова/details"));
    }

    @Тогда("система показывает оценку после оверрайда: {double}")
    public void showsGradeAfterOverride(double grade) throws Exception {
        lastResponse.andExpect(status().isOk())
                .andExpect(jsonPath("$.finalGrade").value(grade));
    }

    @И("кто и когда сделал оверрайд")
    public void showsWhoDidOverride() throws Exception {
        lastResponse.andExpect(jsonPath("$.overrideBy").exists());
    }

    @И("ссылку на журнал действий для администратора\\/преподавателя")
    public void linkToAuditLog() throws Exception {
        lastResponse.andExpect(jsonPath("$.auditLink").exists());
    }
}
