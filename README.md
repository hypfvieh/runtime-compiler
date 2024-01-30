# RuTiCo (_Ru_n_ti_me-_Co_mpiler)

This project provides a helper library and a maven plugin to compile and run Java source code at runtime.

## Library Usage

To get this library to work, you have to execute your program using a JDK.
No other dependency besides slf4j is needed.

Add the library to your project:
```
    <dependency>
        <groupId>com.github.hypfvieh.java</groupId>
        <artifactId>runtime-compiler-lib</artifactId>
        <version>${project.version}</version>
    </dependency
```

Now you can use the library like this:
```java
    private SomeClassInstance createClassInstance() {
        File myFile = new File("path/to/file/MyClass.java");
        Class<?> fileCreator = JavaFileLoader.createClassFromFile(myFile.getAbsolutePath());
        Object javaObjFromFile = fileCreator.getDeclaredConstructor().newInstance();

        if (!(javaObjFromFile instanceof SomeClassInstance)) {
            throw new ClassCastException(String.format("Source %s is not a subclass of %s", myFile, SomeClassInstance.class.getName()));
        }

        return (SomeClassInstance) javaObjFromFile;
    }
```

This will look for a file 'MyClass.java' in 'path/to/file/' and will compile it.
After that it will try to call the default no-args constructor and casts it.
The class `SomeClassInstance` is extended by the source in 'MyClass.java' so it is possible to
return a more concrete class type instead of `Object`.

The library will take care about compiling and also tries to find all classes imported directly or through inheritance by the
compiled source.

It is possible to use inheritance of sources which also have to be compiled.
E.g. when `ClassB` extends `ClassA`, RuTiCo will try to compile `ClassB` which fails.
Due to the errors created by the compiler RuTiCo will try to find `ClassA` using the compiler error.
After compiling `ClassA` the failed compilation of `ClassB` is retried.

## Maven plugin usage
You can use the RuTiCo maven plugin to compile and run Java source during your maven build.
The source does not have to be part of the regular source folder for your project. It can be anywhere
as long as RuTiCo can resolve the path.

For a full list of possible configuration parameters look at: https://hypfvieh.github.io/rutico/

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
    </plugin>
```
