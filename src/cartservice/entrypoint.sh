#!/bin/bash

# Start cart service
/app/cartservice &

# Start choral wrapper
java -jar /choral/cartservice.jar &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
