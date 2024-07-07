use std::{ collections::HashMap, fs::File };
use serde_json;
use std::io::Write;

#[derive(Debug, Clone)]
pub struct JobResult {
    company: String,
    title: String,
    industry: String,
    responsibilities: Vec<String>,
    qualifications: Vec<String>,
    location: String,
    other: Vec<String>,
    pub api_cost: f64,
}

impl JobResult {
    pub fn new(
        company: String,
        title: String,
        industry: String,
        responsibilities: Vec<String>,
        qualifications: Vec<String>,
        location: String,
        other: Vec<String>
    ) -> Self {
        JobResult {
            company,
            title,
            industry,
            responsibilities,
            qualifications,
            location,
            other,
            api_cost: 0.0,
        }
    }

    pub fn from_json(
        company: String,
        title: String,
        location: String,
        json_str: &str,
        api_cost: f64
    ) -> Result<Self, serde_json::Error> {
        let data: HashMap<String, serde_json::Value> = serde_json::from_str(json_str)?;

        let industry = data
            .get("industry")
            .and_then(|v| v.as_str())
            .unwrap_or_default()
            .to_string();
        let responsibilities = data
            .get("responsibilities")
            .and_then(|v| v.as_array())
            .unwrap_or(&vec![])
            .iter()
            .filter_map(|v| v.as_str().map(String::from))
            .collect();
        let qualifications = data
            .get("qualifications")
            .and_then(|v| v.as_array())
            .unwrap_or(&vec![])
            .iter()
            .filter_map(|v| v.as_str().map(String::from))
            .collect();

        Ok(JobResult {
            company,
            title,
            industry,
            responsibilities,
            qualifications,
            location,
            other: vec!["N/A".to_string()],
            api_cost,
        })
    }

    pub fn as_csv(&self, file: &mut File) {
        fn list_to_bullets(lst: &Vec<String>) -> String {
            lst.iter()
                .filter(|item| !item.is_empty() && item != &"." && item != &" " && item != &"N/A")
                .map(|item| format!("• {}", item))
                .collect::<Vec<String>>()
                .join("\n")
        }

        let responsibilities = list_to_bullets(&self.responsibilities);
        let qualifications = list_to_bullets(&self.qualifications);
        let other = list_to_bullets(&self.other);

        writeln!(
            file,
            "{},{},{},{},{},{},{}",
            self.company,
            self.title,
            self.industry,
            responsibilities,
            qualifications,
            self.location,
            other
        ).expect("Unable to write to file");
    }

    pub fn as_string(&self) -> String {
        format!(
            "{} - {} - {} @ {}: {} Responsibilities, {} Qualifications, {} Other",
            self.company,
            self.title,
            self.industry,
            self.location,
            self.responsibilities.len(),
            self.qualifications.len(),
            self.other.len()
        )
    }
}

impl std::fmt::Display for JobResult {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{}", self.as_string())
    }
}
