package org.javawebstack.openapi.client;

import org.javawebstack.command.CommandSystem;
import org.javawebstack.openapi.client.command.JavaCommand;

public class ClientGenerator {

    public static void main(String[] args) {
        new CommandSystem()
                .addCommand("java", new JavaCommand())
                .run(args);
    }

}
