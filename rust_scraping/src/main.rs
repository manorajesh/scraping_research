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
use scraper_impls::GreenhouseScraper;

const FILENAME: &str = "job_results.csv";

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
    let client = Client::new(env::var("OPENAI_API_KEY")?.to_string());
    let result = client.chat_completion(req)?;
    info!("Received response from OpenAI API");
    info!("Time taken for OpenAI API request: {} ms", openai_start.elapsed().as_millis());

    let content = result.choices[0].message.content.clone().expect("No content found");
    let content = content.trim_start_matches("```json").trim_end_matches("```");
    info!("Parsed OpenAI response");

    Ok(content.to_string())
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

    let scraper = GreenhouseScraper::new();
    let job_links = runtime.block_on(
        scraper.fetch_job_links("https://boards.greenhouse.io/radiant")
    )?;

    let job_results: Arc<Mutex<Vec<JobResult>>> = Arc::new(Mutex::new(vec![]));

    job_links.par_iter().for_each(|job_link| {
        let progress = multi.add(ProgressBar::new(3));
        progress.set_style(style.clone());
        progress.set_prefix(job_link.clone());
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
    info!("Total time taken: {:.2} s", total_duration);

    Ok(())
}
