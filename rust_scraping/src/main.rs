use openai_api_rs::v1::api::Client;
use openai_api_rs::v1::chat_completion::{ self, ChatCompletionRequest };
use openai_api_rs::v1::common::GPT3_5_TURBO;
use std::env;
use serde_json;
use reqwest;
use scraper::{ Html, Selector };

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::new(env::var("OPENAI_API_KEY").unwrap().to_string());

    let response = reqwest::blocking::get("https://boards.greenhouse.io/radiant")?.text()?;
    let document = Html::parse_document(&response);
    let selector = Selector::parse("a")?;
    let links = document.select(&selector);

    let mut job_links: Vec<String> = vec![];
    for link in links {
        let href = link.value().attr("href").unwrap_or_default();
        if href.contains("/jobs/") {
            job_links.push(href.to_string());
        }
        println!("{}", href);
    }

    for job_link in job_links {
        let response = reqwest::blocking
            ::get(&format!("https://boards.greenhouse.io{}", job_link))?
            .text()?;
        let document = Html::parse_document(&response);
        let selector = Selector::parse("#content p, #content ul")?;
        let text_elems = document.select(&selector);

        let mut job_description = String::new();
        for elem in text_elems {
            job_description.push_str(&elem.text().collect::<String>());
            job_description.push_str("\n");
        }

        println!("{}", job_description);
    }

    Ok(())
}
