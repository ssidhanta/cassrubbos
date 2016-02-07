#!/bin/bash
/home/surabhi/Fall2012/rtsproj/rts-project/apache-tomcat-6.0.36/bin/shutdown.sh
rm -f rubbos.war
rm -f rubbos_servlets.jar
ant dist
rm -f /home/surabhi/Fall2012/rtsproj/rts-project/apache-tomcat-6.0.36/webapps/rubbos/WEB-INF/lib/rubbos_servlets.jar
cp rubbos_servlets.jar /home/surabhi/Fall2012/rtsproj/rts-project/apache-tomcat-6.0.36/webapps/rubbos/WEB-INF/lib
/home/surabhi/Fall2012/rtsproj/rts-project/apache-tomcat-6.0.36/bin/startup.sh

