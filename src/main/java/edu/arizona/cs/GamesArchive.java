/*
 * Author: Priyansh Nayak
 * File: GamesArchive.java
 * Class: CSC 483 - Information Retrieval
 * Assignment: Final Project - Pseudo Watson
 * Description: The GamesArchive class represents a Watson-like search engine 
 *              that performs queries on an index and compares the retrieved 
 *              answers with the expected answers. It uses Lucene's query parser 
 *              and a custom analyzer for parsing English text with appropriate 
 *              token filters and stemmers. The class provides methods to evaluate 
 *              the performance of the search engine and process user queries.
 */
package edu.arizona.cs;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * The GamesArchive class represents a Watson-like search engine that performs queries on an index
 * and compares the retrieved answers with the expected answers. It uses Lucene's query parser and
 * Custom Analyzer for parsing the text and BM25 similarity for improved search results. The class
 * provides methods to evaluate the performance of the search engine and process user queries.
 */
public class GamesArchive {

    boolean indexExists = false;
    String indexFilePath = "";
    StandardAnalyzer analyzerV1 = null; // Not using it anymore
    EnglishAnalyzer analyzerV2 = null;  // This is better for parsing English text
    CustomAnalyzer analyzerV3 = null; // Custom Analyzer for improved parsing
    Directory index = null;
    QueryParser parser = null;

    /**
     * Constructs a GamesArchive object with the specified input file.
     * @param inputFile the path of the input file
     */
    public GamesArchive(String inputFile) {
        indexFilePath = inputFile;
        // analyzerV1 = new StandardAnalyzer();
        // analyzerV2 = new EnglishAnalyzer();
        
        try {
            // Custom Analyzer for improved parsing
            analyzerV3 = CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("stop", "ignoreCase", "true", "words", "stopwords.txt", "format", "wordset")
                        .addTokenFilter("porterstem")
                        .build();
            index = FSDirectory.open(Paths.get(indexFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /*
     * This class represents a matched answer and its score
     */
    private class MatchedAnswer {
        Document DocName;
        double docScore = 0;
    }

    /*
     * This class represents the result of a query
     */
    private class QueryResult {
        String query;
        String expectedAnswer;
        double rank;
        List<MatchedAnswer> topResults;

        public QueryResult(){
            query = null;
            expectedAnswer = null;
            rank = 0;
            topResults = new ArrayList<MatchedAnswer>();
        }

        /*
         * This function prints the top answer and its score
         */
        public void topAnswer(){
            MatchedAnswer topAnswer = topResults.get(0);
            System.out.println("Retreived answer: " + topAnswer.DocName.get("docName"));
            System.out.println("Score: " + topAnswer.docScore);
        }

        /*
         * This function checks if the expected answer is an exact match with the top result
         * @return boolean: True if the expected answer is an exact match, false otherwise
         */
        public boolean exactMatch(){
            if(topResults.get(0).DocName.get("docName").equals(expectedAnswer)){
                rank = 1;
                return true;
            }
            return false;
        }

        /*
         * This function checks if the expected answer is within the top K results
         * @param k: The number of top results to check
         * @return boolean: True if the expected answer is within the top K results, false otherwise
         */
        public boolean withinTopK(int k){
            for(int i = 1; i < topResults.size() && i < k; i++){
                MatchedAnswer r = topResults.get(i);
                if(r.DocName.get("docName").equals(expectedAnswer)){
                    rank = i+1;
                    return true;
                }
            }
            return false;
        }

    }


    /*
     * This function is used to perform different queries on the index
     * using lucene's query parser and build the list of resulting
     * documents hit by the query and their scores.
     * @param query: The query to be run on the index
     * @return List<MatchedAnswer>: The list of results
     * @throws ParseException 
     * @throws IOException 
     */
    private void runQuery(QueryResult qr) throws ParseException, IOException{
        parser = new QueryParser("docContent", analyzerV3); // Parse the query using the custom analyzer
        Query q = parser.parse(qr.query);
        //System.out.println("Query: " + q.toString());
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity()); // Use BM25 similarity for improved results
        TopDocs docs = searcher.search(q, 10); // Search for top 10 results
        //System.out.println("Found " + docs.totalHits + " hits.");
        for(int i = 0; i < docs.scoreDocs.length && i < 10; i++){
            ScoreDoc scoreDoc = docs.scoreDocs[i];
            Document doc = searcher.doc(scoreDoc.doc);
            // Store the matched answer and its score
            MatchedAnswer result = new MatchedAnswer();
            result.DocName = doc;
            result.docScore = scoreDoc.score;
            qr.topResults.add(result);
        }
        reader.close();
    }

    /**
     * Evaluates the performance of the search engine by calculating the 
     * precision at 1, 5, and 10, and the mean reciprocal rank.
     * @param totalQueries the total number of queries
     * @param correct the number of correct answers
     * @param top5 the number of answers within the top 5 results
     * @param top10 the number of answers within the top 10 results
     * @param incorrect the number of incorrect answers
     * @param rank the sum of the reciprocal ranks
     */
    private void evaluatePerformance(int totalQueries, int correct, int top5, int top10, int incorrect, double rank){
        System.out.println("----------------Stats----------------\n");
        System.out.println("Total Queries: " + totalQueries);
        System.out.println("Correct Answers: " + correct);
        System.out.println("Within Top 5: " + top5);
        System.out.println("Within Top 10: " + top10);
        System.out.println("Incorrect Answers: " + incorrect);

        double p1 = (double)correct / totalQueries;
        double p5 = (double)(correct + top5) / totalQueries;
        double p10 = (double)(correct + top5 + top10) / totalQueries;
        double MRR = rank / totalQueries;

        System.out.println("\n--------------Performance--------------\n");
        System.out.println("Precision at 1: " + p1);
        System.out.println("Precision at 5: " + p5);
        System.out.println("Precision at 10: " + p10);
        System.out.println("Mean Reciprocal Rank: " + MRR);
    }


    /**
     * Reads a text file and extracts answers and questions.
     * Performs queries on the index and compares the retrieved answers with the expected answers.
     * @param filePath the path of the text file
     * @param scanner the scanner object to read user input
     * @throws InterruptedException 
     * @throws ParseException 
     * @throws IOException 
     */
    private void processQuestions(String filePath, Scanner scanner) throws InterruptedException, IOException, ParseException{
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filePath).getFile());
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line; // The line being read from the file
        QueryResult qr = null; // The query result object
        // Variables to keep track of the performance
        int totalQueries = 0, 
            correct = 0, 
            top5 = 0, 
            top10 = 0,
            incorrect = 0;
        double RR = 0;
        Thread.sleep(1000); // Sleep for 1 second
        while((line = reader.readLine()) != null){
            // If the line is not empty, it is either a query or an answer
            if(!line.isEmpty()){
                if(qr == null){
                    qr = new QueryResult();
                    qr.query = line;
                }
                else{
                    qr.expectedAnswer = line;
                    totalQueries++;
                }
            }
            // If the line is empty, then perform the query
            else {
                runQuery(qr);
                System.out.println("Query: " + qr.query);
                System.out.println("Expected answer: " + qr.expectedAnswer);
                qr.topAnswer();
                // Evaluate the query results
                if(!qr.topResults.isEmpty()){
                    if(qr.exactMatch()){
                        System.out.println("Correct! Answer found in top result. :)");
                        correct++;
                    }
                    else if(qr.withinTopK(5)){
                        System.out.println("Answer found in top 5 Results");
                        top5++;
                    }
                    else if(qr.withinTopK(10)){
                        System.out.println("Answer found in top 10 Results");
                        top10++;
                    }
                    else{
                        System.out.println("Incorrect! Answer not found in top 10 results. :(");
                        incorrect++;
                    }
                    System.out.println("Query Rank: " + qr.rank);
                    System.out.println("\n----------------------------------\n");
                    RR += (qr.rank > 0) ? 1.0 / qr.rank : 0; // Reciprocal Rank
                }
                qr = null; // Reset the query result
                Thread.sleep(100); // Sleep for 100ms
            }
        }
        reader.close();
        evaluatePerformance(totalQueries, correct, top5, top10, incorrect, RR);
    }

    /**
     * Reads user input and performs queries on the index.
     * @param scanner the scanner object to read user input
     * @throws ParseException 
     * @throws IOException 
     */
    private void userQuery(Scanner scanner) throws ParseException, IOException{
        while(true){
            System.out.println("Enter your query: ");
            String query = scanner.nextLine();
            QueryResult qr = new QueryResult();
            qr.query = query;
            runQuery(qr);
            qr.topAnswer();
            System.out.println("\nContinue? (y/n)");
            String cont = scanner.nextLine();
            if (cont.equals("n")){
                break;
            }
        }
    }

    /**
     * Main method to run the GamesArchive search engine.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String indexFilePath = "norgindex";
        GamesArchive watson = new GamesArchive(indexFilePath);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Games Archive!\nWhich mode would you like to check?\n");
        System.out.println("1. Run pre-built queries\n2. Enter your own query\n");
        String play = scanner.nextLine();
        if (play.equals("1")){
            System.out.println("\nStarting pre-built queries...");
            System.out.println("\n--------------------------------------------\n");
            try {
                watson.processQuestions("questionBank.txt", scanner);
            } catch (InterruptedException | IOException | ParseException e) {
                e.printStackTrace();
            }
        }
        else if (play.equals("2")){
            System.out.println("Great! Let's get started!");
            System.out.println("\n--------------------------------------------\n");
            try {
                watson.userQuery(scanner);
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        }
        else{
            System.out.println("Invalid input. Exiting...");
        }
    }

    
}
