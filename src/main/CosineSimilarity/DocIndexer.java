package main.CosineSimilarity;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DocIndexer {
    /**
     * 索引保存的位置
     */
    private final File sourceDirectory;

    /**
     * 文档路径
     */
    private final File indexDirectory;

    private int docNumber;
    private IndexReader reader;

    public DocIndexer() {
        this.sourceDirectory = new File(Configuration.SOURCE_DIRECTORY);
        this.indexDirectory = new File(Configuration.INDEX_DIRECTORY);
        this.docNumber = 0;
        this.reader = null;
    }

    public IndexReader getReader() throws IOException {
        if (reader == null) {
            reader = IndexReader.open(NIOFSDirectory.open(new File(Configuration.INDEX_DIRECTORY)));
        }
        return reader;
    }

    /**
     * 将所有的文档加入lucene中
     */
    public void index() throws CorruptIndexException, LockObtainFailedException, IOException {
        Directory dir = FSDirectory.open(this.indexDirectory);

        Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_36);
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_36, analyzer);

        if (Configuration.CLEAR_INDEX_DIRECTORY) {
            Util.delAllFile(Configuration.INDEX_DIRECTORY);
        }
        if (Configuration.APPEND_INDEX_DIRECTORY) {
            // Add new documents to an existing index:
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        IndexWriter writer = new IndexWriter(dir, iwc);

        System.out.println("Number of files: " + sourceDirectory.listFiles().length);

        for (File file : sourceDirectory.listFiles()) {
            //读取文件内容，并去除数字标点符号
            //ArrayList<String> fileContent = fileReader(file);
            //String history = fileContent.get(0);
            //String response = fileContent.get(1);

            //history = history.replaceAll("\\d+(?:[.,]\\d+)*\\s*", "");
            //String docName = file.getName();
            //Document doc = new Document();
            //doc.add(new Field("docName", new StringReader(docName), Field.TermVector.YES));
            //doc.add(new Field("history", new StringReader(history), Field.TermVector.YES));
            //doc.add(new Field("response", new StringReader(response), Field.TermVector.YES));

            //indexWriter.addDocument(doc);
            ArrayList<String> fileContent = Util.fileReader(file);
            for (String dialog: fileContent) {
                String[] sents = dialog.split("\t");
                //generate a document
                Document doc = new Document();
                doc.add(new Field("history", sents[0], Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
                doc.add(new Field("response", sents[1], Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field("document_id", Integer.toString(this.docNumber), Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field("conversation_id", sents[3], Field.Store.YES, Field.Index.NOT_ANALYZED));

                writer.addDocument(doc);
                this.docNumber++;
            }
        }
        writer.close();
        System.out.println("Add " + getReader().maxDoc() +  " documents successfully.");
    }

    /**
     * 获取所有文档的tf-idf值
     */
    public ArrayList<Feature> getAllFeatures() throws IOException {
        ArrayList<Feature> features = new ArrayList<>();
        reader = getReader();

        //每一个文档的tf-idf
        DefaultSimilarity similarity = new DefaultSimilarity();
        for (int k = 0; k < docNumber; k++) {
            Feature feature = new Feature();
            Map<String, Double> wordMap = new HashMap<String, Double>();

            //获取当前文档的内容
            String conversation_id = reader.document(k).get("conversation_id");

            TermFreqVector terms = reader.getTermFreqVector(k, "history");
            String[] termString = terms.getTerms();
            int[] termFreq = terms.getTermFrequencies();
            DefaultSimilarity simi = new DefaultSimilarity();
            for (int i = 0; i < terms.size(); i++) {
                Term term = new Term("history", termString[i]);
                //System.out.println(termString[i] + "  tf: " + termFreq[i] + "  idf: " + reader.docFreq(term));
                double tf = simi.tf(termFreq[i]);
                double idf = simi.idf(reader.docFreq(term), docNumber);
                wordMap.put(termString[i], (tf * idf));
                //System.out.println(tf * idf);
            }

            feature.conversation_id = conversation_id;
            feature.tf_idf = wordMap;
            features.add(feature);
        }
        return features;
    }

    /**
     * 获取查找文本的tf-idf
     */
    public Map<String,Double> getSearchTextTfIdf(String query) throws IOException {
        String[] querySplit = query.split(" ");

        //统计每一个词，在文档中的数目
        Map<String,Integer> termFreqMap = new HashMap();
        for (String term : querySplit) {
            if (termFreqMap.get(term) == null) {
                termFreqMap.put(term,1);
                continue;
            }
            termFreqMap.put(term,termFreqMap.get(term) + 1);
        }

        Map<String, Double> scoreMap = new HashMap();
        DefaultSimilarity simi = new DefaultSimilarity();
        for (String termString: termFreqMap.keySet()) {
            Term term = new Term("history", termString);
            double tf = simi.tf(termFreqMap.get(termString));
            double idf = simi.idf(reader.docFreq(term), docNumber);
            //System.out.println(tf * idf);
            scoreMap.put(termString, (tf * idf));
        }

        return scoreMap;
    }

}
