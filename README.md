Elephant
========

[![Join the chat at https://gitter.im/jusu/Elephant](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jusu/Elephant?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Notetaker with a classic interface.

## Installing and Running

### Requirements

Elephant runs on any system equipped with the Java Virtual Machine (1.8 or newer), which can be downloaded at no cost from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

### Installing and Running:

Elephant uses gradle build system.

Use gradlew script on linux and gradlew.bat on Windows.

You can run app at least 3 ways:

run app directly with gradle:
bash ./gradlew run

build application 'fat' jar, move to jar directory and run it with java:
bash ./gradlew jar; cd build/libs; java -jar Elephant

assemble distribution with gradle, move to dist directory, unpack it and run:
bash ./gradlew distZip; cd build/distributions; unzip Elephant.zip; ./Elephant/bin/Elephant 

You can see other gradle tasks available with ./gradlew tasks.
For more information you can read this https://spring.io/guides/gs/gradle/

go to http://elephant.mine.nu
