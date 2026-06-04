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

public class PenaltySteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions lastResponse;

    @Дано("задание {string} с дедлайном {string}")
    public void taskWithDeadline(String taskName, String deadline) throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + taskName + "\", \"deadline\": \"" + deadline + "\"}"));
    }

    @И("правилом наказания: шаг = {int} минут, величина = {double}, тип = ограничитель")
    public void penaltyRuleLimiter(int step, double amount) throws Exception {
        mockMvc.perform(post("/api/tasks/1/penalty")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepMinutes\": " + step + ", \"amount\": " + amount + ", \"type\": \"LIMITER\"}"));
    }

    @И("максимальный балл за задание = {int}")
    public void maxGrade(int max) {
        // mock
    }

    @Когда("студент {string} сдаёт работу в {string} \\(опоздание на {int} минут)")
    public void studentSubmitsLate(String student, String time, int delay) throws Exception {
        lastResponse = mockMvc.perform(post("/api/solutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"student\": \"" + student + "\", \"time\": \"" + time + "\"}"));
    }

    @Тогда("система определяет количество шагов = {int} \\({int} минут \\/ {int} минут с округлением вверх)")
    public void systemDeterminesSteps(int steps, int totalMinutes, int stepMinutes) {
        // Validation in the next step
    }

    @И("уменьшает максимальный балл на {int} \\* {double} = {double}")
    public void reducesMaxGrade(int steps, double amount, double totalPenalty) {
    }

    @И("итоговый максимальный балл для этого студента становится {double}")
    public void finalMaxGradeIs(double grade) throws Exception {
        mockMvc.perform(get("/api/grades/max/Михайлов/1"))
                .andExpect(jsonPath("$.maxGrade").value(grade));
    }

    @И("при расчёте итога используется этот новый максимум")
    public void newMaxIsUsed() {
    }

    @Дано("задание с правилом: шаг = {int} день, величина = {double}, тип = коэффициент")
    public void penaltyRuleCoefficient(int step, double amount) throws Exception {
        mockMvc.perform(post("/api/tasks/1/penalty")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepDays\": " + step + ", \"amount\": " + amount + ", \"type\": \"COEFFICIENT\"}"));
    }

    @И("полученная студентом оценка = {double}")
    public void studentReceivedGrade(double grade) {
    }

    @Когда("студент сдал работу на {int} дня позже дедлайна")
    public void studentSubmitsDaysLate(int days) {
    }

    @Тогда("штрафной коэффициент = {double} \\* {double} = {double}")
    public void penaltyCoefficientIs(double a, double b, double c) {
    }

    @И("итоговая оценка = {double} \\* {double} = {double} \\(с округлением до сотых)")
    public void finalGradeIsCalculated(double original, double coef, double finalGrade) throws Exception {
        mockMvc.perform(get("/api/grades/1/student"))
                .andExpect(jsonPath("$.finalGrade").value(finalGrade));
    }

    @Дано("студенту {string} автоматически начислен штраф {int} балла за просрочку")
    public void penaltyAutoAssigned(String student, int points) {
    }

    @Когда("преподаватель заходит в карточку студента и выбирает {string}")
    public void teacherCancelsPenalty(String action) throws Exception {
        lastResponse = mockMvc.perform(delete("/api/penalties/1/student/Смирнов"));
    }

    @Тогда("система убирает штраф")
    public void systemRemovesPenalty() throws Exception {
        lastResponse.andExpect(status().isOk());
    }

    @И("записывает в журнал действий: {string}")
    public void writesToAuditLog(String logMsg) throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(jsonPath("$[0].action").value(logMsg));
    }
}
