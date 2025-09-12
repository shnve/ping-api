package fr.shom.ping;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.springframework.boot.SpringApplication.run;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "${ping.shedlock.default-lock-time-at-most-for}")
@OpenAPIDefinition(servers = {@Server(url = "/", description = "Base Server URL")})
public class PingApiApplication {

    public static void main(String[] args) {
        run(PingApiApplication.class, args);
    }

}
