package ru.hits.car_school_automatization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CarSchoolAutomatizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarSchoolAutomatizationApplication.class, args);
    }

}
