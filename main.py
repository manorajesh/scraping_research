import time
import random
import logging
from selenium import webdriver
from selenium.webdriver.firefox.options import Options
from urllib.parse import quote_plus
from fake_useragent import UserAgent

class IndeedScraper:
    def __init__(self, radius=10, location='El Segundo, CA', start=0, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.radius = radius
        self.location = location
        self.start = start
        self.user_agent = UserAgent()
        self.assemble_url()
        self.browser = self.init_browser()
        self.browser.get(self.url)

        if '<body>Forbidden</body>' in self.browser.page_source:
            raise Exception("IP address has been blocked by Indeed. Try again later.")

    def init_browser(self):
        options = Options()
        options.set_preference("general.useragent.override", self.get_random_user_agent())
        self.logger.info("Initializing browser")
        return webdriver.Firefox(options=options)

    def get_random_user_agent(self):
        user_agent = self.user_agent.random
        while 'bot' in user_agent or 'spider' in user_agent:
            user_agent = self.user_agent.random

        self.logger.info(f"Using user agent: {user_agent}")
        return user_agent

    def random_delay(self, min_seconds=1, max_seconds=5):
        self.logger.info(f"Sleeping for {min_seconds} to {max_seconds} seconds")
        time.sleep(random.uniform(min_seconds, max_seconds))

    def assemble_url(self):
        self.url = f'https://www.indeed.com/jobs?q=&l={quote_plus(self.location)}&radius={self.radius}&sort=date&start={self.start}'
        self.logger.info(f"Assembled URL: {self.url}")

    def get_jobs(self) -> None:
        self.random_delay()
        try:
            div = self.browser.find_element('id', 'mosaic-provider-jobcards')
            lis = div.find_elements('tag name', 'li')
        except Exception as e:
            self.logger.error(f"Error: {e}")
            return

        # Get the title of each job in list
        for li in lis:
            self.random_delay()
            try:
                title = li.find_element('tag name', 'span').text
                if title != '':
                    print(title)
            except Exception as e:
                self.logger.error(f"Error: {e}")
                title = 'N/A'

        self.next_page()

    def next_page(self):
        self.logger.info(f"Scraped {self.start} jobs. Next page.")
        self.start += 10
        self.assemble_url()
        self.browser.get(self.url)
        self.get_jobs()

    def close_browser(self):
        self.logger.info("Closing browser")
        self.browser.quit()

logger = logging.getLogger(__name__)
if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    scraper = IndeedScraper(logger=logger)
    scraper.get_jobs()
    scraper.close_browser()
