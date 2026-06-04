package ru.hits.car_school_automatization.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.hits.car_school_automatization.CarSchoolAutomatizationApplication;

@CucumberContextConfiguration
@SpringBootTest(classes = CarSchoolAutomatizationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
