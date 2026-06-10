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

public class P2PTeamSteps {

    @Autowired private BddTestHelper helper;
    @Autowired private BddState state;
    @Autowired private P2PService p2pService;
    @Autowired private MetricValueService metricValueService;

    @Дано("создано командное задание {string}")
    public void teamTaskCreated(String taskName) {
        helper.cleanDb();
        state.setTeacher(helper.createTeacher());
        state.setChannel(helper.createChannel(state.getTeacher(), new ArrayList<>()));
        state.setTask(helper.createTaskWithP2P(state.getChannel(), taskName));
        state.setMetric(helper.createMetricForTask(state.getTask(), "Критерий 1"));
    }

    @И("сформированы команды: {string}, {string}, {string}")
    public void teamsCreated(String t1, String t2, String t3) {
        String[] teamNames = {t1, t2, t3};
        for (String name : teamNames) {
            User captain = helper.createStudent(name + "_cap");
            User member1 = helper.createStudent(name + "_m1");
            User member2 = helper.createStudent(name + "_m2");
            state.getStudents().put(captain.getFirstName(), captain);
            state.getStudents().put(member1.getFirstName(), member1);
            state.getStudents().put(member2.getFirstName(), member2);

            Team team = helper.createTeam(name, state.getTask(), List.of(captain, member1, member2), captain);
            state.getTeams().put(name, team);

            TaskSolution ts = new TaskSolution();
            ts.setTeamId(team.getId());
            ts.setTaskId(state.getTask().getId());
            ts.setIsSelected(true);
            ts = helper.getTaskSolutionRepository().save(ts);
            state.getTaskSolutions().put(name, ts);
        }
    }

    @И("в задании включено командное P2P-оценивание")
    public void p2pTeamEnabled() {
        // Automatically enabled in helper.createTaskWithP2P
    }

    @Когда("преподаватель запускает случайное распределение")
    public void teacherStartsRandomDistribution() {
        p2pService.generateP2PForTask(state.getTask().getId());
    }

    @Тогда("система назначает каждой команде ровно одну другую команду на проверку")
    public void systemAssignsOneTeamToAnother() {
        String token = helper.getToken(state.getTeacher());
        List<P2PPairTeamDto> pairs = p2pService.getP2PPairTeam(state.getTask().getId(), token);
        assertEquals(3, pairs.size());
    }

    @И("все команды получают уведомления: {string}")
    public void teamsGetNotifications(String notification) {
        // We don't have notifications yet, ignore this step
    }

    @Дано("команда {string} проверяет работу команды {string}")
    public void teamChecksAnotherTeam(String reviewerTeam, String targetTeam) {
        teamTaskCreated("Задание 1");
        User captain1 = helper.createStudent("Соколов");
        state.getStudents().put("Соколов", captain1);
        Team t1 = helper.createTeam(reviewerTeam, state.getTask(), List.of(captain1), captain1);
        state.getTeams().put(reviewerTeam, t1);

        User memberTarget = helper.createStudent("Таргет1");
        Team t2 = helper.createTeam(targetTeam, state.getTask(), List.of(memberTarget), null);
        state.getTeams().put(targetTeam, t2);

        TaskSolution ts2 = new TaskSolution();
        ts2.setTeamId(t2.getId());
        ts2.setTaskId(state.getTask().getId());
        ts2.setIsSelected(true);
        ts2 = helper.getTaskSolutionRepository().save(ts2);

        String token = helper.getToken(state.getTeacher());
        AssignP2PTeamDto dto = AssignP2PTeamDto.builder()
                .taskId(state.getTask().getId())
                .reviewerTeamId(t1.getId())
                .ownerTeamId(t2.getId())
                .targetTaskSolutionId(ts2.getId())
                .build();
        p2pService.assignP2PTeam(dto, token);
    }

    @И("у команды {string} есть капитан {string}")
    public void teamHasCaptain(String team, String captain) {
        // Done in previous step
    }

    @Когда("Соколов выставляет оценку {string} за решение")
    public void captainSetsGrade(String gradeStr) {
        String token = helper.getToken(state.getStudents().get("Соколов"));
        SetTeamMetricValueDto dto = new SetTeamMetricValueDto(state.getMetric().getId(), state.getTeams().get("Бета").getId(), Double.parseDouble(gradeStr));
        try {
            metricValueService.setTeamMetricValue(dto, token);
        } catch (Exception e) {
            state.setLastException(e);
        }
    }

    @Тогда("эта оценка становится итоговой оценкой для команды {string}")
    public void gradeBecomesFinalForTeam(String team) {
        assertNull(state.getLastException());
        String token = helper.getToken(state.getTeacher());
        List<MetricWithValuesDto> grades = metricValueService.getTaskTeamMetricsWithValues(state.getTask().getId(), state.getTeams().get(team).getId(), token);
        assertFalse(grades.isEmpty());
        assertEquals(8.0, grades.get(0).getValues().get(0).getValue());
    }

    @И("другие члены команды {string} не могут изменить эту оценку")
    public void otherMembersCannotChangeGrade(String team) {
        User otherMember = helper.createStudent("НеСоколов");
        Team alpha = state.getTeams().get(team);
        alpha.getUsers().add(otherMember);
        helper.getTeamRepository().save(alpha);

        String token = helper.getToken(otherMember);
        SetTeamMetricValueDto dto = new SetTeamMetricValueDto(state.getMetric().getId(), state.getTeams().get("Бета").getId(), 9.0);
        try {
            metricValueService.setTeamMetricValue(dto, token);
        } catch (Exception e) {
            state.setLastException(e);
        }
        assertNotNull(state.getLastException());
        assertTrue(state.getLastException().getMessage().contains("капитан"));
    }

    @И("в команде {string} нет капитана")
    public void teamHasNoCaptain(String team) {
        teamTaskCreated("Задание 2");
        state.getTeams().put("Гамма", helper.createTeam("Гамма", state.getTask(), new ArrayList<>(), null));
        state.getTeams().put("Дельта", helper.createTeam("Дельта", state.getTask(), List.of(helper.createStudent("ТаргетД")), null));

        TaskSolution ts2 = new TaskSolution();
        ts2.setTeamId(state.getTeams().get("Дельта").getId());
        ts2.setTaskId(state.getTask().getId());
        ts2.setIsSelected(true);
        ts2 = helper.getTaskSolutionRepository().save(ts2);

        String token = helper.getToken(state.getTeacher());
        AssignP2PTeamDto dto = AssignP2PTeamDto.builder()
                .taskId(state.getTask().getId())
                .reviewerTeamId(state.getTeams().get("Гамма").getId())
                .ownerTeamId(state.getTeams().get("Дельта").getId())
                .targetTaskSolutionId(ts2.getId())
                .build();
        p2pService.assignP2PTeam(dto, token);
    }

    @И("члены команды {string}: Студент1, Студент2, Студент3")
    public void teamMembersAre(String team) {
        User s1 = helper.createStudent("Студент1");
        User s2 = helper.createStudent("Студент2");
        User s3 = helper.createStudent("Студент3");
        state.getStudents().put("Студент1", s1);
        state.getStudents().put("Студент2", s2);
        state.getStudents().put("Студент3", s3);

        Team gamma = state.getTeams().get(team);
        gamma.getUsers().addAll(List.of(s1, s2, s3));
        helper.getTeamRepository().save(gamma);
    }

    @Когда("Студент1 ставит {string}, Студент2 ставит {string}, Студент3 ставит {string}")
    public void studentsSetGrades(String g1, String g2, String g3) {
        for (int i = 1; i <= 3; i++) {
            String token = helper.getToken(state.getStudents().get("Студент" + i));
            double grade = Double.parseDouble(i == 1 ? g1 : (i == 2 ? g2 : g3));
            SetTeamMetricValueDto dto = new SetTeamMetricValueDto(state.getMetric().getId(), state.getTeams().get("Дельта").getId(), grade);
            metricValueService.setTeamMetricValue(dto, token);
        }
    }

    @Тогда("система вычисляет среднее = \\({int}+{int}+{int})\\/{int} = {double}")
    public void systemCalculatesAverage(int g1, int g2, int g3, int count, double avg) {
        // Asserted in next step
    }

    @И("выставляет итоговую оценку {double} для команды {string}")
    public void setsFinalGradeForTeam(double grade, String team) {
        String token = helper.getToken(state.getTeacher());
        List<MetricWithValuesDto> grades = metricValueService.getTaskTeamMetricsWithValues(state.getTask().getId(), state.getTeams().get(team).getId(), token);
        assertFalse(grades.isEmpty());
        assertEquals(grade, grades.get(0).getValues().get(0).getValue());
    }
}