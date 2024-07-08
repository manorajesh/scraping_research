use std::error::Error;

use log::info;
use reqwest::Client;
use scraper::{Html, Selector};
use url::Url;

use crate::{get_openai_response, jobresult::JobResult, scraper_trait::Scraper};

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
    async fn fetch_job_links(&self, url: &str) -> Result<(Vec<String>, f64), Box<dyn Error>> {
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

        Ok((job_links, 0.0))
    }

    async fn fetch_job_details(&self, job_link: &str) -> Result<String, Box<dyn Error>> {
        let response = self.client.get(job_link).send().await?.text().await?;
        Ok(response)
    }

    async fn parse_job_details(&self, job_details: &str) -> Result<JobResult, Box<dyn Error>> {
        let document = Html::parse_document(job_details);
        let company = "Radiant";
        let title = document
            .select(&Selector::parse(".app-title")?)
            .next()
            .expect("No job title found")
            .text()
            .collect::<String>()
            .trim()
            .to_string();
        let location = document
            .select(&Selector::parse(".location")?)
            .next()
            .expect("No job location found")
            .text()
            .collect::<String>()
            .trim()
            .to_string();
        let selector = Selector::parse("#content p, #content ul")?;
        let text_elems = document.select(&selector);

        let mut job_description = String::new();
        for elem in text_elems {
            job_description.push_str(&elem.text().collect::<String>());
            job_description.push('\n');
        }

        info!("Job description extracted");

        // Assume you have some method to get the OpenAI response
        let (openai_response, api_cost) = get_openai_response(None, &job_description).await?;

        let job_listing: serde_json::Value = serde_json::from_str(&openai_response)?;
        let job_result = JobResult::from_impartial_json(
            company.to_string(),
            title,
            location,
            &serde_json::to_string(&job_listing)?,
            api_cost,
        )?;

        Ok(job_result)
    }
}

#[derive(Debug, Clone)]
pub struct GenericScraper {
    client: Client,
}

impl GenericScraper {
    pub fn new() -> Self {
        Self {
            client: Client::new(),
        }
    }
}

impl Scraper for GenericScraper {
    async fn fetch_job_links(&self, url: &str) -> Result<(Vec<String>, f64), Box<dyn Error>> {
        let response = self.client.get(url).send().await?.text().await?;
        let document = Html::parse_document(&response);

        let selector = Selector::parse("a")?;
        let links = document.select(&selector);
        let hrefs = links
            .map(|link| link.value().attr("href").unwrap_or(""))
            .collect::<Vec<&str>>();
        let hrefs = format!("{:?}", hrefs);
        info!("Hrefs extracted: {}", hrefs);

        let (suggested_links, cost) = get_openai_response(
            Some(
                "Of these links, which ones most likely forwards to the details of the particular job. Respond only in a valid JSON array of strings:"
            ),
            &hrefs
        ).await?;
        info!("Suggested links received: {}", suggested_links);

        let json_links: serde_json::Value = serde_json::from_str(&suggested_links)?;
        let job_links: Vec<String> = json_links
            .as_array()
            .ok_or("json_links is not an array")?
            .iter()
            .map(|link| {
                let link_str = link.as_str().ok_or("link is not a string")?;
                if let Ok(parsed_url) = Url::parse(link_str) {
                    Ok(parsed_url.to_string())
                } else {
                    Ok(Url::parse(url)?.join(link_str)?.to_string())
                }
            })
            .collect::<Result<Vec<String>, Box<dyn Error>>>()?;

        info!("Job links extracted: {:?}", job_links);

        Ok((job_links, cost))
    }

    async fn fetch_job_details(&self, job_link: &str) -> Result<String, Box<dyn Error>> {
        let response = self.client.get(job_link).send().await?.text().await?;
        Ok(response)
    }

    async fn parse_job_details(&self, job_details: &str) -> Result<JobResult, Box<dyn Error>> {
        let document = Html::parse_document(job_details);
        let selector = Selector::parse("body")?;

        let text_elems = document.select(&selector);
        let mut job_description = String::new();
        for elem in text_elems {
            job_description.push_str(&elem.text().collect::<String>());
            job_description.push('\n');
        }
        // info!("Job description extracted: {}", job_description);
        info!("Job description extracted");

        let (openai_response, api_cost) = get_openai_response(
            Some(
                "Can you give me the company (string), job title (string), location (string), industry (string), responsibilities (array of strings), and qualifications (array of strings) of this job listing in valid JSON. Find as many responsibilities and qualifications as it says:"
            ),
            &job_description
        ).await?;

        let job_listing: serde_json::Value = serde_json::from_str(&openai_response)?;
        let job_result =
            JobResult::from_complete_json(&serde_json::to_string(&job_listing)?, api_cost)?;

        Ok(job_result)
    }
}
