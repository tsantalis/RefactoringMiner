FROM amazoncorretto:17

RUN yum install -y tar unzip

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=GMT

COPY . /opt/refactoringminer/

RUN /opt/refactoringminer/gradlew -x test -p /opt/refactoringminer build 2>/dev/null
RUN unzip /opt/refactoringminer/build/distributions/*.zip -d /opt/refactoringminer/build/distributions
RUN ln -s /opt/refactoringminer/build/distributions/RefactoringMiner-*/bin/RefactoringMiner /usr/bin/refactoringminer
ENV LANG C.UTF-8
EXPOSE 6789
#ENTRYPOINT ["refactoringminer"]
