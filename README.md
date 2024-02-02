# RuTiCo (Ru-n-ti-me Co-mpiler)

This project provides a helper library and a maven plugin to compile and run Java source code at runtime.

## Usage as Library or Launcher

[See here](https://hypfvieh.github.io/runtime-compiler/runtime-compiler-lib/usage.html)

## Additional Information
The library does not have any dependencies besides running on a JDK (***not a JRE***).

All logging is handled by Java Platform Logger (JPL).
This means if you want nicer logging or using your favorite logging framework (e.g. slf4j or log4j)
you have to provide the artifacts the additional required briding libraries as well (e.g. org.apache.logging.log4j:log4j-jpl / org.slf4j:slf4j-jdk-platform-logging).

## Maven plugin usage
You can use the RuTiCo maven plugin to compile and run Java source during your maven build.
The source does not have to be part of the regular source folder for your project. It can be anywhere
as long as RuTiCo can resolve the path.

For a full list of possible configuration parameters look [here](https://hypfvieh.github.io/runtime-compiler/java-runtime-compiler-maven-plugin/execute-mojo.html)

Example:
```
    <plugin>
        <groupId>com.github.hypfvieh.java</groupId>
        <artifactId>runtime-compiler-maven-plugin</artifactId>
        <version>${project.version}</version>
        <executions>
            <!-- Run plugin with specific execution:
                    mvn com.github.hypfvieh.java:runtime-compiler-maven-plugin:execute@execution-id
            -->
            <execution>
                <id>print-env</id>
                <goals>
                    <goal>execute</goal>
                </goals>
                <phase>compile</phase>
                <configuration>
                    <workingDirectory>${project.basedir}</workingDirectory>
                    <classOutputDir>target/rutico</classOutputDir>
                    <mainClass>com.github.hypfvieh.java.sample.ShowEnvProps</mainClass>
                    <sourceLocations>
                        <sourceLocation>other/sources</sourceLocation>
                    </sourceLocations>
                </configuration>
            </execution>
        </executions>
    </plugin>
```
