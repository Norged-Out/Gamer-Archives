import os
import re
import requests

import mwparserfromhell

# Function to fetch wikitext of a Wikipedia page using the Wikimedia API
def fetch_wikitext(page_title):
    base_url = "https://en.wikipedia.org/w/api.php"
    params = {
        "action": "query",
        "titles": page_title,
        "prop": "revisions",
        "rvprop": "content",
        "format": "json"
    }
    response = requests.get(base_url, params=params)
    data = response.json()
    page_id = list(data["query"]["pages"].keys())[0]
    wikitext = data["query"]["pages"][page_id]["revisions"][0]["*"]
    return wikitext

# Function to parse wikitext and extract information
def parse_wikitext(wikitext):
    parsed_wikitext = mwparserfromhell.parse(wikitext)
    section_titles = [section.title.strip() for section in parsed_wikitext.filter_headings()]
    links = [link.title.strip_code() for link in parsed_wikitext.filter_wikilinks()]
    templates = [template.name.strip_code() for template in parsed_wikitext.filter_templates()]
    paragraphs = [paragraph.strip() for paragraph in parsed_wikitext.filter_text()]
    
    return section_titles, links, templates, paragraphs

def clean_text(text):
    # Remove HTML tags
    clean_text = re.sub(r'<.*?>', '', text)
    # Remove special characters, punctuation, etc.
    clean_text = re.sub(r'[^\w\s]', '', clean_text)
    # Remove extra whitespace
    clean_text = re.sub(r'\s+', ' ', clean_text)
    return clean_text.strip()

# Function to store parsed data into a text file with regex cleaning
def store_data_to_text(page_title, section_titles, links, templates, paragraphs):
    filename = f"{page_title}_data.txt"
    with open(filename, 'w', encoding='utf-8') as f:
        f.write("Page Title:\n")
        f.write(clean_text(page_title) + "\n\n")
        '''
        f.write("Section Titles:\n")
        for title in section_titles:
            f.write(clean_text(title) + "\n")
        f.write("\n")

        f.write("Templates:\n")
        for template in templates:
            f.write(clean_text(template) + "\n")
        f.write("\n")
        '''
        f.write("Paragraphs:\n")
        for paragraph in paragraphs:
            cleaned_paragraph = clean_text(paragraph)
            if cleaned_paragraph:
                f.write(cleaned_paragraph + "\n\n")

def main():
    initial_path = "src/main/resources/"
    input_file = "src/main/resources/" + "library.txt"
    os.makedirs(initial_path + "wikidata", exist_ok=True)
    with open(input_file, 'r', encoding='utf-8') as file:
        for line in file:
            page_title = line.strip()
            clean_title = re.sub(r'[^\w\s]', '', page_title)
            destination = os.path.join(initial_path, clean_title)
            wikitext = fetch_wikitext(page_title)
            section_titles, links, templates, paragraphs = parse_wikitext(wikitext)

        store_data_to_text(destination, section_titles, links, templates, paragraphs)
        print(f"Data has been stored in {destination}_data.txt file.")



    



if __name__ == '__main__':
    main()