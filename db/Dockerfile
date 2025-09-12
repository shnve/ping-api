FROM postgis/postgis:13-3.1  

ENV POSTGRES_DB 'ping-api' 
ENV POSTGRES_USER 'ping-api' 
ENV POSTGRES_PASSWORD 'XXXXXX' 

ADD extension.sh /docker-entrypoint-initdb.d/11_init.sh
RUN chmod -x /docker-entrypoint-initdb.d/11_init.sh
