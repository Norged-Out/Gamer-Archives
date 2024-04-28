"""
Author: Pri
Purpose: Take the queries from the source file and generate 
         a new file with randomized order of queries and answers
"""
import random

def main():
    initial_path = "src/main/resources/"

    # Read the contents of the original file
    with open(initial_path + "queries.txt", "r") as file:
        lines = file.readlines()

    # Process each line
    pairs = []
    for line in lines:
        line = line.strip()
        if line:
            if not line.endswith("?"):
                answer = line
            else:
                pairs.append((line, answer))

    # Randomize the order of pairs and write to a new file
    random.shuffle(pairs)
    with open(initial_path + "questionBank.txt", "w") as file:
        for query, answer in pairs:
            file.write(query + "\n")
            file.write(answer + "\n\n")

if __name__ == '__main__':
    main()
