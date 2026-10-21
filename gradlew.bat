@echo off
setlocal
set STRIDE_ROOT=%~dp0
if defined JAVA_HOME (set "STRIDE_JAVA=%JAVA_HOME%\bin\java.exe") else (set "STRIDE_JAVA=java.exe")
"%STRIDE_JAVA%" %JAVA_OPTS% %GRADLE_OPTS% -classpath "%STRIDE_ROOT%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
endlocal
