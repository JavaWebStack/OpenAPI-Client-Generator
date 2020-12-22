package org.javawebstack.openapi.client;

import lombok.Getter;
import lombok.Setter;
import org.javawebstack.openapi.parser.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter @Setter
public class JavaClientGenerator {

    private static final List<String> reservedKeywords = new ArrayList<String>(){{
        add("interface");
        add("private");
        add("public");
        add("enum");
        add("abstract");
        add("class");
        add("final");
        add("static");
        add("import");
        add("package");
        add("new");
        add("throws");
        add("protected");
        add("void");
        add("default");
    }};

    OpenAPI api;
    String basePackage = "com.example";
    String apiName;
    boolean justSource = false;
    String groupId;
    String artifactId;
    String version;

    public JavaClientGenerator(OpenAPI api){
        this.api = api;
        final String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for(char c : api.getInfo().getTitle().toCharArray()){
            if(allowedChars.contains(String.valueOf(c)))
                sb.append(c);
        }
        apiName = sb.toString();
        artifactId = apiName;
        version = api.getInfo() != null ? api.getInfo().getVersion() : null;
        if(version == null)
            version = "1.0";
    }

    private void writeClassFile(File targetFolder, String className, String content){
        File sourceFolder = justSource ? targetFolder : new File(targetFolder, "src/main/java");
        File file = new File(sourceFolder, className.replace(".", "/")+".java");
        writeFile(file, content);
    }

    public void generate(File targetFolder){
        if(!justSource)
            generatePomFile(targetFolder);
        generateClient(targetFolder);
        generateException(targetFolder);
        api.getComponents().getSchemas().forEach((name, schema) -> generateSchema(targetFolder, name, schema));
        api.getComponents().getResponses().forEach((name, response) -> generateResponse(targetFolder, name, response));
        api.getTags().forEach(t -> generateTag(targetFolder, t.getName()));
    }

    private void generatePomFile(File targetFolder){
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "    <groupId>"+(groupId != null ? groupId : basePackage)+"</groupId>\n" +
                "    <artifactId>"+artifactId+"</artifactId>\n" +
                "    <version>"+version+"</version>\n" +
                "\n" +
                "    <repositories>\n" +
                "        <repository>\n" +
                "            <id>javawebstack</id>\n" +
                "            <url>https://repo.javawebstack.org</url>\n" +
                "        </repository>\n" +
                "    </repositories>\n" +
                "\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.javawebstack</groupId>\n" +
                "            <artifactId>HTTP-Client</artifactId>\n" +
                "            <version>1.0-SNAPSHOT</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "\n" +
                "    <build>\n" +
                "        <plugins>\n" +
                "            <plugin>\n" +
                "                <groupId>org.apache.maven.plugins</groupId>\n" +
                "                <artifactId>maven-compiler-plugin</artifactId>\n" +
                "                <configuration>\n" +
                "                    <source>8</source>\n" +
                "                    <target>8</target>\n" +
                "                </configuration>\n" +
                "            </plugin>\n" +
                "        </plugins>\n" +
                "    </build>\n" +
                "\n" +
                "</project>";
        writeFile(new File(targetFolder, "pom.xml"), pom);
    }

    private void generateClient(File targetFolder){
        StringBuilder sb = new StringBuilder();
        sb
                .append("package ")
                .append(basePackage)
                .append(";\n\n")
                .append("import org.javawebstack.httpclient.HTTPClient;\n")
                .append("import org.javawebstack.httpclient.HTTPRequest;\n")
                .append("import ")
                .append(basePackage)
                .append(".tags.*;\n\n")
                .append("public class ")
                .append(apiName)
                .append("Client extends HTTPClient {\n\n");
        api.getTags().forEach(t -> sb
                .append("    private final ")
                .append(capitalize(t.getName()))
                .append("Tag ")
                .append(t.getName())
                .append(";\n")
        );
        sb
                .append("\n    public ")
                .append(apiName)
                .append("Client() {\n");
        api.getTags().forEach(t -> sb
                .append("        this.")
                .append(t.getName())
                .append(" = new ")
                .append(capitalize(t.getName()))
                .append("Tag(this);\n")
        );
        sb
                .append("    }\n\n")
                .append("    public <T> T orError(HTTPRequest request, Class<T> type) throws ")
                .append(apiName)
                .append("Exception {\n")
                .append("        if(request.status() < 200 || request.status() > 299)\n")
                .append("            throw new ")
                .append(apiName)
                .append("Exception(request);\n")
                .append("        if(type == null)\n")
                .append("            return null;")
                .append("        try {\n")
                .append("            return request.object(type);\n")
                .append("        } catch (Throwable throwable) {\n")
                .append("            throw new ")
                .append(apiName)
                .append("Exception(request, throwable);\n")
                .append("        }\n")
                .append("    }\n\n");
        api.getTags().forEach(t -> sb
                .append("    public ")
                .append(capitalize(t.getName()))
                .append("Tag ")
                .append(t.getName())
                .append("() {\n        return ")
                .append(t.getName())
                .append(";\n    }\n\n")
        );
        sb.append("}");
        writeClassFile(targetFolder, basePackage+"."+apiName+"Client", sb.toString());
    }

    private void generateException(File targetFolder){
        writeClassFile(targetFolder, basePackage+"."+apiName+"Exception", "package " +
                basePackage +
                ";\n\n" +
                "import org.javawebstack.httpclient.HTTPRequest;\n\n" +
                "public class " +
                apiName +
                "Exception extends Exception {\n\n" +
                "    private final HTTPRequest request;\n\n" +
                "    public " +
                apiName +
                "Exception(HTTPRequest request) {\n" +
                "        super(\"HTTP Response \"+request.status()+\": \"+request.string());\n" +
                "        this.request = request;\n" +
                "    }\n\n" +
                "    public " +
                apiName +
                "Exception(HTTPRequest request, Throwable parent) {\n" +
                "        super(parent);\n" +
                "        this.request = request;\n" +
                "    }\n\n" +
                "    public HTTPRequest getRequest() {\n" +
                "        return request;\n" +
                "    }\n\n" +
                "}"
        );
    }

    private void generateTag(File targetFolder, String tag){
        StringBuilder sb = new StringBuilder();
        sb
                .append("package ")
                .append(basePackage)
                .append(".tags;\n\n")
                .append("import org.javawebstack.httpclient.HTTPRequest;\n")
                .append("import ")
                .append(basePackage)
                .append(".*;\n");
        if(api.getComponents().getSchemas() != null && api.getComponents().getSchemas().size() > 0){
            sb
                    .append("import ")
                    .append(basePackage)
                    .append(".schemas.*;\n");
        }
        if(api.getComponents().getResponses() != null && api.getComponents().getResponses().size() > 0){
            sb
                    .append("import ")
                    .append(basePackage)
                    .append(".responses.*;\n");
        }
        sb
                .append("\n")
                .append("public class ")
                .append(capitalize(tag))
                .append("Tag {\n\n")
                .append("    private final ")
                .append(apiName)
                .append("Client client;\n\n")
                .append("    public ")
                .append(capitalize(tag))
                .append("Tag(")
                .append(apiName)
                .append("Client client) {\n")
                .append("        this.client = client;\n")
                .append("    }\n\n");
        api.getPaths().forEach((path, pathObject) -> {
            pathObject.getMethods().forEach((method, operation) -> {
                if(!operation.getTags().contains(tag))
                    return;
                List<OpenAPIParameter> parameters = new ArrayList<>();
                if(pathObject.getParameters() != null)
                    parameters.addAll(pathObject.getParameters());
                if(operation.getParameters() != null)
                    parameters.addAll(operation.getParameters());
                String returnType = getJavaType(
                        getResponseSchema(
                                operation
                                        .getResponses()
                                        .entrySet()
                                        .stream()
                                        .filter(e -> e.getKey().startsWith("2"))
                                        .map(Map.Entry::getValue)
                                        .findFirst()
                                        .orElse(null)
                        )
                );
                List<String> methodParams = new ArrayList<>();
                String url = "\""+path+"\"";
                for(OpenAPIParameter p : parameters.stream().filter(p -> p.getIn() == OpenAPIParameter.Location.PATH).collect(Collectors.toList())){
                    url = url.replace("{"+p.getName()+"}", "\" + "+p.getName()+" + \"");
                    String type = getJavaType(p.getSchema());
                    if(type == null)
                        type = "String";
                    methodParams.add(type+" "+p.getName());
                }
                boolean hasQuery = false;
                if(parameters.stream().filter(p -> p.getIn() == OpenAPIParameter.Location.QUERY).findFirst().orElse(null) != null){
                    hasQuery = true;
                    methodParams.add("Map<String, String> queryParams");
                }
                String bodyType = getJavaType(getContentSchema(operation.getRequestBody() != null ? operation.getRequestBody().getContent() : null));
                if(bodyType != null)
                    methodParams.add(bodyType+" body");
                sb.append("    public ");
                sb.append(returnType == null ? "void" : returnType);
                sb
                        .append(" ")
                        .append(operation.getOperationId())
                        .append("(")
                        .append(String.join(", ", methodParams))
                        .append(") throws ")
                        .append(apiName)
                        .append("Exception {\n")
                        .append("        HTTPRequest request = client.")
                        .append(method)
                        .append("(")
                        .append(url)
                        .append(");\n");
                if(hasQuery){
                    sb
                            .append("        if(queryParams != null) {\n")
                            .append("            for(String key : queryParams) {\n")
                            .append("                request.query(key, queryParams.get(key));")
                            .append("            }\n")
                            .append("        }\n");
                }
                if(bodyType != null)
                    sb.append("        request.jsonBody(body);\n");
                sb
                        .append("        ")
                        .append(returnType == null ? "" : "return ")
                        .append("client.orError(request.execute(), ")
                        .append(returnType == null ? "null" : (returnType + ".class"))
                        .append(");\n")
                        .append("    }\n\n");
            });
        });
        sb.append("}");
        writeClassFile(targetFolder, basePackage+".tags."+capitalize(tag)+"Tag", sb.toString());
    }

    private void generateSchema(File targetFolder, String name, OpenAPISchema schema){
        StringBuilder sb = new StringBuilder();
        sb.append("package ")
                .append(basePackage)
                .append(".schemas;\n\n")
                .append("import com.google.gson.annotations.SerializedName;\n\n");
        generateSchema(sb, "", name, schema);
        writeClassFile(targetFolder, basePackage+".schemas."+name, sb.toString());
    }

    private void generateResponse(File targetFolder, String name, OpenAPIResponse response){
        StringBuilder sb = new StringBuilder();
        sb.append("package ")
                .append(basePackage)
                .append(".responses;\n\n");
        if(api.getComponents().getSchemas() != null && api.getComponents().getSchemas().size() > 0){
            sb
                    .append("import ")
                    .append(basePackage)
                    .append(".schemas.*;\n");
        }
        sb.append("import com.google.gson.annotations.SerializedName;\n\n");
        OpenAPISchema schema = getResponseSchema(response);
        if(schema == null)
            return;
        generateSchema(sb, "", name, schema);
        writeClassFile(targetFolder, basePackage+".responses."+name, sb.toString());
    }

    private void generateSchema(StringBuilder sb, String intendation, String name, OpenAPISchema schema){
        Map<String, String> types = new HashMap<>();
        Map<String, OpenAPISchema> subSchemas = new HashMap<>();
        Map<String, String> serializedNames = new HashMap<>();
        if(schema.getProperties() != null){
            schema.getProperties().forEach((pName, pSchema) -> {
                String type = getJavaType(pSchema);
                if(type == null){
                    String subName = capitalize(pName);
                    if(subName.endsWith("ies")){
                        subName = subName.substring(0, subName.length()-3)+"y";
                    }else if(subName.endsWith("es")){
                        subName = subName.substring(0, subName.length()-2);
                    }else if(subName.endsWith("s")) {
                        subName = subName.substring(0, subName.length() - 1);
                    }
                    subSchemas.put(subName, pSchema);
                    type = subName;
                }
                String sName = getSerializedName(pName, type);
                if(sName != null){
                    serializedNames.put(sName, pName);
                    pName = sName;
                }
                types.put(pName, type);
            });
        }
        sb
                .append(intendation)
                .append("public ");
        if(intendation.length() > 0)
            sb.append("static ");
        sb
                .append("class ")
                .append(name)
                .append(" {\n\n");
        types.forEach((pName, pType) -> {
            if(serializedNames.containsKey(pName))
                sb.append(intendation).append("    @SerializedName(\"").append(serializedNames.get(pName)).append("\")\n");
            sb.append(intendation).append("    private ").append(pType).append(" ").append(pName).append(";\n");
        });
        sb.append("\n");
        types.forEach((pName, pType) -> {
            sb
                    .append(intendation)
                    .append("    public void set")
                    .append(capitalize(pName))
                    .append("(")
                    .append(pType)
                    .append(" ")
                    .append(pName)
                    .append(") {\n")
                    .append(intendation)
                    .append("        this.")
                    .append(pName)
                    .append(" = ")
                    .append(pName)
                    .append(";\n")
                    .append(intendation)
                    .append("    }\n\n")
                    .append(intendation)
                    .append("    public ")
                    .append(pType)
                    .append(" get")
                    .append(capitalize(pName))
                    .append("() {\n")
                    .append(intendation)
                    .append("        return this.")
                    .append(pName)
                    .append(";\n")
                    .append(intendation)
                    .append("    }\n\n");
            if(pType.equals("Boolean")){
                sb.append(intendation)
                        .append("    public ")
                        .append(pType)
                        .append(" is")
                        .append(capitalize(pName))
                        .append("() {\n")
                        .append(intendation)
                        .append("        return this.")
                        .append(pName)
                        .append(" != null && this.")
                        .append(pName)
                        .append(";\n")
                        .append(intendation)
                        .append("    }\n\n");
            }
        });
        if(subSchemas.size()>0){
            subSchemas.forEach((subName, subSchema) -> generateSchema(sb, intendation+"    ", subName, subSchema));
            sb.append("\n");
        }
        sb.append(intendation).append("}");
    }

    private String getSerializedName(String name, String type){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < name.length(); i++){
            if(name.charAt(i) == '_' || name.charAt(i) == '-' || name.charAt(i) == '.'){
                i++;
                if(i < name.length())
                    sb.append(Character.toUpperCase(name.charAt(i)));
            }else{
                sb.append(name.charAt(i));
            }
        }
        String sName = sb.toString();
        if(reservedKeywords.contains(name)){
            if(type == null)
                type = "Object";
            if(type.endsWith("[]"))
                type = "Array";
            switch (type){
                case "Integer":
                case "Long":
                case "Float":
                case "Double":
                case "String":
                case "java.util.UUID":
                case "Array":
                case "com.google.gson.JsonObject":
                    break;
                default:
                    type = "Object";
                    break;
            }
            sName = sName+type;
        }
        if(sName.equals(name))
            return null;
        return sName;
    }

    private String getJavaType(OpenAPISchema schema){
        if(schema == null)
            return null;
        if(schema.getReference() != null)
            return schema.getReference().split("/")[3];
        switch (schema.getType()){
            case INTEGER: {
                return schema.getFormat() == OpenAPIFormat.INT64 ? "Long" : "Integer";
            }
            case NUMBER: {
                return schema.getFormat() == OpenAPIFormat.FLOAT ? "Float" : "Double";
            }
            case STRING: {
                return schema.getFormat() == OpenAPIFormat.UUID ? "java.util.UUID" : "String";
            }
            case BOOLEAN: {
                return "Boolean";
            }
            case ARRAY: {
                String elementType = schema.getItems() == null ? "com.google.gson.JsonObject" : getJavaType(schema.getItems());
                return elementType+"[]";
            }
            case OBJECT: {
                return "com.google.gson.JsonObject";
            }
        }
        return null;
    }

    private OpenAPISchema getResponseSchema(OpenAPIResponse response){
        if(response == null)
            return null;
        return getContentSchema(response.getContent());
    }

    private OpenAPISchema getContentSchema(OpenAPIContent content){
        if(content == null)
            return null;
        if(content.getJson() != null){
            return content.getJson().getSchema();
        }else if(content.getForm() != null){
            return content.getForm().getSchema();
        }else if(content.getXml() != null){
            return content.getXml().getSchema();
        }
        return null;
    }

    private static String capitalize(String source){
        if(source.length() == 0)
            return null;
        return Character.toUpperCase(source.charAt(0))+source.substring(1);
    }

    private static void writeFile(File file, String content){
        if(!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
        } catch (IOException e) {
        }
    }

}
