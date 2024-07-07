use std::error::Error;

use log::info;
use reqwest::Client;
use scraper::{ Html, Selector };

use crate::{ get_openai_response, jobresult::JobResult, scraper_trait::Scraper };

#[derive(Debug, Clone)]
pub struct GreenhouseScraper {
    client: Client,
}

impl GreenhouseScraper {
    pub fn new() -> Self {
        Self {
            client: Client::new(),
        }
    }
}

impl Scraper for GreenhouseScraper {
    async fn fetch_job_links(&self, url: &str) -> Result<Vec<String>, Box<dyn Error>> {
        let response = self.client.get(url).send().await?.text().await?;
        let document = Html::parse_document(&response);
        let selector = Selector::parse("a")?;
        let links = document.select(&selector);

        let job_links: Vec<String> = links
            .filter_map(|link| {
                let href = link.value().attr("href")?;
                if href.contains("/jobs/") {
                    Some(format!("https://boards.greenhouse.io{}", href))
                } else {
                    None
                }
            })
            .collect();

        Ok(job_links)
    }

    async fn fetch_job_details(&self, job_link: &str) -> Result<String, Box<dyn Error>> {
        let response = self.client.get(job_link).send().await?.text().await?;
        Ok(response)
    }

    async fn parse_job_details(&self, job_details: &str) -> Result<JobResult, Box<dyn Error>> {
        let document = Html::parse_document(&job_details);
        let selector = Selector::parse("#content p, #content ul")?;
        let text_elems = document.select(&selector);

        let mut job_description = String::new();
        for elem in text_elems {
            job_description.push_str(&elem.text().collect::<String>());
            job_description.push_str("\n");
        }

        info!("Job description extracted");

        // Assume you have some method to get the OpenAI response
        let openai_response = get_openai_response(&job_description).await?;

        let job_listing: serde_json::Value = serde_json::from_str(&openai_response)?;
        let job_result = JobResult::from_json(
            "Company Name".to_string(),
            "Job Title".to_string(),
            "Location".to_string(),
            &serde_json::to_string(&job_listing)?
        )?;

        Ok(job_result)
    }
}
