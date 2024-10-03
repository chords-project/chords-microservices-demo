#!/bin/bash

# Start shipping service
/src/shippingservice &

# Start choral sidecar
java -jar /src/choral-shippingservice.jar &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
