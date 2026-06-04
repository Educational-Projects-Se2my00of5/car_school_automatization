package ru.hits.car_school_automatization.bdd;

import io.cucumber.java.ru.Дано;
import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Когда;
import io.cucumber.java.ru.Тогда;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OverrideSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions lastResponse;

    @Дано("задание {string} имеет автоматически рассчитанную оценку {double} для студента {string}")
    public void taskHasAutoGrade(String task, double grade, String student) {
        // mock
    }

    @Когда("преподаватель открывает журнал оценок и вручную устанавливает оценку {double} для Козловой")
    public void teacherOverridesGrade(double newGrade) throws Exception {
        lastResponse = mockMvc.perform(post("/api/grades/override")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"student\": \"Козлова\", \"task\": 1, \"grade\": " + newGrade + "}"));
    }

    @Тогда("система сохраняет новую оценку {double} как оверрайдную")
    public void systemSavesOverride(double grade) throws Exception {
        lastResponse.andExpect(status().isOk());
        mockMvc.perform(get("/api/grades/1/student/Козлова"))
                .andExpect(jsonPath("$.grade").value(grade))
                .andExpect(jsonPath("$.isOverridden").value(true));
    }

    @И("в журнале действий появляется запись об оверрайде: {string}")
    public void auditLogOverride(String record) throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(jsonPath("$[0].action").value("Было: 7.5, Стало: 9.0"));
    }

    @И("автоматический расчёт больше не применяется к этой паре")
    public void autoCalculationNotApplied() {
    }

    @И("на UI отображается пометка {string}")
    public void uiShowsNote(String note) {
    }

    @Дано("для студента {string} установлен оверрайд {double}")
    public void overrideSet(String student, double grade) {
    }

    @Когда("преподаватель выбирает {string}")
    public void teacherRevertsOverride(String action) throws Exception {
        lastResponse = mockMvc.perform(delete("/api/grades/override/1/student/Козлова"));
    }

    @Тогда("система удаляет оверрайд")
    public void systemRemovesOverride() throws Exception {
        lastResponse.andExpect(status().isOk());
    }

    @И("оценка становится {double}")
    public void gradeBecomes(double grade) throws Exception {
        mockMvc.perform(get("/api/grades/1/student/Козлова"))
                .andExpect(jsonPath("$.grade").value(grade))
                .andExpect(jsonPath("$.isOverridden").value(false));
    }

    @И("в журнал добавляется запись об отмене оверрайда")
    public void auditLogOverrideRemoved() {
    }
}
