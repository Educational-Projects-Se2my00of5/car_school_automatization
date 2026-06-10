package ru.hits.car_school_automatization.bdd;


import io.cucumber.spring.ScenarioScope;
import lombok.Data;
import org.springframework.stereotype.Component;
import ru.hits.car_school_automatization.entity.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ScenarioScope
public class BddState {
    private User teacher;
    private Map<String, User> students = new HashMap<>();
    private Map<String, Team> teams = new HashMap<>();
    private Channel channel;
    private Post post;
    private Task task;
    private Map<String, Solution> solutions = new HashMap<>();
    private Map<String, TaskSolution> taskSolutions = new HashMap<>();
    private Metric metric;
    
    private Exception lastException;
}
