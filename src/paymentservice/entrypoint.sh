#!/bin/sh

# Start payment service
node index.js &

# Start choral sidecar
/choral/paymentservice/bin/paymentservice &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
