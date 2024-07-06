use log::{ info, error };
use openai_api_rs::v1::api::Client;
use openai_api_rs::v1::chat_completion::{ self, ChatCompletionRequest };
use openai_api_rs::v1::common::GPT3_5_TURBO;
use reqwest;
use scraper::{ Html, Selector };
use serde_json;
use std::env;
use std::time::Instant;

mod jobresult;
use jobresult::JobResult;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    env_logger::init();

    let start_time = Instant::now();
    info!("Program started");

    let client_start = Instant::now();
    let client = Client::new(env::var("OPENAI_API_KEY").unwrap().to_string());
    info!("Client initialized");
    info!("Time taken to initialize client: {} ms", client_start.elapsed().as_millis());

    let response_start = Instant::now();
    let response = reqwest::blocking::get("https://boards.greenhouse.io/radiant")?.text()?;
    info!("Fetched job listings page");
    info!("Time taken to fetch job listings page: {} ms", response_start.elapsed().as_millis());

    let document_start = Instant::now();
    let document = Html::parse_document(&response);
    let selector = Selector::parse("a")?;
    let links = document.select(&selector);
    info!("Parsed document and selected links");
    info!("Time taken to parse document: {} ms", document_start.elapsed().as_millis());

    let mut job_links: Vec<String> = vec![];
    for link in links {
        let href = link.value().attr("href").unwrap_or_default();
        if href.contains("/jobs/") {
            job_links.push(href.to_string());
        }
        println!("{}", href);
    }
    info!("Collected job links");
    info!("Number of job links found: {}", job_links.len());

    let mut job_results: Vec<JobResult> = vec![];

    for job_link in job_links {
        let job_response_start = Instant::now();
        let response = reqwest::blocking
            ::get(&format!("https://boards.greenhouse.io{}", job_link))?
            .text()?;
        info!("Fetched job details page");
        info!(
            "Time taken to fetch job details page: {} ms",
            job_response_start.elapsed().as_millis()
        );

        let job_document_start = Instant::now();
        let document = Html::parse_document(&response);
        let selector = Selector::parse("#content p, #content ul")?;
        let text_elems = document.select(&selector);
        info!("Parsed job details document");
        info!(
            "Time taken to parse job details document: {} ms",
            job_document_start.elapsed().as_millis()
        );

        let mut job_description = String::new();
        for elem in text_elems {
            job_description.push_str(&elem.text().collect::<String>());
            job_description.push_str("\n");
        }
        info!("Collected job description");

        let req = ChatCompletionRequest::new(
            GPT3_5_TURBO.to_string(),
            vec![chat_completion::ChatCompletionMessage {
                role: chat_completion::MessageRole::user,
                content: chat_completion::Content::Text(
                    format!("Can you give me the industry (string), responsibilities (array of strings), and qualifications (array of strings) of this job listing in valid JSON. Find as many responsibilities and qualifications as it says:{}", job_description)
                ),
                name: None,
            }]
        );

        let openai_start = Instant::now();
        let result = client.chat_completion(req)?;
        info!("Received response from OpenAI API");
        info!("Time taken for OpenAI API request: {} ms", openai_start.elapsed().as_millis());

        let content = result.choices[0].message.content.clone().expect("No content found");
        let content = content.trim_start_matches("```json").trim_end_matches("```");
        let job_listing: serde_json::Value = serde_json::from_str(content)?;
        info!("Parsed OpenAI response");
        println!("{}", serde_json::to_string_pretty(&job_listing)?);

        let job_result = JobResult::from_json(
            "Radiant".to_string(),
            "Test".to_string(),
            "Remote".to_string(),
            content
        )?;
        println!("{}", job_result);
        job_results.push(job_result);
        info!("Created job result object");
    }

    info!("Total number of job results: {}", job_results.len());

    let total_duration = start_time.elapsed().as_millis();
    info!("Program ended");
    info!("Total time taken: {} ms", total_duration);

    Ok(())
}
