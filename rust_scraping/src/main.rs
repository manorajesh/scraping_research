use std::{ fs::OpenOptions, sync::{ Arc, Mutex }, time::Instant };
use indicatif::{ MultiProgress, ProgressBar, ProgressStyle };
use indicatif_log_bridge::LogWrapper;
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
use scraper_impls::{ GenericScraper };

const FILENAME: &str = "jobs.csv";
const PROMPT_TOKEN_PRICE: f64 = 0.0000005;
const COMPLETION_TOKEN_PRICE: f64 = 0.0000015;

pub async fn get_openai_response(
    prompt: Option<&str>,
    description: &str
) -> Result<(String, f64), Box<dyn Error>> {
    let prompt = prompt.unwrap_or(
        "Can you give me the industry (string), responsibilities (array of strings), and qualifications (array of strings) of this job listing in valid JSON. Find as many responsibilities and qualifications as it says:"
    );
    let req = ChatCompletionRequest::new(
        GPT3_5_TURBO.to_string(),
        vec![chat_completion::ChatCompletionMessage {
            role: chat_completion::MessageRole::user,
            content: chat_completion::Content::Text(format!("{}{}", prompt, description)),
            name: None,
        }]
    );
    let openai_start = Instant::now();
    let client = Client::new(env::var("OPENAI_API_KEY")?.to_string());
    let result = client.chat_completion(req)?;
    info!("Time taken for OpenAI API request: {} ms", openai_start.elapsed().as_millis());

    // price calculation
    let prompt_tokens = result.usage.prompt_tokens;
    let completion_tokens = result.usage.completion_tokens;
    let prompt_price = (prompt_tokens as f64) * PROMPT_TOKEN_PRICE;
    let completion_price = (completion_tokens as f64) * COMPLETION_TOKEN_PRICE;
    info!(
        "Received response from OpenAI API: {} tokens used @ ${}",
        prompt_tokens + completion_tokens,
        prompt_price + completion_price
    );

    let content = result.choices[0].message.content.clone().expect("No content found");
    let content = content.trim_start_matches("```json").trim_end_matches("```");
    info!("Parsed OpenAI response");

    Ok((content.to_string(), prompt_price + completion_price))
}

fn ellipsize(s: &str, max_length: usize) -> String {
    if s.chars().count() > max_length {
        let truncated: String = s
            .chars()
            .take(max_length - 3)
            .collect();
        format!("{}...", truncated)
    } else {
        s.to_string()
    }
}

fn main() -> Result<(), Box<dyn Error>> {
    // let logger = tracing_subscriber::fmt().with_thread_ids(true).pretty().finish();
    let logger = env_logger::Builder
        ::from_env(env_logger::Env::default().default_filter_or("info"))
        .build();

    let style = ProgressStyle::default_bar()
        .template("[{elapsed_precise}] {prefix}: {bar:40.cyan/blue} {pos:>7}/{len:7} {msg}")?
        .progress_chars("##-");

    let multi = MultiProgress::new();

    LogWrapper::new(multi.clone(), logger).try_init()?;

    let start_time = std::time::Instant::now();
    info!("Initializing");

    let runtime = Runtime::new()?;

    // let scraper = GreenhouseScraper::new();
    let scraper = GenericScraper::new();
    let (job_links, link_retriv_cost) = runtime.block_on(
        // scraper.fetch_job_links("https://boards.greenhouse.io/radiant")
        scraper.fetch_multiple_job_links(
            vec![
                "https://boards.greenhouse.io/radiant",
                "https://jobs.lever.co/make-rain",
                "https://us241.dayforcehcm.com/CandidatePortal/en-US/nantmedia",
                "https://boards.greenhouse.io/goguardian",
                "https://radlink.com/careers/",
                "https://jobs.lever.co/ablspacesystems",
                "https://jobs.mattel.com/en/search-jobs/El%20Segundo%2C%20CA/",
                "https://www.spacex.com/careers/jobs?location=hawthorne%252C%2520ca",
                "https://www.disneycareers.com/en/search-jobs/Los%20Angeles%2C%20CA",
                "https://jobs.boeing.com/search-jobs/El%20Segundo%2C%20CA/",
                "https://jobs.netflix.com/search"
            ]
        )
    )?;

    let job_results: Arc<Mutex<Vec<JobResult>>> = Arc::new(Mutex::new(vec![]));

    job_links.par_iter().for_each(|job_link| {
        let progress = multi.add(ProgressBar::new(3));
        progress.set_style(style.clone());
        progress.set_prefix(ellipsize(job_link, 40));
        progress.set_message("Processing job link");

        let scraper = scraper.clone();
        let job_results = Arc::clone(&job_results);
        progress.inc(1);
        progress.set_message("Fetching job details");

        let job_result = runtime.block_on(async {
            let job_details = scraper.fetch_job_details(job_link).await?;
            progress.inc(1);
            progress.set_message("Parsing job details");
            let job_result = scraper.parse_job_details(&job_details).await?;
            progress.inc(1);
            Ok(job_result) as Result<JobResult, Box<dyn Error>>
        });

        progress.finish();
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

    // write to file
    let mut file = OpenOptions::new()
        .append(true)
        .create(true)
        .open(FILENAME)
        .expect("Unable to open file");

    let progress = multi.add(ProgressBar::new(job_results.len() as u64));
    progress.set_style(style.clone());
    progress.set_prefix("Writing to file");
    for job_result in job_results.iter() {
        job_result.as_csv(&mut file);
        progress.inc(1);
    }
    info!("Wrote job results to file");

    let total_duration = start_time.elapsed().as_secs_f64();
    let total_cost: f64 = job_results
        .iter()
        .map(|job_result| job_result.api_cost)
        .sum();
    info!("Total time taken: {:.2} s @ ${:.4}", total_duration, total_cost + link_retriv_cost);

    Ok(())
}
