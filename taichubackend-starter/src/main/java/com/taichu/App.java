package com.taichu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@EnableAsync
@Slf4j
public class App {

    @Value("${algo.service.mock}")
    private boolean isAlgoMock;

    @Value("${algo.small-scale-test}")
    private boolean isAlgoSmallScaleTest;

    public static void main( String[] args ) {
        SpringApplication.run(App.class, args);
        System.out.println( "Hello World!" );
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("===================== Application Configuration =====================");
            log.info("Algo Service Mock Enabled: {}", isAlgoMock);
            log.info("Algo Small Scale Test Enabled: {}", isAlgoSmallScaleTest);
            log.info("=====================================================================");
        };
    }
}
