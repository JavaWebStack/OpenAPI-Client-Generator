# OpenAPI-Client-Generator
A very basic OpenAPI Client Generator

## Warning
This generator was built to work with specific specification files and does not aim to fully cover the OpenAPI specification.

## Usage (Java Target)
`java -jar JAR_FILE java <specFile>`

### Additional Parameters
Parameter | Description | Default
--- | --- | ---
--out [folder] | Specify Output Folder | Current Directory
--package [package] | Specify Client Package | `com.example`
--api-name [name] | The base name for classes to use | `title` from API Info (spacing removed)
-s | Only output java sources (without maven structure) | false
--group-id [groupId] | Maven Group Id | Package Name (see --package)
--artifact-id [artifactId] | Maven Artifact Id | API Name (see --api-name)
--version [version] | Maven Artifact Version | `version` from API Info (1.0 if not present)
--snippet [file] | A snippet that will be added to the client (multiple allowed) | None
