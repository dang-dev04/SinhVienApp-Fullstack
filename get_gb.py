import urllib.request
import json
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

books = [
    ("Head First Java", 2),
    ("Zero to One", 4),
    ("Harry Potter và Hòn đá", 5),
    ("Doraemon short stories", 6),
    ("Detective Conan vol 1", 7),
    ("Đắc Nhân Tâm Dale", 8)
]

for title, id in books:
    try:
        url = "https://www.googleapis.com/books/v1/volumes?q=" + urllib.parse.quote(title)
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        data = urllib.request.urlopen(req, context=ctx).read()
        js = json.loads(data)
        if "items" in js:
            for item in js["items"]:
                info = item.get("volumeInfo", {})
                if "imageLinks" in info and "thumbnail" in info["imageLinks"]:
                    img = info["imageLinks"]["thumbnail"]
                    img = img.replace("http://", "https://")
                    print(f"UPDATE book SET image = '{img}' WHERE id = {id};")
                    break
    except Exception as e:
        print(e)
