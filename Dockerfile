# Stage 1: Build stage
FROM amazoncorretto:17 AS build

RUN yum install -y tar unzip

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=GMT

COPY . /opt/refactoringminer/

RUN /opt/refactoringminer/gradlew -x test -p /opt/refactoringminer build -PbuildVersion=DockerBuild 2>/dev/null
RUN unzip /opt/refactoringminer/build/distributions/RefactoringMiner-DockerBuild.zip -d /opt/refactoringminer/build/distributions

# Stage 2: Runtime stage
FROM amazoncorretto:17

RUN mkdir -p /diff/left /diff/right

COPY --from=build /opt/refactoringminer/build/distributions/RefactoringMiner-DockerBuild /opt/refactoringminer/
RUN ln -s /opt/refactoringminer/bin/RefactoringMiner /usr/bin/refactoringminer

ENV LANG C.UTF-8
EXPOSE 6789

WORKDIR /diff
