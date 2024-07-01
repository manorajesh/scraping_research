import time
import random
import logging
from selenium import webdriver
from selenium.webdriver.firefox.options import Options
from urllib.parse import quote_plus
from fake_useragent import UserAgent

class JobResult:
    def __init__(self, platform, title, industry, responsibilities, qualifications, other):
        self.platform = platform
        self.title = title
        self.industry = industry
        self.responsibilities = responsibilities
        self.qualifications = qualifications
        self.other = other

    def __repr__(self):
        return f"JobResult(platform={self.platform}, title={self.title}, industry={self.industry})"


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

    def get_jobs(self) -> list:
        job_results = []
        self.random_delay()
        try:
            div = self.browser.find_element('id', 'mosaic-provider-jobcards')
            lis = div.find_elements('tag name', 'li')
        except Exception as e:
            self.logger.error(f"Error: {e}")
            return job_results

        # Get job info
        for li in lis:
            self.random_delay()
            try:
                title_element = li.find_element('tag name', 'span')
                title = title_element.text if title_element else 'N/A'

                # placeholders
                industry = "N/A"
                responsibilities = "N/A"
                qualifications = "N/A"
                other = "N/A"

                job_result = JobResult(
                    platform='Indeed',
                    title=title,
                    industry=industry,
                    responsibilities=responsibilities,
                    qualifications=qualifications,
                    other=other
                )
                job_results.append(job_result)
            except Exception as e:
                self.logger.error(f"Error: {e}")

        self.logger.info(f"Found {len(job_results)} jobs")
        self.next_page(job_results)
        return job_results

    def next_page(self, job_results):
        self.logger.info(f"Next page.")
        self.start += 10
        self.assemble_url()
        self.browser.get(self.url)
        job_results.extend(self.get_jobs())

    def close_browser(self):
        self.logger.info("Closing browser")
        self.browser.quit()

logger = logging.getLogger(__name__)
if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    scraper = IndeedScraper(logger=logger)
    jobs = scraper.get_jobs()
    scraper.close_browser()
    for job in jobs:
        print(job)
