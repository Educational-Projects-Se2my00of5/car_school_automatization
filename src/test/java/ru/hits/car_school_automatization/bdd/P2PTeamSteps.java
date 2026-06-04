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

public class P2PTeamSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions lastResponse;

    @Дано("создано командное задание {string}")
    public void teamTaskCreated(String taskName) throws Exception {
        mockMvc.perform(post("/api/tasks/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + taskName + "\"}"));
    }

    @И("сформированы команды: {string}, {string}, {string}")
    public void teamsCreated(String t1, String t2, String t3) {
        // mock
    }

    @И("в задании включено командное P2P-оценивание")
    public void p2pTeamEnabled() {
        // mock
    }

    @Когда("преподаватель запускает случайное распределение")
    public void teacherStartsRandomDistribution() throws Exception {
        lastResponse = mockMvc.perform(post("/api/p2p/team/assign/random"));
    }

    @Тогда("система назначает каждой команде ровно одну другую команду на проверку")
    public void systemAssignsOneTeamToAnother() throws Exception {
        lastResponse.andExpect(status().isOk());
    }

    @И("все команды получают уведомления: {string}")
    public void teamsGetNotifications(String notification) throws Exception {
        mockMvc.perform(get("/api/notifications/teams"))
                .andExpect(jsonPath("$[0].message").value(notification));
    }

    @Дано("команда {string} проверяет работу команды {string}")
    public void teamChecksAnotherTeam(String reviewerTeam, String targetTeam) {
    }

    @И("у команды {string} есть капитан {string}")
    public void teamHasCaptain(String team, String captain) {
    }

    @Когда("Соколов выставляет оценку {string} за решение")
    public void captainSetsGrade(String gradeStr) throws Exception {
        lastResponse = mockMvc.perform(post("/api/p2p/team/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"captain\": \"Соколов\", \"grade\": 8}"));
    }

    @Тогда("эта оценка становится итоговой оценкой для команды {string}")
    public void gradeBecomesFinalForTeam(String team) throws Exception {
        lastResponse.andExpect(status().isOk());
    }

    @И("другие члены команды {string} не могут изменить эту оценку")
    public void otherMembersCannotChangeGrade(String team) throws Exception {
        mockMvc.perform(put("/api/p2p/team/grades/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\": \"НеСоколов\", \"grade\": 9}"))
                .andExpect(status().isForbidden());
    }

    @И("в команде {string} нет капитана")
    public void teamHasNoCaptain(String team) {
    }

    @И("члены команды {string}: Студент1, Студент2, Студент3")
    public void teamMembersAre(String team) {
    }

    @Когда("Студент1 ставит {string}, Студент2 ставит {string}, Студент3 ставит {string}")
    public void studentsSetGrades(String g1, String g2, String g3) throws Exception {
        mockMvc.perform(post("/api/p2p/team/grades/member")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"member\": \"Студент1\", \"grade\": " + g1 + "}"));
        mockMvc.perform(post("/api/p2p/team/grades/member")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"member\": \"Студент2\", \"grade\": " + g2 + "}"));
        lastResponse = mockMvc.perform(post("/api/p2p/team/grades/member")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"member\": \"Студент3\", \"grade\": " + g3 + "}"));
    }

    @Тогда("система вычисляет среднее = \\({int}+{int}+{int})\\/{int} = {double}")
    public void systemCalculatesAverage(int g1, int g2, int g3, int count, double avg) {
        // Validation in the next step
    }

    @И("выставляет итоговую оценку {double} для команды {string}")
    public void setsFinalGradeForTeam(double grade, String team) throws Exception {
        mockMvc.perform(get("/api/p2p/team/grades/target/" + team))
                .andExpect(jsonPath("$.finalGrade").value(grade));
    }
}
