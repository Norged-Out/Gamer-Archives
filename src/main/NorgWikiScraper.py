"""
Author: Pri
This script scrapes the text content of Wikipedia articles and stores it in text files.
"""
import os
import re
from urllib.request import urlopen
import unicodedata

# Function to clean the text content of a Wikipedia article
def clean_text(text):
    normalized_text = unicodedata.normalize('NFKD', text) # Normalize Unicode characters
    clean_text = re.sub(r'<ref.*?</ref>', '', normalized_text)  # Remove reference tags and their content
    clean_text = re.sub(r'<.*?>', '\n', clean_text)  # Replace other HTML tags with newline characters
    clean_text = re.sub(r'\s+', ' ', clean_text) # Remove extra whitespace
    return clean_text.strip()

def main():
    initial_path = "src/main/resources/"
    input_file = initial_path + "library.txt"
    os.makedirs(initial_path + "wikidata", exist_ok=True) # Create a directory to store the text files
    with open(input_file, 'r', encoding='utf-8') as file:
        # Process each line in the input file
        for link in file:
            link = link.strip() # Remove trailing whitespace
            clean_title = link.replace(':', '').replace("#", " ") # Remove special characters from the title
            # Construct the URL for the raw text content of the Wikipedia article
            url = f"https://en.wikipedia.org/w/index.php?title={link}&action=raw"
            destination = initial_path + "wikidata/" + clean_title + ".txt" # Define the destination file path
            print(clean_title)
            with urlopen(url) as response:
                # Read the content of the URL and clean it
                cleaned_content = ''
                for line in response:
                    cleaned_line = clean_text(line.decode('utf-8'))
                    if cleaned_line.strip():  # Only include non-empty lines
                        cleaned_content += cleaned_line + '\n'
                with open(destination, 'w', encoding='utf-8') as f:
                    f.write(cleaned_content)
                print(f"Text content has been stored in '{destination}'.")

if __name__ == '__main__':
    main()
