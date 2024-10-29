#!/bin/bash

# Start productcatalog service
/src/server &

# Start choral sidecar
/choral/productcatalog/bin/productcatalog &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
