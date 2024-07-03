from base_class import BaseScraper, JobResult
from urllib.parse import quote_plus
from selenium.webdriver.common.by import By
from selenium.webdriver.remote.webelement import WebElement
import logging

class RadiantScraper(BaseScraper):
    def __init__(self, radius=10, location='El Segundo, CA', start=0, logger=None):
        base_url = 'https://boards.greenhouse.io/radiant'
        super().__init__(base_url, radius, location, start, logger)

    def assemble_url(self):
        self.url = 'https://boards.greenhouse.io/radiant'
        self.logger.info(f"Assembled URL: {self.url}")

    def next_page(self, job_results):
        return super().next_page(job_results)

    def get_jobs(self) -> list:
        job_results = []
        try:
            links = self.browser.find_elements(By.TAG_NAME, 'a')
            job_links = [link.get_attribute('href') for link in links if 'jobs' in link.get_attribute('href')]
        except Exception as e:
            self.logger.error(f"Error: {e}")
            return job_results

        for href in job_links:
            self.random_delay(0.1, 0.5)
            try:
                self.browser.get(href)

                title_element = self.browser.find_element(By.TAG_NAME, 'h1')
                title = title_element.text if title_element else "N/A"

                # Extracting the text content of the sections
                responsibilities = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div[1]/div/div/div[3]/ul[1]'))
                qualifications = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div[1]/div/div/div[3]/ul[2]'))
                desired_qualifications = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div[1]/div/div/div[3]/ul[3]'))

                # Combine qualifications and desired qualifications
                all_qualifications = qualifications + desired_qualifications

                job_result = JobResult(
                    company='Radiant',
                    title=title,
                    industry="N/A",
                    responsibilities=responsibilities,
                    qualifications=all_qualifications,
                    other=["N/A"]
                )
                job_results.append(job_result)

                # Navigate back to the main jobs page
                self.browser.back()

            except Exception as e:
                self.logger.error(f"Error: {e}")

        self.logger.info(f"Found {len(job_results)} jobs")
        return job_results
    
    def extract_children_span_text(self, element: WebElement):
        text = []
        try:
            children = element.find_elements(By.TAG_NAME, 'span')
            if not children:
                children = element.find_elements(By.TAG_NAME, 'li')

            for child in children:
                text.append(child.text)
            return [text for text in text if text != '']
        except Exception as e:
            self.logger.error(f"Span parsing error: {e}")
            return "N/A"

class MakeRainScraper(BaseScraper):
    def __init__(self, radius=10, location='El Segundo, CA', start=0, logger=None):
        base_url = 'https://jobs.lever.co/make-rain'
        super().__init__(base_url, radius, location, start, logger)

    def assemble_url(self):
        self.url = 'https://jobs.lever.co/make-rain'
        self.logger.info(f"Assembled URL: {self.url}")

    def next_page(self, job_results):
        return super().next_page(job_results)

    def get_jobs(self) -> list:
        job_results = []
        try:
            links = self.browser.find_elements(By.TAG_NAME, 'a')
            job_links = [link.get_attribute('href') for link in links if 'posting-btn-submit' in link.get_attribute('class')]
        except Exception as e:
            self.logger.error(f"Sub link Error: {e}")
            return job_results

        for href in job_links:
            self.random_delay(0.1, 0.5)
            self.browser.get(href)

            title_element = self.browser.find_element(By.TAG_NAME, 'h2')
            title = title_element.text if title_element else "N/A"

            # Extracting the text content of the sections
            # TODO: Fix all of this

            responsibilities = []
            try:
                responsibilities = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div[2]/div/div[2]/div[2]/div/ul/ul'))
            except Exception as e:
                self.logger.error(f"responsibilities Error: {e}")

            qualifications = []
            try:
                qualifications = self.browser.find_element(By.XPATH, '/html/body/div[2]/div/div[2]/div[1]/div[21]/span').text.split(', ')
            except Exception as e:
                self.logger.error(f"qualifications Error: {e}")

            desired_qualifications = []
            try:
                desired_qualifications = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div[2]/div/div[2]/div[3]/div/ul/ul'))
            except Exception as e:
                self.logger.error(f"desired_qualifications Error: {e}")

            # Combine qualifications and desired qualifications
            all_qualifications = desired_qualifications + [qual.replace('or', '') for qual in qualifications]

            job_result = JobResult(
                company='MakeRain',
                title=title,
                industry="N/A",
                responsibilities=responsibilities,
                qualifications=all_qualifications,
                other=["N/A"]
            )
            job_results.append(job_result)

            # Navigate back to the main jobs page
            self.browser.back()

        self.logger.info(f"Found {len(job_results)} jobs")
        return job_results
    
    def extract_children_span_text(self, element: WebElement):
        text = []
        try:
            children = element.find_elements(By.TAG_NAME, 'span')
            if not children:
                children = element.find_elements(By.TAG_NAME, 'li')

            for child in children:
                text.append(child.text)
            return [text for text in text if text != '']
        except Exception as e:
            self.logger.error(f"Span parsing error: {e}")
            return "N/A"

# uses wellfound.com which has bot detection and blocks the scraper (against TOS)
class BoundaryDigitalScraper(BaseScraper):
    def __init__(self, radius=10, location='El Segundo, CA', start=0, logger=None):
        base_url = 'https://wellfound.com/company/boundary-digital/jobs'
        super().__init__(base_url, radius, location, start, logger)

    def assemble_url(self):
        self.url = 'https://wellfound.com/company/boundary-digital/jobs'
        self.logger.info(f"Assembled URL: {self.url}")

    def next_page(self, job_results):
        return super().next_page(job_results)

    def get_jobs(self) -> list:
        job_results = []
        try:
            links = self.browser.find_elements(By.TAG_NAME, 'a')
            job_links = [link.get_attribute('href') for link in links if '/jobs/' in link.get_attribute('href')]
        except Exception as e:
            self.logger.error(f"Sub link Error: {e}")
            return job_results

        for href in job_links:
            self.random_delay(2, 2.5)
            self.browser.get(href)

            title_element = self.browser.find_element(By.TAG_NAME, 'h1')
            title = title_element.text if title_element else "N/A"

            # Extracting the text content of the sections
            # TODO: Fix all of this

            responsibilities = []
            try:
                responsibilities = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div/div/div/div[1]/div[4]/div[1]/div/ul[2]'))
            except Exception as e:
                self.logger.error(f"responsibilities Error: {e}")

            qualifications = []
            try:
                qualifications = self.browser.find_element(By.XPATH, '/html/body/div/div/div/div[1]/div[4]/div[1]/div/ul[3]')
            except Exception as e:
                self.logger.error(f"qualifications Error: {e}")

            desired_qualifications = []
            try:
                desired_qualifications = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div/div/div/div[1]/div[4]/div[1]/div/ul[4]'))
            except Exception as e:
                self.logger.error(f"desired_qualifications Error: {e}")

            # Combine qualifications and desired qualifications
            all_qualifications = desired_qualifications + qualifications

            job_result = JobResult(
                company='Boundary Digital',
                title=title,
                industry="N/A",
                responsibilities=responsibilities,
                qualifications=all_qualifications,
                other=["N/A"]
            )
            job_results.append(job_result)

            # Navigate back to the main jobs page
            self.browser.back()

        self.logger.info(f"Found {len(job_results)} jobs")
        return job_results
    
    def extract_children_span_text(self, element: WebElement):
        text = []
        try:
            children = element.find_elements(By.TAG_NAME, 'span')
            if not children:
                children = element.find_elements(By.TAG_NAME, 'li')

            for child in children:
                text.append(child.text)
            return [text for text in text if text != '']
        except Exception as e:
            self.logger.error(f"Span parsing error: {e}")
            return "N/A"
        
class LATimesScraper(BaseScraper):
    def __init__(self, radius=10, location='El Segundo, CA', start=0, logger=None):
        base_url = 'https://us241.dayforcehcm.com/CandidatePortal/en-US/nantmedia'
        super().__init__(base_url, radius, location, start, logger)

    def assemble_url(self):
        self.url = 'https://us241.dayforcehcm.com/CandidatePortal/en-US/nantmedia'
        self.logger.info(f"Assembled URL: {self.url}")

    def next_page(self, job_results):
        return super().next_page(job_results)

    def get_jobs(self) -> list:
        job_results = []
        try:
            links = self.browser.find_elements(By.TAG_NAME, 'a')
            job_links = [link.get_attribute('href') for link in links if 'Posting' in link.get_attribute('href')]
        except Exception as e:
            self.logger.error(f"Sub link Error: {e}")
            return job_results

        for href in job_links:
            self.random_delay(2, 2.5)
            self.browser.get(href)
            print(self.browser.current_url)

            title_element = self.browser.find_element(By.TAG_NAME, 'h1')
            title = title_element.text if title_element else "N/A"

            # Extracting the text content of the sections
            # TODO: Fix all of this

            responsibilities = []
            try:
                responsibilities = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div[1]/div/div[1]/main/div/div[1]/div[2]/div[5]/ul[1]'))
            except Exception as e:
                self.logger.error(f"responsibilities Error: {e}")

            qualifications = []
            try:
                qualifications = self.browser.find_element(By.XPATH, '/html/body/div[1]/div/div[1]/main/div/div[1]/div[2]/div[5]/ul[2]')
            except Exception as e:
                self.logger.error(f"qualifications Error: {e}")

            desired_qualifications = []
            try:
                desired_qualifications = self.extract_children_span_text(self.browser.find_element(By.XPATH, '/html/body/div[1]/div/div[1]/main/div/div[1]/div[2]/div[5]/ul[3]'))
            except Exception as e:
                self.logger.error(f"desired_qualifications Error: {e}")

            # Combine qualifications and desired qualifications
            all_qualifications = desired_qualifications + qualifications

            job_result = JobResult(
                company='LA Times',
                title=title,
                industry="N/A",
                responsibilities=responsibilities,
                qualifications=all_qualifications,
                other=["N/A"]
            )
            job_results.append(job_result)

            self.act_human()
            # Navigate back to the main jobs page
            self.browser.back()

        self.logger.info(f"Found {len(job_results)} jobs")
        return job_results
    
    def extract_children_span_text(self, element: WebElement):
        text = []
        try:
            children = element.find_elements(By.TAG_NAME, 'span')
            if not children:
                children = element.find_elements(By.TAG_NAME, 'li')

            for child in children:
                text.append(child.text)
            return [text for text in text if text != '']
        except Exception as e:
            self.logger.error(f"Span parsing error: {e}")
            return "N/A"

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)

    job_results = []
    # radiantScraper = RadiantScraper(logger=logger)
    # job_results = radiantScraper.get_jobs()
    # radiantScraper.close_browser()

    # makeRainScraper = MakeRainScraper(logger=logger)
    # job_results += makeRainScraper.get_jobs()
    # makeRainScraper.close_browser()

    # boundaryDigitalScraper = BoundaryDigitalScraper(logger=logger)
    # job_results += boundaryDigitalScraper.get_jobs()
    # boundaryDigitalScraper.close_browser()

    laTimesScraper = LATimesScraper(logger=logger)
    job_results += laTimesScraper.get_jobs()
    laTimesScraper.close_browser()

    for job in job_results:
        print(job)