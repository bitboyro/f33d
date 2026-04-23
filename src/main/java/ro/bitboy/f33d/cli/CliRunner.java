package ro.bitboy.f33d.cli;

import java.util.UUID;

public class CliRunner {

    public static void createToken(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: create-token <client-name>");
            System.exit(1);
        }
        String name = args[1];
        String token = UUID.randomUUID().toString().replace("-", "");
        System.out.println(token + "=" + name);
        System.err.println("# Append the line above to your tokens.properties file");
        System.exit(0);
    }
}
