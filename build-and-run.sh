#!/bin/sh

echo 'Building sidecars'
(cd choral/sidecars && gradle copyApp)

echo 'Building and starting containers'
skaffold run

echo '---'
echo 'Everything should be up and running now!'
echo 'Web shop running on: http://localhost:80'
echo 'Load generator running on: http://localhost:8089'
echo ''
echo 'For Traefik, first enable port forwarding, by running the following command first'
echo 'Afterwards Traefik is accessible from: http://localhost:16686'
echo '> kubectl port-forward service/jaeger-query 16686'
echo ''
echo 'To stop the cluster run: skaffold delete'
