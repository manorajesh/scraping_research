import time
import random
import logging
from selenium import webdriver
from selenium.webdriver.firefox.options import Options
from selenium.webdriver.common.action_chains import ActionChains
from fake_useragent import UserAgent
from abc import ABC, abstractmethod

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

class BaseScraper(ABC):
    def __init__(self, base_url, radius=10, location='El Segundo, CA', start=0, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.radius = radius
        self.location = location
        self.start = start
        self.user_agent = UserAgent()
        self.base_url = base_url
        self.assemble_url()
        self.browser = self.init_browser()
        self.browser.get(self.url)

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

    def act_human(self):
        self.logger.info("Simulating human interaction")
        actions = ActionChains(self.browser)
        for _ in range(random.randint(1, 5)):
            x_offset = random.randint(0, 100)
            y_offset = random.randint(0, 100)
            actions.move_by_offset(x_offset, y_offset).perform()
            self.random_delay(0.1, 0.3)
        
        scroll_script = "window.scrollBy({}, {})".format(random.randint(-300, 300), random.randint(-300, 300))
        self.browser.execute_script(scroll_script)
        self.random_delay(0.5, 1)

    @abstractmethod
    def assemble_url(self):
        pass

    @abstractmethod
    def get_jobs(self):
        pass

    @abstractmethod
    def next_page(self, job_results):
        pass

    def close_browser(self):
        self.logger.info("Closing browser")
        self.browser.quit()

