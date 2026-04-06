#!/bin/bash
COOKIE_FILE="cookies.txt"
curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" -d "fullName=TestReader&email=reader@test.com&password=123" http://localhost:8080/register
curl -s -c $COOKIE_FILE -X POST -d "username=reader@test.com" -d "password=123" http://localhost:8080/login
curl -sv -b $COOKIE_FILE http://localhost:8080/reader/home > response.html 2> curl_err.log
