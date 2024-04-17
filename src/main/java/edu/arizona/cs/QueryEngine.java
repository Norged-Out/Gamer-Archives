package edu.arizona.cs;

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
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class QueryEngine {
    boolean indexExists=false;
    String inputFilePath ="";
    StandardAnalyzer analyzer = null;
    Directory index = null;
    IndexWriterConfig config = null;
    IndexWriter w = null;
    QueryParser parser = null;

    public QueryEngine(String inputFile){
        inputFilePath =inputFile;
        buildIndex();
    }

    /*
     * This function is used to build the index from the input file
     * and store it in the ByteBuffersDirectory index.
     */
    private void buildIndex() {
        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(inputFilePath).getFile());
        analyzer = new StandardAnalyzer();
        index = new ByteBuffersDirectory();
        config = new IndexWriterConfig(analyzer);
        
        try (Scanner inputScanner = new Scanner(file)) {
            w = new IndexWriter(index, config);
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                String[] parts = line.split("\\s+", 2);
                Document newDoc = new Document();
                newDoc.add(new StringField("docid", parts[0], Field.Store.YES));
                newDoc.add(new TextField("text", parts[1], Field.Store.YES));
                w.addDocument(newDoc);
            }
            w.close();
            inputScanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        indexExists = true;
    }

    public static void main(String[] args ) {
        try {
            String fileName = "input.txt";
            System.out.println("********Welcome to  Homework 3!");
            //String[] query13a = {"information", "retrieval"};
            QueryEngine objQueryEngine = new QueryEngine(fileName);
            try {
                IndexReader reader = DirectoryReader.open(objQueryEngine.index);
                for (int i = 0; i < reader.maxDoc(); i++) {
                    Document doc = reader.document(i);
                    System.out.println("docid: " + doc.get("docid"));
                    System.out.println("text: " + doc.get("text"));
                    System.out.println("--------------------------------------------");
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /*
     * This function is used to perform different queries on the index
     * using lucene's query parser and build the list of resulting
     * documents hit by the query and their scores.
     * @param query: The query to be run
     * @param altSimilarity: The flag to indicate if the similarity is to be changed
     * @return List<ResultClass>: The list of results
     */
    private List<ResultClass> runQuery(String query, Boolean altSimilarity) throws java.io.FileNotFoundException,java.io.IOException {
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
        }
        System.out.println("Found " + results.size() + " hits");
        for(ResultClass res: results){
            System.out.print(res.DocName.get("docid"));
            System.out.println(", Score: " + res.docScore);
        }
        return results;
    }

    /*
     * This function is used to run the query Q1.1: information retrieval
     * @param query: The query to be run
     * @return List<ResultClass>: The list of results
     */
    public List<ResultClass> runQ1_1(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        if(!indexExists) {
            buildIndex();
        }
        List<ResultClass>  ans= runQuery(String.join(" ", query), false);
        return ans;
    }

    /*
     * This function is used to run the query Q1.2.a: +text:information +text:retrieval
     * @param query: The query to be run
     * @return List<ResultClass>: The list of results
     */
    public List<ResultClass> runQ1_2_a(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        if(!indexExists) {
            buildIndex();
        }
        String queryString = "text:\"" + query[0] + "\" AND text:\"" + query[1] + "\"";
        // Gets parsed to query: +text:information +text:retrieval
        List<ResultClass>  ans= runQuery(queryString, false);
        return ans;
    }

    /*
     * This function is used to run the query Q1.2.b: +text:information -text:retrieval
     * @param query: The query to be run
     * @return List<ResultClass>: The list of results
     */
    public List<ResultClass> runQ1_2_b(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        if(!indexExists) {
            buildIndex();
        }
        String queryString = "text:\"" + query[0] + "\" AND NOT text:\"" + query[1] + "\"";
        // Gets parsed to query: +text:information -text:retrieval
        List<ResultClass>  ans= runQuery(queryString, false);
        return ans;
    }

    /*
     * This function is used to run the query Q1.2.c: text:"information retrieval"~1
     * @param query: The query to be run
     * @return List<ResultClass>: The list of results
     */
    public List<ResultClass> runQ1_2_c(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        if(!indexExists) {
            buildIndex();
        }
        String queryString = "text:\"" + query[0] + " " + query[1] + "\"~1";
        List<ResultClass>  ans= runQuery(queryString, false);
        return ans;
    }

    /*
     * This function is used to run the query Q1.3: information retrieval
     * I'm going to try changing the similarity from Classic VSM to BM25
     * @param query: The query to be run
     * @return List<ResultClass>: The list of results
     */
    public List<ResultClass> runQ1_3(String[] query) throws java.io.FileNotFoundException,java.io.IOException {

        if(!indexExists) {
            buildIndex();
        }
        List<ResultClass>  ans = runQuery(String.join(" ", query), true);
        System.out.println("Scores for BM25 look similar to classic VSM");
        return ans;
    }


    private  List<ResultClass> returnDummyResults(int maxNoOfDocs) {

        List<ResultClass> doc_score_list = new ArrayList<ResultClass>();
            for (int i = 0; i < maxNoOfDocs; ++i) {
                Document doc = new Document();
                doc.add(new TextField("title", "", Field.Store.YES));
                doc.add(new StringField("docid", "Doc"+Integer.toString(i+1), Field.Store.YES));
                ResultClass objResultClass= new ResultClass();
                objResultClass.DocName =doc;
                doc_score_list.add(objResultClass);
            }

        return doc_score_list;
    }

}
