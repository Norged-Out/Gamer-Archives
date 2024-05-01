Welcome to Gamer Archives!

This is the Final Project for CSC 483 by Priyansh Nayak, Mahesh Magendaran, Alberto Andres Sanchez, Alex Scott Gable, and Osama Alzahrawi

Here are the steps to compile and run:

1. Ensure `pom.xml` is present in the parent directory
2. The target folder should have already compiled all the files. If not, run `mvn clean` and `mvn compile` to recompile.
3. Ensure there is an index called `norgindex` in the parent directory. If this is not present, run `NorgIndexBuilder.java` present in `src\main\java\edu\arizona\cs` which would use the resources directory to build the index for you.
4. Run `GamerArchives.java` which is present in the same directory to run the main program.
5. It will prompt you to choose between running the 330 queries we have prepared for you, or to run queries of your own.
6. A sample run of the 330 queries is present in `output.txt` in the parent directory.

That is sufficient for running the main program. Here is a description of other things we have:
1. `MochiThreader.py` uses data in `resources\redditLinks.txt` to parse reddit links and produces text files in a directory of your choice in `resources`
2. `NorgQuestionGenerator.py` uses data in `resources\queries.txt` which is organized in a fashion of an expect answer followed by a number of queries for it, and different games are grouped by separating them with empty lines. It produces a random order of queries and answers in `resources\questionBank.txt`
3. `NorgWikiScraper.py` uses data in `resources\library.txt` which contains wikipedia article names to parse its content into text files
