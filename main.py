from selenium import webdriver

url = 'https://www.indeed.com/jobs?q=&l=El+Segundo%2C+CA&radius=10&advn=6787486936730117&sort=date&vjk=0b710fbedcd006ce'
browser = webdriver.Firefox()
browser.get(url)

assert 'El Segundo, CA' in browser.title

div = browser.find_element('id', 'mosaic-provider-jobcards')
ul = div.find_element('tag name', 'ul')
lis = ul.find_elements('tag name', 'li')

for li in lis:
    try:
        title = li.find_element('tag name', 'span').text
        if title != '':
            print(title)
    except Exception as e:
        title = 'N/A'
