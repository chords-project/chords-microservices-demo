# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# https://mcr.microsoft.com/product/dotnet/sdk
FROM mcr.microsoft.com/dotnet/sdk:8.0.401-noble@sha256:d37cbb248b78027e3f149ea422bc9dc3bbf5b8c47bd1150e14c9849462235e91 AS builder
WORKDIR /app
COPY service/src/cartservice.csproj .
RUN dotnet restore cartservice.csproj
# -r linux-x64

COPY service/src/ .
RUN dotnet publish cartservice.csproj \
    -p:PublishSingleFile=true \
    # -r linux-x64 \
    --self-contained true \
    -p:PublishTrimmed=true \
    -p:TrimMode=full \
    -c release \
    -o /cartservice

# https://mcr.microsoft.com/product/dotnet/runtime-deps
FROM mcr.microsoft.com/dotnet/runtime-deps:8.0-alpine@sha256:cd52298239f7553c2c18cd4ad5952bcfac7c49c3fa2da04ef5b2504dfd6bb1eb

# RUN apt update && apt install -y openjdk-21-jre
RUN apk add --no-cache openjdk21

WORKDIR /app

COPY ./entrypoint.sh .
RUN chmod 755 ./entrypoint.sh

COPY --from=builder /cartservice .

COPY ./choral-build/cartservice.tar /choral/cartservice.tar
RUN tar -xvaf /choral/cartservice.tar -C /choral/ \
    && rm /choral/cartservice.tar

EXPOSE 7070
ENV DOTNET_EnableDiagnostics=0 \
    ASPNETCORE_HTTP_PORTS=7070

USER 1000
ENTRYPOINT ["/app/entrypoint.sh"]