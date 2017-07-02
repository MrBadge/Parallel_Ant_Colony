FROM python:alpine
MAINTAINER Anatoly Maltsev <malcevanatoly@gmail.com>

RUN apk update && apk upgrade
RUN apk add bash && sed -i -e "s/bin\/ash/bin\/bash/" /etc/passwd

WORKDIR /src

COPY final_matrix.json /src/
RUN mkdir /src/pants
COPY pants/* /src/pants/
COPY pants/__init__.py /src/pants/__init__.py
COPY slave.py /src/

CMD python3 slave.py