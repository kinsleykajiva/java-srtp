@echo off
set "MVN_PATH=C:\Users\Kinsley\.m2\wrapper\dists\apache-maven-3.8.4-bin\52ccbt68d252mdldqsfsn03jlf\apache-maven-3.8.4\bin\mvn.cmd"
call "%MVN_PATH%" compile
java --enable-preview --enable-native-access=ALL-UNNAMED -cp "target/classes;C:/Users/Kinsley/.m2/repository/io/github/kinsleykajiva/java-srtp/0.1.0/java-srtp-0.1.0.jar" demo.io.github.kinsleykajiva.SrtpDemo
