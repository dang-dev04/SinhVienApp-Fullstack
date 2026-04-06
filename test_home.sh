#!/bin/bash
COOKIE_FILE="cookies.txt"
curl -s -c $COOKIE_FILE -X POST -d "username=admin@test.com" -d "password=123" http://localhost:8080/login
curl -sv -b $COOKIE_FILE http://localhost:8080/reader/home > response.html 2> curl_err.log
