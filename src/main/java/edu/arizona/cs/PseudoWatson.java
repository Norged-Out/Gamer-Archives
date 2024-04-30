package edu.arizona.cs;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
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
 * The `PseudoWatson` class represents a Watson-like search engine that performs queries on an index
 * and compares the retrieved answers with the expected answers. It uses Lucene's query parser and
 * EnglishAnalyzer for parsing English text.
 */
public class PseudoWatson {

    boolean indexExists = false;
    String indexFilePath = "";
    StandardAnalyzer analyzerV1 = null; // Not using it anymore
    EnglishAnalyzer analyzerV2 = null;  // This is better for parsing English text
    CustomAnalyzer analyzerV3 = null; // Custom Analyzer for improved parsing
    Directory index = null;
    IndexWriter writer = null;
    QueryParser parser = null;

    /**
     * Constructs a WatsonIndex object with the specified input file.
     * @param inputFile the path of the input file
     */
    public PseudoWatson(String inputFile) {
        indexFilePath = inputFile;
        //analyzerV1 = new StandardAnalyzer();
        analyzerV2 = new EnglishAnalyzer();
        
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
        //config = new IndexWriterConfig(analyzer);
    }

    private class MatchedAnswer {
        Document DocName;
        double docScore = 0;
    }

    private class QueryResult {
        String query;
        String expectedAnswer;
        //MatchedAnswer topAnswer;
        List<MatchedAnswer> topResults;

        public QueryResult(){
            query = null;
            expectedAnswer = null;
            //topAnswer = null;
            topResults = new ArrayList<MatchedAnswer>();
        }

        public void topAnswer(){
            MatchedAnswer topAnswer = topResults.get(0);
            System.out.print("Retreived answer: " + topAnswer.DocName.get("docName"));
            System.out.println(", Score: " + topAnswer.docScore);
        }

        public boolean exactMatch(){
            //return topAnswer.DocName.get("docName").equals(expectedAnswer);
            return topResults.get(0).DocName.get("docName").equals(expectedAnswer);
        }

        public boolean withinTopK(int k){
            for(int i = 1; i < topResults.size() && i < k; i++){
                MatchedAnswer r = topResults.get(i);
                if(r.DocName.get("docName").equals(expectedAnswer)){
                    //System.out.print("Answer at rank " + (i+1));
                    //System.out.println(", Score: " + r.docScore);
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
        parser = new QueryParser("docContent", analyzerV3);
        Query q = parser.parse(qr.query);
        //System.out.println("Query: " + q.toString());
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity());
        TopDocs docs = searcher.search(q, 10);
        //System.out.println("Found " + docs.totalHits + " hits.");
        for(int i = 0; i < docs.scoreDocs.length && i < 10; i++){
            ScoreDoc scoreDoc = docs.scoreDocs[i];
            Document doc = searcher.doc(scoreDoc.doc);
            MatchedAnswer result = new MatchedAnswer();
            result.DocName = doc;
            result.docScore = scoreDoc.score;
            qr.topResults.add(result);
        }
        reader.close();
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
        Thread.sleep(1000);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line; // The line being read from the file
        //List<QueryResult> queryResults = new ArrayList<QueryResult>(); // List of query results
        QueryResult qr = null;
        int totalQueries = 0, 
            correct = 0, 
            top5 = 0, 
            top10 = 0,
            incorrect = 0;
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
                System.out.println("\n----------------------------------\n");
                if(!qr.topResults.isEmpty()){
                    //qr.topAnswer();
                    if(qr.exactMatch()){
                        //System.out.println("Correct!");
                        correct++;
                    }
                    else if(qr.withinTopK(5)){
                        //System.out.println("In top 5 Results");
                        top5++;
                    }
                    else if(qr.withinTopK(10)){
                        //System.out.println("In top 10 Results");
                        top10++;
                    }
                    else{
                        //System.out.println("Incorrect!");
                        incorrect++;
                    }
                }
                //queryResults.add(qr);
                qr = null;
                /*
                System.out.println("\nContinue? (y/n)");
                String cont = scanner.nextLine();
                if (cont.equals("n")){
                    break;
                }
                */
                Thread.sleep(100);
            }
        }
        reader.close();
        System.out.println("\n----------------------------------\nStats");
        System.out.println("Total Queries: " + totalQueries);
        System.out.println("Correct Answers: " + correct);
        System.out.println("Within Top 5: " + top5);
        System.out.println("Within Top 10: " + top10);
        System.out.println("Incorrect Answers: " + incorrect);
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
        String indexFilePath = "watsonindex";
        PseudoWatson watson = new PseudoWatson(indexFilePath);
        /*
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
        */
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Pseudo Watson!\nWould you to like to play? (y/n)");
        String play = scanner.nextLine();
        if (play.equals("y")){
            System.out.println("Great! Let's get started!");
            System.out.println("\n--------------------------------------------\n");
            try {
                watson.processQuestions("questionBank.txt", scanner);
            } catch (InterruptedException | IOException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    
}
