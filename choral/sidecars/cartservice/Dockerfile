FROM amazoncorretto:21-alpine-jdk@sha256:8b16834e7fabfc62d4c8faa22de5df97f99627f148058d52718054aaa4ea3674

WORKDIR /sidecar

ADD build/distributions/cartservice.tar .

ENTRYPOINT ["cartservice/bin/cartservice"]
