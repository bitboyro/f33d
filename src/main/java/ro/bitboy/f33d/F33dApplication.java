package ro.bitboy.f33d;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ro.bitboy.f33d.cli.CliRunner;

@SpringBootApplication
public class F33dApplication {

    public static void main(String[] args) {
        if (args.length > 0 && "create-token".equals(args[0])) {
            CliRunner.createToken(args);
            return;
        }
        SpringApplication app = new SpringApplication(F33dApplication.class);
        String authMode = System.getenv().getOrDefault("F33D_AUTH_MODE", "local");
        if ("keycloak".equals(authMode)) {
            app.setAdditionalProfiles("keycloak");
        }
        app.run(args);
    }
}
