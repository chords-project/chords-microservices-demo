#!/bin/bash

# Start productcatalog service
/src/server &

# Start choral sidecar
java -jar /src/choral-productcatalog.jar &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
