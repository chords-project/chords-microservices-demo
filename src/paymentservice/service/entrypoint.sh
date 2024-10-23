#!/bin/sh

# Start payment service
node index.js &

# Start choral sidecar
java -jar choral-paymentservice.jar &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
