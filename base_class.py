import time
import random
import logging
from selenium import webdriver
from selenium.webdriver.firefox.options import Options
from selenium.webdriver.common.action_chains import ActionChains
from fake_useragent import UserAgent
from abc import ABC, abstractmethod
import ollama
import csv


class JobResult:
    def __init__(
        self,
        company: str,
        title: str,
        industry: str,
        responsibilities: list[str],
        qualifications: list[str],
        location: str,
        other: list[str],
    ):
        self.company = company
        self.title = title
        self.industry = industry
        self.responsibilities = responsibilities
        self.qualifications = qualifications
        self.location = location
        self.other = other

    def from_llm_output(
        title: str, company: str, location: str, llm_output: str
    ) -> "JobResult":
        data = BaseScraper.parse_llm_output(llm_output)
        return JobResult(
            company=company,
            title=title,
            industry=data["industry"],
            responsibilities=data["responsibilities"],
            qualifications=data["qualifications"],
            location=location,
            other=["N/A"],
        )

    def as_csv(self, filename: str):
        # Convert lists to bulleted strings
        def list_to_bullets(lst):
            return "\n".join(
                [
                    f"• {item}"
                    for item in lst
                    if item != "" and item != "." and item != " " and item != "N/A"
                ]
            )

        with open(filename, "a", newline="", encoding="utf-8") as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow(
                [
                    self.company,
                    self.title,
                    self.industry,
                    list_to_bullets(self.responsibilities),
                    list_to_bullets(self.qualifications),
                    self.location,
                    list_to_bullets(self.other),
                ]
            )

    def __repr__(self):
        return f"{self.company} - {self.title} - {self.industry} @ {self.location}: {len(self.responsibilities)} responsibilities, {len(self.qualifications)} qualifications, {len(self.other)} other"


class BaseScraper(ABC):
    def __init__(self, base_url, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.user_agent = UserAgent()
        self.base_url = base_url
        self.assemble_url()
        self.browser = self.init_browser()
        self.browser.get(self.base_url)

    def init_browser(self):
        options = Options()
        options.set_preference(
            "general.useragent.override", self.get_random_user_agent()
        )
        self.logger.info("Initializing browser")
        return webdriver.Firefox(options=options)

    def get_random_user_agent(self):
        user_agent = self.user_agent.random
        while "bot" in user_agent or "spider" in user_agent:
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

        scroll_script = "window.scrollBy({}, {})".format(
            random.randint(-300, 300), random.randint(-300, 300)
        )
        self.browser.execute_script(scroll_script)
        self.random_delay(0.5, 1)

    def llm_extract(self, job_listing: str, model="gemma:2b", custom_prompt=None):
        prompt = "Can you give me the industry (string), responsibilities (array of strings), and qualifications (array of strings) of this job listing. Find as many responsibilities and qualifications as it says:"
        if custom_prompt:
            prompt = custom_prompt

        now = time.time()
        self.logger.info(f"Processing job listing with LLM")
        response = ollama.generate(
            model=model, prompt=prompt + job_listing, options={"temperature": 0.2}
        )
        self.logger.info(f"LLM processing took {time.time() - now} seconds")

        return response["response"]

    def parse_llm_output(output: str):
        data = {"industry": "", "responsibilities": [], "qualifications": []}

        sections = output.strip().split("\n")

        current_section = ""
        for section in sections:
            if section.startswith("**Industry:**"):
                data["industry"] = section.replace("**Industry:**", "").strip()
            elif section.startswith("**Responsibilities:**"):
                current_section = "responsibilities"
            elif section.startswith("**Qualifications:**"):
                current_section = "qualifications"
            else:
                if current_section and section != "":
                    data[current_section].append(
                        section.strip("* ").strip("- ").strip()
                    )

        return data

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
