CREATE TABLE job_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company VARCHAR(255),
    job_title VARCHAR(255),
    industry VARCHAR(255),
    responsibilities TEXT,
    qualifications TEXT,
    skills TEXT,
    location VARCHAR(255),
    job_link_hash BINARY(32),
    UNIQUE (job_link_hash)
);