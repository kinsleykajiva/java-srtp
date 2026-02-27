@echo off
set "MVN_PATH=C:\Users\Kinsley\.m2\wrapper\dists\apache-maven-3.8.4-bin\52ccbt68d252mdldqsfsn03jlf\apache-maven-3.8.4\bin\mvn.cmd"
echo Starting deployment... > deploy_log.txt
call "%MVN_PATH%" clean deploy -DskipTests >> deploy_log.txt 2>&1
echo Deployment finished with exit code %ERRORLEVEL% >> deploy_log.txt
