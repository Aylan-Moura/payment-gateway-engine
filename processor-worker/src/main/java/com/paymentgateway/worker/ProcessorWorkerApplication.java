package com.paymentgateway.worker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class ProcessorWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcessorWorkerApplication.class, args);
    }
}
