@ECHO OFF

SET SERVICE_HOST=http://localhost:8888
REM SET SERVICE_HOST=http://pasta-d.lternet.edu
REM SET SERVICE_HOST=http://pasta-s.lternet.edu:8080
REM SET SERVICE_HOST=http://pasta.lternet.edu

SET SCOPE=edi
SET IDENTIFIER=8
SET REVISION=2

curl -i -X GET %SERVICE_HOST%/package/eml/%SCOPE%/%IDENTIFIER%/%REVISION%
