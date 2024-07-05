from openai import OpenAI
import time
import json

client = OpenAI()

now = time.time()
completion = client.chat.completions.create(
    model="gpt-3.5-turbo",
    messages=[
        {
            "role": "system",
            "content": "You are skilled at extracting relevant data from job listings. You respond only in valid JSON",
        },
        {
            "role": "user",
            "content": """Can you give me the industry (string), responsibilities (array of strings), and qualifications (array of strings) of this job listing. Find as many responsibilities and qualifications as it says: Multiplatform Editor, Newsletters (Temporary)
  The Los Angeles Times, 2300 E. Imperial Hwy, El Segundo, California, United States of America Req #20
  Saturday, June 22, 2024

  The Los Angeles Times is looking for a temporary multiplatform editor to help edit, write and produce our newsletters, which have become a vital platform for connecting our journalism with our audience.

  The Times currently has more than two dozen newsletters, including Essential California, the Latinx Files, Boiling Point and Screen Gab.

  

  We’re looking for an enthusiastic and entrepreneurial journalist who can edit and contribute to our expanding portfolio of newsletters as well as help compile Essential California, our flagship newsletter.

    

  You will also:

  Be a final set of eyes on our morning flagship newsletter before it goes to publish 
  Be a key member of our Essential California team, helping compile links, write editions and track and improve performance 
  Work closely with journalists across departments and time zones to write, edit, schedule, produce and proofread newsletters
  Track the newsletters’ performance and work with editors and writers to build audience and improve key performance indicators
  Assist editors and writers in tracking and incorporating reader response and suggestions

  

  Requirements:

  A minimum of five years of journalism experience
  Strong news judgment, writing and editing skills and attention to detail        
  Experience working diplomatically across newsroom disciplines and an inclusive approach in the workplace
  Deep interest in online audiences, analytics and the best practices of editorial newsletters
  Basic HTML skills

  

  This editor will work Monday to Friday starting at 5:30 a.m. Pacific time. The full-time, temporary position is expected to run from July to December. 

  

  Qualified applicants should send a resume, cover letter and work samples to Head of Newsletters Karim Doumar. Please note, the opportunity to attach additional documents appears on the third screen and is labeled “Upload.”

  The L.A. Times is an equal employment opportunity employer and welcomes all qualified applicants regardless of race, ethnicity, religion, gender, gender identity, sexual orientation, disability status, protected veteran status, or any other characteristic protected by law. We actively work to create an inclusive environment where all of our employees can thrive.  Explore our company history, achievement, values, mission and more on our career site.

  The pay scale the Company reasonably expects to pay for this position at the time of the posting is $39.59 - $45.77 and takes into account a wide range of factors including but not limited to skill set, experience, training, licenses, certifications, and other business or organizational needs.  Compensation will be determined based on the above factors along with the requirements of the position.  At The L.A. Times, it is not typical for an individual to be hired at or near the top of the range for the role. Please visit our career site to view the benefits available to our employees.

  The Company is a mandatory vaccination employer for COVID-19 and its variants. The Company requires that its employees be fully vaccinated as of their start date. If you require a medical or religious accommodation, we will engage in the interactive process with you. Proof of vaccination will be required prior to start. If we make you an offer and you are not yet vaccinated, we will accommodate a delay in start date.

  We recently transitioned to a new system if you noticed any issues in the application process, please contact Talent Acquisition at: talentacquisition@caltimes.com

  

  Other details
  Job Family Los Angeles Times Super Co. Job Function Individual Contributor Pay Type Hourly Min Hiring Rate $39.59 Max Hiring Rate $45.77
  Apply Now""",
        },
    ],
)

print(f"Time taken: {time.time() - now} seconds")
print(completion.choices[0].message)

output = json.loads(completion.choices[0].message.content)
print(json.dumps(output, indent=2))
