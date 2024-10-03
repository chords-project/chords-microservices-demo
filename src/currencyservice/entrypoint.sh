#!/bin/sh

# Start currency service
node server.js &

# Start choral sidecar
java -jar choral-currencyservice.jar &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
