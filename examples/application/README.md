# Application example

## Building

From the root dir, execute:

    ./gradlew -p examples/application installDist

This creates the `examples/application/build/install/example` directory containing:

    examples/application/build/install/example
    ├── bin
    │   ├── example
    │   └── example.bat
    └── lib
        ├── application.jar
        └── toolbelt-master-SNAPSHOT.jar

## Running

Simply execute:

    examples/application/build/install/bin/example
