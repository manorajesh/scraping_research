use std::{ sync::{ Arc, Mutex }, time::Instant };
use log::{ info, error };
use tokio::runtime::Runtime;
use rayon::prelude::*;
use std::error::Error;
use std::env;

use openai_api_rs::v1::api::Client;
use openai_api_rs::v1::chat_completion::{ self, ChatCompletionRequest };
use openai_api_rs::v1::common::GPT3_5_TURBO;

mod jobresult;
use jobresult::JobResult;

mod scraper_trait;
use scraper_trait::Scraper;

mod scraper_impls;
use scraper_impls::GreenhouseScraper;

pub async fn get_openai_response(description: &str) -> Result<String, Box<dyn Error>> {
    let req = ChatCompletionRequest::new(
        GPT3_5_TURBO.to_string(),
        vec![chat_completion::ChatCompletionMessage {
            role: chat_completion::MessageRole::user,
            content: chat_completion::Content::Text(
                format!("Can you give me the industry (string), responsibilities (array of strings), and qualifications (array of strings) of this job listing in valid JSON. Find as many responsibilities and qualifications as it says:{}", description)
            ),
            name: None,
        }]
    );
    let openai_start = Instant::now();
    let client = Client::new(env::var("OPENAI_API_KEY").unwrap().to_string());
    let result = client.chat_completion(req).unwrap();
    info!("Received response from OpenAI API");
    info!("Time taken for OpenAI API request: {} ms", openai_start.elapsed().as_millis());

    let content = result.choices[0].message.content.clone().expect("No content found");
    let content = content.trim_start_matches("```json").trim_end_matches("```");
    info!("Parsed OpenAI response");

    Ok(content.to_string())
}

fn main() -> Result<(), Box<dyn Error>> {
    tracing_subscriber::fmt().with_thread_ids(true).pretty().init();

    let start_time = std::time::Instant::now();
    info!("Program started");

    let runtime = Runtime::new()?;

    let scraper = GreenhouseScraper::new();
    let job_links = runtime.block_on(
        scraper.fetch_job_links("https://boards.greenhouse.io/radiant")
    )?;

    let job_results: Arc<Mutex<Vec<JobResult>>> = Arc::new(Mutex::new(vec![]));

    job_links.par_iter().for_each(|job_link| {
        let scraper = scraper.clone();
        let job_results = Arc::clone(&job_results);

        let job_result = runtime.block_on(async {
            let job_details = scraper.fetch_job_details(job_link).await?;
            let job_result = scraper.parse_job_details(&job_details).await?;
            Ok(job_result) as Result<JobResult, Box<dyn Error>>
        });

        match job_result {
            Ok(job_result) => {
                info!("Created job result object: {}", job_result);
                job_results.lock().unwrap().push(job_result);
            }
            Err(e) => {
                error!("Error processing job link {}: {}", job_link, e);
            }
        }
    });

    let job_results = job_results.lock().unwrap();
    info!("Total number of job results: {}", job_results.len());

    let total_duration = start_time.elapsed().as_secs_f64();
    info!("Program ended");
    info!("Total time taken: {:.2} s", total_duration);

    Ok(())
}
