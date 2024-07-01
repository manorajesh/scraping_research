from selenium import webdriver
from urllib.parse import quote_plus

class IndeedScraper:
    def __init__(self, radius=10, location='El Segundo, CA'):
        self.url = f'https://www.indeed.com/jobs?q=&l={quote_plus(location)}&radius={radius}&sort=date'
        print(self.url)
        self.radius = radius
        self.location = location
        self.browser = webdriver.Firefox()
        self.browser.get(self.url)

    def get_jobs(self):
        div = self.browser.find_element('id', 'mosaic-provider-jobcards')
        lis = div.find_elements('tag name', 'li')

        # Get the title of each job in list
        for li in lis:
            try:
                title = li.find_element('tag name', 'span').text
                if title != '':
                    print(title)
            except Exception as e:
                title = 'N/A'

    def close_browser(self):
        self.browser.quit()
    

if __name__ == '__main__':
    scraper = IndeedScraper()
    scraper.get_jobs()
    scraper.close_browser()