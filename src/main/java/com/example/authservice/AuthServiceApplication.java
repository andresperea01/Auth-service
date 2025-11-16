package com.example.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);

        System.out.println("\n==========================================");
        System.out.println("🔐 AUTH-SERVICE INICIADO CORRECTAMENTE");
        System.out.println("==========================================");
        System.out.println("📄 Login Page: http://localhost:8081/login.html");
        System.out.println("📊 Dashboard: http://localhost:8081/dashboard.html");
        System.out.println("📄 Swagger UI: http://localhost:8081/swagger-ui.html");
        System.out.println("💾 H2 Console: http://localhost:8081/h2-console");
        System.out.println("==========================================\n");
    }
}