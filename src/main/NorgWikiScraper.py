import os
import re
from urllib.request import urlopen
import unicodedata

# Function to clean text using regular expressions
def clean_text(text):
    normalized_text = unicodedata.normalize('NFKD', text)
    clean_text = re.sub(r'<ref.*?</ref>', '', normalized_text)  # Remove reference tags and their content
    clean_text = re.sub(r'<.*?>', '\n', clean_text)  # Replace other HTML tags with newline characters
    # Remove extra whitespace
    clean_text = re.sub(r'\s+', ' ', clean_text)
    return clean_text.strip()

def main():
    initial_path = "src/main/resources/"
    input_file = "src/main/resources/library.txt"
    os.makedirs(initial_path + "wikidata", exist_ok=True)
    with open(input_file, 'r', encoding='utf-8') as file:
        for link in file:
            link = link.strip()
            clean_title = link.replace(':', '').replace("#", " ")
            url = f"https://en.wikipedia.org/w/index.php?title={link}&action=raw"
            destination = initial_path + "wikidata/" + clean_title + ".txt"
            print(clean_title)
            with urlopen(url) as response:
                print("opened url")
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
