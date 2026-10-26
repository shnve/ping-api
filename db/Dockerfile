FROM postgis/postgis:13-3.1

ENV POSTGRES_DB 'ping-api'
ENV POSTGRES_USER 'ping-api'
ENV POSTGRES_PASSWORD 'XXXXXX'

# Copiamos tu script original
ADD extension.sh /docker-entrypoint-initdb.d/11_init.sh

# CORRECCIÓN: Le damos permisos de ejecución (+x) en lugar de quitárselos (-x)
RUN chmod +x /docker-entrypoint-initdb.d/11_init.sh

