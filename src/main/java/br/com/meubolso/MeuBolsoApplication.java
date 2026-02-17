package br.com.meubolso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MeuBolsoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeuBolsoApplication.class, args);
    }
}
