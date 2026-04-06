curl -s "https://openlibrary.org/search.json?q=doraemon+vol+1" | grep -o 'cover_i":[0-9]*' | head -1
curl -s "https://openlibrary.org/search.json?q=detective+conan+vol+1" | grep -o 'cover_i":[0-9]*' | head -1
curl -s "https://openlibrary.org/search.json?q=how+to+win+friends+and+influence+people" | grep -o 'cover_i":[0-9]*' | head -1
