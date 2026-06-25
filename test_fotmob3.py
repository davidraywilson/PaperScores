import requests

url = "https://www.fotmob.com/api/matchDetails?matchId=4393693"
headers = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "application/json"
}
response = requests.get(url, headers=headers)
print(response.status_code)
print(response.text[:200])
