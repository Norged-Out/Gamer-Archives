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

public class PseudoWatson {

    boolean indexExists = false;
    String indexFilePath = "";
    //StandardAnalyzer oldAnalyzer = null; // Not using it anymore
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
        try {
            index = FSDirectory.open(Paths.get(indexFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        //config = new IndexWriterConfig(analyzer);
    }


    /*
     * This function is used to perform different queries on the index
     * using lucene's query parser and build the list of resulting
     * documents hit by the query and their scores.
     * @param query: The query to be run
     * @param altSimilarity: The flag to indicate if the similarity is to be changed
     * @return List<ResultClass>: The list of results
     */
    private List<ResultClass> runQuery(String query, Boolean altSimilarity){
        List<ResultClass>  results = new ArrayList<ResultClass>();
        parser = new QueryParser("text", analyzer);
        Query q;
        try {
            q = parser.parse(query);
            System.out.println("Query: " + q.toString());
            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            if(altSimilarity) {
                searcher.setSimilarity(new BM25Similarity());
            }
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Found " + results.size() + " hits");
        for(ResultClass res: results){
            System.out.print(res.DocName.get("docName"));
            System.out.println(", Score: " + res.docScore);
        }
        return results;
    }

    public static void main(String[] args) {
        String indexFilePath = "testindex";
        PseudoWatson watson = new PseudoWatson(indexFilePath);
        try {
            IndexReader reader = DirectoryReader.open(watson.index);
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document doc = reader.document(i);
                System.out.println("docid: " + doc.get("docName"));
                System.out.println("text: " + doc.get("docData"));
                System.out.println("--------------------------------------------");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the query: ");
        String query = scanner.nextLine();
        List<ResultClass> results = watson.runQuery(query, false);
        for(ResultClass res: results){
            System.out.print(res.DocName.get("docName"));
            System.out.println(", Score: " + res.docScore);
        }
        System.out.println("Enter the query: ");
        query = scanner.nextLine();
        results = watson.runQuery(query, true);
        for(ResultClass res: results){
            System.out.print(res.DocName.get("docName"));
            System.out.println(", Score: " + res.docScore);
        }
    }

    
}
