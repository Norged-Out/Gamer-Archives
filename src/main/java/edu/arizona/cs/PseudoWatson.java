package edu.arizona.cs;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The `PseudoWatson` class represents a Watson-like search engine that performs queries on an index
 * and compares the retrieved answers with the expected answers. It uses Lucene's query parser and
 * EnglishAnalyzer for parsing English text.
 */
public class PseudoWatson {

    boolean indexExists = false;
    String indexFilePath = "";
    //StandardAnalyzer analyzer = null; // Not using it anymore
    EnglishAnalyzer analyzer = null;  // This is better for parsing English text
    Directory index = null;
    //IndexWriterConfig config = null;
    IndexWriter writer = null;
    QueryParser parser = null;

    /**
     * Constructs a WatsonIndex object with the specified input file.
     * @param inputFile the path of the input file
     */
    public PseudoWatson(String inputFile) {
        indexFilePath = inputFile;
        analyzer = new EnglishAnalyzer();
        //analyzer = new StandardAnalyzer();
        try {
            index = FSDirectory.open(Paths.get(indexFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        //config = new IndexWriterConfig(analyzer);
    }

    private class ResultClass {
        Document DocName;
        double docScore = 0;
    }


    /*
     * This function is used to perform different queries on the index
     * using lucene's query parser and build the list of resulting
     * documents hit by the query and their scores.
     * @param query: The query to be run on the index
     * @return List<ResultClass>: The list of results
     */
    private List<ResultClass> runQuery(String query){
        List<ResultClass>  results = new ArrayList<ResultClass>();
        parser = new QueryParser("docContent", analyzer);
        Query q;
        try {
            q = parser.parse(query);
            System.out.println("Query: " + q.toString());
            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());
            TopDocs docs = searcher.search(q, 10);
            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                ResultClass result = new ResultClass();
                result.DocName = doc;
                result.docScore = scoreDoc.score;
                results.add(result);
            }
            reader.close();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int hits = results.size();
        System.out.println("Found " + hits + " hits");
        int i = 0;
        for(ResultClass res: results){
            //System.out.print(res.DocName.get("docName"));
            //System.out.println(", Score: " + res.docScore);
            i++;
            if(i == 10){
                break;
            }
        }
        return results;
    }
    /**
     * Reads a text file and extracts answers and questions.
     * Performs queries on the index and compares the retrieved answers with the expected answers.
     * @param filePath the path of the text file
     * @param scanner the scanner object to read user input
     */
    private void processQuestions(String filePath, Scanner scanner){
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filePath).getFile());
        List<ResultClass> results = null;
        ResultClass topAnswer = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line; // The line being read from the file
            String query = null, answer = null; // The query and the expected answer
            while((line = reader.readLine()) != null){
                // If the line is not empty, it is either a query or an answer
                if(!line.isEmpty()){
                    if(query == null){
                        query = line;
                    }
                    else{
                        answer = line;
                    }
                }
                // If the line is empty, it is the end of the question
                else if(line.isEmpty()){
                    // Perform the query
                    results = runQuery(query);
                    System.out.println("Query: " + query);
                    System.out.println("\nRunning Watson...\n");
                    // If there are results, get the top answer
                    if (results.size() > 0){
                        topAnswer = results.get(0);
                        String docName = topAnswer.DocName.get("docName");
                        System.out.println("Retrieved answer: " + docName);
                        // Compare the top answer with the expected answer
                        if(docName.equals(answer)){
                            System.out.println("Correct!");
                        }
                        // Ask the user if they want to see alternative answers
                        else{
                            System.out.println("Incorrect!");
                            System.out.println("Expected answer: " + answer);
                            System.out.println("\nWould you like to see alternative answers? (y/n)");
                            String alt = scanner.nextLine();
                            // List up to 3 alternative answers
                            if(alt.equals("y")){
                                System.out.println("Here are some alternative answers I found:");
                                int i = 1;
                                while(i < 4 && i < results.size()){
                                    System.out.println(results.get(i).DocName.get("docName"));
                                    i++;
                                }
                            }
                        }
                        
                    }
                    // Reset the query and answer for the next query
                    query = null;
                    answer = null;
                    System.out.println("\nContinue? (y/n)");
                    String cont = scanner.nextLine();
                    if (cont.equals("n")){
                        break;
                    }
                }
            }
            reader.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }


     /**
      * Old implementation of processQuestions
      * Reads a text file and extracts answers and questions.
      * @param filePath the path of the text file
      * @return a HashMap containing answers as keys and a list of questions as values
      */
    /*private HashMap<String, List<String>> processQuestions(String filePath) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filePath).getFile());
        HashMap<String, List<String>> data = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            String answer = null;
            List<String> questions = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if(line.isEmpty() && answer != null){
                    data.put(answer, questions);
                    answer = null;
                    questions = new ArrayList<>();
                }
                else if (!line.isEmpty()) {
                    if (answer == null) {
                        answer = line;
                    } else {
                        questions.add(line);
                    }
                }
            }
            if (answer != null) {
                data.put(answer, questions);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }*/

    public static void main(String[] args) {
        String indexFilePath = "testindex";
        PseudoWatson watson = new PseudoWatson(indexFilePath);
        try {
            IndexReader reader = DirectoryReader.open(watson.index);
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document doc = reader.document(i);
                System.out.println("docid: " + doc.get("docName"));
                //System.out.println("text: " + doc.get("docContent"));
                System.out.println("--------------------------------------------");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Pseudo Watson!\nWould you to like to play? (y/n)");
        String play = scanner.nextLine();
        if (play.equals("y")){
            System.out.println("Great! Let's get started!");
            System.out.println("\n--------------------------------------------\n");
            watson.processQuestions("questionBank.txt", scanner);
        }
    }

    
}
