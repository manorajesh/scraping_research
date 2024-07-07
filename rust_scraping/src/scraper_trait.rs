use std::error::Error;

use crate::jobresult::JobResult;

pub trait Scraper {
    async fn fetch_job_links(&self, url: &str) -> Result<(Vec<String>, f64), Box<dyn Error>>;
    async fn fetch_job_details(&self, job_link: &str) -> Result<String, Box<dyn Error>>;
    async fn parse_job_details(&self, job_details: &str) -> Result<JobResult, Box<dyn Error>>;
}
