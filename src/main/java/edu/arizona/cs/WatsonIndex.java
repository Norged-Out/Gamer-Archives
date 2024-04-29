
package edu.arizona.cs;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The WatsonIndex class represents an index builder for Watson documents.
 * It uses Apache Lucene library for indexing and searching documents.
 * The class provides methods to build a new index from a given input file,
 * parse text files, and add documents to the index.
 */
public class WatsonIndex {
    boolean indexExists = false;
    String indexFilePath = "";
    StandardAnalyzer analyzerV1 = null; // Not using it anymore
    EnglishAnalyzer analyzerV2 = null;  // This is better for parsing English text
    CustomAnalyzer analyzerV3 = null; // Custom Analyzer for improved parsing
    Directory index = null;
    IndexWriterConfig config = null;
    IndexWriter writer = null;

    /**
     * Constructs a WatsonIndex object with the specified input file.
     * @param inputFile the path of the input file
     */
    public WatsonIndex(String inputFile) {
        indexFilePath = inputFile;
        try {
            buildNewIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds a new index using the specified input file.
     */
    private void buildNewIndex() throws IOException{
        //Get file from resources folder
        analyzerV2 = new EnglishAnalyzer();
        //analyzerV1 = new StandardAnalyzer();
        // Custom Analyzer for improved parsing
        analyzerV3 = CustomAnalyzer.builder()
                    .withTokenizer("standard")
                    .addTokenFilter("lowercase")
                    .addTokenFilter("stop", "ignoreCase", "true", "words", "stopwords.txt", "format", "wordset")
                    .addTokenFilter("porterstem")
                    .build();
        config = new IndexWriterConfig(analyzerV3);
        index = FSDirectory.open(Paths.get(indexFilePath));
        // Create IndexWriter
        writer = new IndexWriter(index, config);
        // Parse input file and add documents to index
        allFilesToProcess(writer);
        // Commit and close IndexWriter
        writer.commit();
        writer.close();
        System.out.println("New index created and saved successfully at: " + indexFilePath);
        indexExists = true;
    }

    /**
     * Parses a text file and returns its content as a string.
     * @param filePath the path of the text file
     * @return the content of the text file
     */
    private String parseTextFile(String filePath){
        StringBuilder contentBuilder = new StringBuilder();
        try {
            // Open the file
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));

            // Read the file line by line and append to the content builder
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }

            // Close the reader
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    /**
     * Adds all text files in the resources directory to the index.
     * @param writer the IndexWriter object
     */
    private void allFilesToProcess(IndexWriter writer) {
        String resourcesPath = "src/main/resources";
        // Get the resources directory
        File resourcesDirectory = new File(resourcesPath);
        String directoryName = null; 
        int docCount = 0, fileCount = 0;
        // Check if it exists and is a directory
        if (resourcesDirectory.exists() && resourcesDirectory.isDirectory()) {
            // Get all subdirectories in the resources directory
            File[] subDirectories = resourcesDirectory.listFiles(File::isDirectory);
            // Iterate over each subdirectory
            for (File subDirectory : subDirectories) {
                directoryName = subDirectory.getName();
                System.out.println("Directory: " + directoryName);
                docCount++;
                // Get all text files in the subdirectory
                File[] textFiles = subDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
                // Iterate over each text file
                List<String> fileContents = new ArrayList<>();
                for (File textFile : textFiles) {
                    System.out.println("  Text File: " + textFile.getName());
                    fileCount++;
                    String fileContent = parseTextFile(textFile.getPath());
                    fileContents.add(fileContent);
                }
                String accumulatedContent = String.join("", fileContents);
                // Add the accumulated content to the index
                try {
                    Document newDoc = new Document();
                    newDoc.add(new StringField("docName", directoryName, Field.Store.YES));
                    newDoc.add(new TextField("docContent", accumulatedContent, Field.Store.YES));
                    writer.addDocument(newDoc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Resources directory does not exist or is not a directory.");
        }
        System.out.println("Total directories: " + docCount);
        System.out.println("Total files: " + fileCount);
    }

    /*
     * This function acted as an earlier version of the parser. It is not used anymore.
     * It was used to parse a single text file and extract documents from it.
     * It obeyed the format provided in the Watson specification.
     * @param writer the IndexWriter object
     * @param filePath the path of the text file
     */
    /*
    private void oldParser(IndexWriter writer, String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        String docName = null;
        
        // Regex pattern for extracting document names enclosed in [[ ]]
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        System.out.println("Parsing file: " + filePath);
        //ClassLoader classLoader = getClass().getClassLoader();
        //System.out.println("File path: " + classLoader.getResource("resources"));
        //File file = new File(classLoader.getResource(filePath).getFile());
        File file = new File(filePath);
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
                        writer.addDocument(newDoc);
                        contentBuilder.setLength(0); // Clear contentBuilder for the next document
                    }
                    docName = matcher.group(1); // Extract the document name
                    System.out.println("Line: " + line);
                    System.out.println("Document name: " + docName);
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
                writer.addDocument(newDoc);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    */

    /**
     * The main method of the WatsonIndex program.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Hello, World!");
        WatsonIndex engine = new WatsonIndex("watsonindex");
        
        try {
            System.out.println("Reading index");
            IndexReader reader = DirectoryReader.open(engine.index);
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
    }
    
}