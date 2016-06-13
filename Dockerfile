FROM node
EXPOSE 7000
RUN apt-get -y update && apt-get -y upgrade
RUN apt-get -y install openjdk-7-jdk

RUN npm install bower -g

RUN echo '{ "allow_root": true }' > /root/.bowerrc

RUN npm install -g grunt-cli
RUN apt-get  -y install wget
RUN wget -q -O /usr/bin/lein     https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein     && chmod +x /usr/bin/lein &&     lein

ENV APP_HOME /usr/local/visualreview
ENV APP_TEMP_HOME /usr/local/visualreviewTemp

# RUN mkdir -p ${APP_HOME}
# RUN mkdir -p ${APP_TEMP_HOME}


WORKDIR ${APP_TEMP_HOME}

# add source
ADD . ${APP_TEMP_HOME}

RUN cd $(npm root -g)/npm \
    && npm install fs-extra \
    && sed -i -e s/graceful-fs/fs-extra/ -e s/fs.rename/fs.move/ ./lib/utils/rename.js

RUN LEIN_ROOT=true lein uberjar

WORKDIR ${APP_HOME}
ADD ${APP_TEMP_HOME}/target/*-standalone.jar ${APP_HOME}
# COPY target/*-standalone.jar /usr/local/visualreview ||| CONTINUE with warning

ADD ${APP_TEMP_HOME}/config.edn .

RUN rm -fr $APP_TEMP_HOME

# WORKDIR $APP_HOME
RUN mv `ls *-standalone.jar` app-standalone.jar

# RUN mkdir -p $APP_HOME

# clean
RUN apt-get remove -y wget  \
    && apt-get clean \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

CMD ["java", "-jar", "app-standalone.jar"]


