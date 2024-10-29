#!/bin/bash

# Start shipping service
/src/shippingservice &

# Start choral sidecar
/choral/shippingservice/bin/shippingservice &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
