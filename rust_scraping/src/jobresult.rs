use std::{ collections::HashMap, fs::OpenOptions };
use serde_json;
use std::io::Write;

#[derive(Debug)]
pub struct JobResult {
    company: String,
    title: String,
    industry: String,
    responsibilities: Vec<String>,
    qualifications: Vec<String>,
    location: String,
    other: Vec<String>,
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
        }
    }

    pub fn from_json(
        company: String,
        title: String,
        location: String,
        json_str: &str
    ) -> Result<Self, serde_json::Error> {
        let data: HashMap<String, Vec<String>> = serde_json::from_str(json_str)?;

        Ok(JobResult {
            company,
            title,
            industry: data
                .get("industry")
                .cloned()
                .unwrap_or_default()
                .first()
                .cloned()
                .unwrap_or_default(),
            responsibilities: data.get("responsibilities").cloned().unwrap_or_default(),
            qualifications: data.get("qualifications").cloned().unwrap_or_default(),
            location,
            other: vec!["N/A".to_string()],
        })
    }

    pub fn as_csv(&self, filename: &str) {
        fn list_to_bullets(lst: &Vec<String>) -> String {
            lst.iter()
                .filter(|item| !item.is_empty() && item != &"." && item != &" " && item != &"N/A")
                .map(|item| format!("• {}", item))
                .collect::<Vec<String>>()
                .join("\n")
        }

        let mut file = OpenOptions::new()
            .append(true)
            .create(true)
            .open(filename)
            .expect("Unable to open file");

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
}

impl std::fmt::Display for JobResult {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(
            f,
            "{} - {} - {} @ {}: {} responsibilities, {} qualifications, {} other",
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
