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
import org.apache.lucene.store.ByteBuffersDirectory;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WatsonIndex {
    boolean indexExists = false;
    String indexFilePath = "";
    StandardAnalyzer analyzer = null;
    Directory index = null;
    IndexWriterConfig config = null;
    IndexWriter writer = null;

    public WatsonIndex(String inputFile) {
        indexFilePath = inputFile;
    }

    private List<Document> parseTextFile(String filePath) {
        List<Document> documents = new ArrayList<>();
        StringBuilder contentBuilder = new StringBuilder();
        String docName = null;
        
        // Regex pattern for extracting document names enclosed in [[ ]]
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filePath).getFile());

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // Found a new document name
                    if (docName != null) {
                        // If there was already a document being processed, add it to the list
                        Document newDoc = new Document();
                        newDoc.add(new StringField("docName", docName, Field.Store.YES));
                        newDoc.add(new TextField("docData", contentBuilder.toString(), Field.Store.YES));
                        documents.add(newDoc);
                        contentBuilder.setLength(0); // Clear contentBuilder for the next document
                    }
                    docName = matcher.group(1); // Extract the document name
                } else {
                    // Append line to content if not a document name
                    contentBuilder.append(line).append("\n");
                }
            }
            // Add the last document
            if (docName != null) {
                Document newDoc = new Document();
                newDoc.add(new StringField("docName", docName, Field.Store.YES));
                newDoc.add(new TextField("docData", contentBuilder.toString(), Field.Store.YES));
                documents.add(newDoc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return documents;
    }

    private void buildNewIndex(String inputFilePath) {
        //Get file from resources folder
        analyzer = new StandardAnalyzer();
        //index = new ByteBuffersDirectory();
        config = new IndexWriterConfig(analyzer);
        try {
            index = FSDirectory.open(Paths.get(indexFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            // Create IndexWriter
            writer = new IndexWriter(index, config);
            // Parse input file and add documents to index
            List<Document> documents = parseTextFile(inputFilePath);
            for (Document doc : documents) {
                writer.addDocument(doc);
            }

            // Commit and close IndexWriter
            writer.commit();
            writer.close();
            System.out.println("New index created and saved successfully at: " + indexFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        indexExists = true;
    }

    private void updateIndex() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(indexFilePath).getFile());
        
    }

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        WatsonIndex engine = new WatsonIndex("testindex");
        if(!engine.indexExists){
            engine.buildNewIndex("example.txt");
        }
        try {
            IndexReader reader = DirectoryReader.open(engine.index);
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document doc = reader.document(i);
                System.out.println("docid: " + doc.get("docName"));
                //System.out.println("text: " + doc.get("docData"));
                System.out.println("--------------------------------------------");
            }
            reader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
