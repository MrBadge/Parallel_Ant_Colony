FROM python:alpine
MAINTAINER Anatoly Maltsev <malcevanatoly@gmail.com>

EXPOSE 8081

RUN apk update && apk upgrade
RUN apk add bash && sed -i -e "s/bin\/ash/bin\/bash/" /etc/passwd

COPY final_matrix.json /src/
WORKDIR /src

COPY master.py /src/

CMD python3 master.py