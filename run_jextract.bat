@echo off
set JEXTRACT=c:\Users\Kinsley\jextract-25\bin\jextract.bat
echo Running jextract...
call "%JEXTRACT%" --output java-srtp\src\main\java --target-package io.github.kinsleykajiva.libsrtp --header-class-name srtp_h -I libsrtp/include -I libsrtp/crypto/include libsrtp/include/master.h
echo Exit code: %ERRORLEVEL%
