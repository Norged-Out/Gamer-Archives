import os
import re
from urllib.request import urlopen

# Function to clean text using regular expressions
def clean_text(text):
    clean_text = re.sub(r'<ref.*?</ref>', '', text)  # Remove reference tags and their content
    clean_text = re.sub(r'<.*?>', '\n', clean_text)  # Replace other HTML tags with newline characters
    # Remove extra whitespace
    clean_text = re.sub(r'\s+', ' ', clean_text)
    return clean_text.strip()

def main():
    initial_path = "src/main/resources/"
    input_file = "src/main/resources/" + "library.txt"
    os.makedirs(initial_path + "wikidata", exist_ok=True)
    with open(input_file, 'r', encoding='utf-8') as file:
        for link in file:
            clean_title = link.replace(' ', '_').strip()
            url = f"https://en.wikipedia.org/w/index.php?title={clean_title}&action=raw"
            destination = initial_path + "wikidata/" + link + ".txt"
            with urlopen(url) as response:
                with open(destination, 'a', encoding='utf-8') as f:
                    for line in response:
                        cleaned_line = clean_text(line.decode('utf-8'))
                        if cleaned_line.strip():  # Only write non-empty lines
                            f.write(cleaned_line + '\n')

        print(f"Text content has been stored in '{destination}'.")

if __name__ == '__main__':
    main()

