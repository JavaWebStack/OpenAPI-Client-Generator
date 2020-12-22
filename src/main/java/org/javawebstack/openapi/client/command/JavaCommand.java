package org.javawebstack.openapi.client.command;

import org.javawebstack.command.Command;
import org.javawebstack.command.CommandResult;
import org.javawebstack.command.CommandSystem;
import org.javawebstack.openapi.client.JavaClientGenerator;
import org.javawebstack.openapi.parser.OpenAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JavaCommand implements Command {

    public CommandResult execute(CommandSystem system, List<String> args, Map<String, List<String>> params) {
        if(args.size() < 1)
            return CommandResult.syntax("java <specFile>");
        File file = new File(args.get(0));
        OpenAPI api = OpenAPI.fromFile(file);
        if(api == null)
            return CommandResult.error("The spec file isn't readable");
        JavaClientGenerator clientGenerator = new JavaClientGenerator(api);

        if(params.containsKey("snippet")){
            for(String name : params.get("snippet")){
                File snippetFile = new File(name);
                if(!snippetFile.exists())
                    return CommandResult.error("Snippet '"+name+"' not found");
                clientGenerator.getSnippet().addAll(readFile(snippetFile));
            }
        }
        if(params.containsKey("s"))
            clientGenerator.setJustSource(true);
        if(params.containsKey("artifact-id"))
            clientGenerator.setArtifactId(params.get("artifact-id").get(0));
        if(params.containsKey("group-id"))
            clientGenerator.setGroupId(params.get("group-id").get(0));
        if(params.containsKey("version"))
            clientGenerator.setVersion(params.get("version").get(0));
        if(params.containsKey("package"))
            clientGenerator.setBasePackage(params.get("package").get(0));
        if(params.containsKey("api-name"))
            clientGenerator.setApiName(params.get("api-name").get(0));

        File targetFolder = new File("");
        if(params.containsKey("out"))
            targetFolder = new File(params.get("out").get(0));

        clientGenerator.generate(targetFolder);
        return CommandResult.success();
    }

    private static List<String> readFile(File file){
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int r;
            while ((r = fis.read(buffer)) != -1)
                baos.write(buffer, 0, r);
            fis.close();
            return Arrays.asList(new String(baos.toByteArray(), StandardCharsets.UTF_8).replace("\r", "").split("\n"));
        }catch (IOException ignored){
            return new ArrayList<>();
        }
    }

}
