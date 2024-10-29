#!/bin/sh

# Start cart service
/app/cartservice

# Start choral wrapper
/choral/cartservice/bin/cartservice &

# Wait for any process to exit
wait

# Exit with status of process that exited first
exit $?
