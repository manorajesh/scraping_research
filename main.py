from selenium import webdriver
from urllib.parse import quote_plus

class IndeedScraper:
    def __init__(self, radius=10, location='El Segundo, CA', start=0):        
        self.radius = radius
        self.location = location
        self.start = start

        self.assemble_url()
        self.browser = webdriver.Firefox()
        self.browser.get(self.url)

    def assemble_url(self):
        self.url = f'https://www.indeed.com/jobs?q=&l={quote_plus(self.location)}&radius={self.radius}&sort=date&start={self.start}'
        print(f'Using url: {self.url}')

    def get_jobs(self):
        try:
            div = self.browser.find_element('id', 'mosaic-provider-jobcards')
            lis = div.find_elements('tag name', 'li')
        except Exception as e:
            print('No more jobs found: ', e)
            return

        # Get the title of each job in list
        for li in lis:
            try:
                title = li.find_element('tag name', 'span').text
                if title != '':
                    print(title)
            except Exception as e:
                title = 'N/A'

        self.next_page()

    def next_page(self):
        self.start += 10
        self.assemble_url()
        self.browser.get(self.url)
        self.get_jobs()

    def close_browser(self):
        self.browser.quit()
    

if __name__ == '__main__':
    scraper = IndeedScraper()
    scraper.get_jobs()
    scraper.close_browser()