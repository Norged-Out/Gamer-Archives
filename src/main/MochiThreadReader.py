#reddit thread reader!
#author: Alberto Andres Sanchez (mochi <3)
#editor: Priyansh Nayak (priiiiiiiiii)

#HOW TO USE: 
#1. create a file called "redditLinks.txt" and fill it with all your reddit thread links
#2. links should follow this format -> "https://www.reddit.com/r/<subreddit>/comments/<rest of link>"
#3. the program should create txt files for every link in the txt file!

import os
import praw
import re

# Reddit API credentials
client_id = 'iblolf92COEnbiw7oJTEyw'
client_secret = 'nwqle0bR_An3_IVp9bHxwkbzcIHBtg'
user_agent = 'stinky by u/mochisun'

# Initialize Reddit instance
reddit = praw.Reddit(client_id=client_id, client_secret=client_secret, user_agent=user_agent)

#get all the comments at each level from the body of the reddit post
def extract_comments(comments, level=0):
    extracted_comments = ''
    for comment in comments:
        extracted_comments += f"{'    ' * level}{comment.body}\n"
        if comment.replies:
            extracted_comments += extract_comments(comment.replies, level + 1)
    return extracted_comments

#cleans the title of any illegal characters in order to make the txt file correctly
def clean_filename(title):
    cleaned_title = re.sub(r'[<>:"/\\|?*]', '', title)
    return cleaned_title

def save_post_data(url, destination_path):
    #get the comments list from the submission object
    submission = reddit.submission(url=url)
    submission.comments.replace_more(limit=0)
    comments = submission.comments.list() 

    post_title = submission.title
    post_content = submission.selftext
    #extract all post comments
    post_comments = extract_comments(comments)

    cleaned_title = clean_filename(post_title)


    filename = destination_path + f'{cleaned_title}.txt'
    #open file with accurate name and write entire reddit post to txt file
    with open(filename, 'w', encoding='utf-8') as file:
        file.write(f'[[{cleaned_title}]]\n\n') #title
        file.write(f'\n{post_content}\n\n') #Post Content
        file.write(f'\n{post_comments}') #Comments

def main():
    # Create folder to save files
    initial_path = "src/main/resources/"
    folderName = input("Enter the name of the folder you want to save the files in: ")
    source_path = initial_path + "redditLinks.txt"
    destination_path = initial_path + folderName + "/"
    os.makedirs(destination_path)
    print(f'\nFiles will be saved in: {destination_path}\n')
    print(source_path)

    #opening file containing reddit links
    with open(source_path) as file:
        reddit_links = file.readlines()
    #save the data of each linked post into their respective files
    for link in reddit_links:
        print(link)
        save_post_data(link, destination_path)

    #finished!
    print("\nDone with extraction :)")

if __name__ == '__main__':
    main()