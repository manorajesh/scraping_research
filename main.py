from bs4 import BeautifulSoup
import requests

url = 'https://www.indeed.com/jobs?q=&l=El+Segundo%2C+CA&radius=10&start=0&vjk=e531c26c96d74ed8'
response = requests.get(url)

print(response.status_code)

soup = BeautifulSoup(response.text, 'html.parser')

results = soup.find(id='mosaic-provider-jobcards')

print(results.prettify())