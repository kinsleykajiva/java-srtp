@echo off
set CP=demo\target\classes;java-srtp\target\classes
set LIBPATH=java-srtp\src\main\resources
echo Running ROC_DriverDemo...
java --enable-preview --enable-native-access=ALL-UNNAMED -Djava.library.path=%LIBPATH% -cp %CP% demo.io.github.kinsleykajiva.ROC_DriverDemo
echo Exit code: %ERRORLEVEL%
