#!/bin/sh

# Start currency service
node server.js &

# Start choral sidecar
/choral/currencyservice/bin/currencyservice &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
