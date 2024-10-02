#!/bin/bash

# Start frontend service
/src/server &

# Start choral sidecar
java -jar /src/choral-frontend.jar &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
