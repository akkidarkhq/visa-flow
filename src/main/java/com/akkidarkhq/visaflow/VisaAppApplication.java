package com.akkidarkhq.visaflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@SpringBootApplication
public class VisaAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisaAppApplication.class, args);
    }

}
