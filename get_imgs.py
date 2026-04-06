import urllib.request
import json
import re

def search_img(query):
    url = f"https://html.duckduckgo.com/html/?q={urllib.parse.quote(query + ' filetype:jpg')}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        html = urllib.request.urlopen(req).read().decode('utf-8')
        links = re.findall(r'src="([^"]+)"', html)
        for l in links:
            if l.startswith('//'): l = 'https:' + l
            if 'external-content' in l: return l
        return ""
    except Exception as e:
        return str(e)

for q in ["Conan manga vol 1 cover tiki", "Đắc Nhân Tâm cover tiki", "Doraemon manga vol 1 cover wiki", "Harry Potter hòn đá phù thuỷ cover nxb trẻ", "Head First Java cover o reilly", "Zero to One peter thiel cover"]:
    print(q, search_img(q))
