import requests
import json
import re

url = "https://www.fotmob.com/teams/9825/overview/"
headers = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}
response = requests.get(url, headers=headers)
text = response.text
regex = re.compile(r'<script id="__NEXT_DATA__" type="application/json">(.*?)</script>', re.DOTALL)
match = regex.search(text)
if match:
    data = json.loads(match.group(1))
    pageProps = data.get("props", {}).get("pageProps", {})
    team_data = pageProps.get("fallback", {}).get("team-9825", {})
    fixtures = team_data.get("fixtures", {})
    print("fixtures keys:", list(fixtures.keys()))
    if "allFixtures" in fixtures:
        print("nextMatch:", json.dumps(fixtures["allFixtures"]["fixtures"][0], indent=2))
