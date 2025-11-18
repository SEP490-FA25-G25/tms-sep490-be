package org.fyp.tmssep490be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TmsSep490BeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmsSep490BeApplication.class, args);
    }

}
