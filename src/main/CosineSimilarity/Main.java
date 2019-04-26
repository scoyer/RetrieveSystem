package main.CosineSimilarity;

import java.io.IOException;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        DocIndexer docIndexer = new DocIndexer();
        docIndexer.index();

        long start = System.currentTimeMillis();
        search_with_MultiThread(docIndexer, Configuration.QUERY_FILE, Configuration.SAVE_FILE, 1);
        //search(docIndexer, Configuration.QUERY_FILE, Configuration.SAVE_FILE, 1);
        long end = System.currentTimeMillis();
        System.out.println((end - start) / 1000);
    }

    public static void search(DocIndexer docIndexer, String queryPath, String savePath, int topk) throws IOException {
        //get query strings
        ArrayList<String> queryStrings = Util.fileReader(new File(queryPath));

        //get all tf-idf features
        ArrayList<Feature> features = docIndexer.getAllFeatures();

        MultiThread thread = new MultiThread(docIndexer, queryStrings.subList(0,1), features, savePath, topk);
        thread.run();
    }

    public static void search_with_MultiThread(DocIndexer docIndexer, String queryPath, String savePath, int topk) throws IOException, InterruptedException, ExecutionException {
        //get query strings
        ArrayList<String> queryStrings = Util.fileReader(new File(queryPath));

        //get all tf-idf features
        ArrayList<Feature> features = docIndexer.getAllFeatures();

        //for(int i = 0 ; i < total_thread ;i++){
        //    int begin_idx = size_each_thread * i;
        //    int end_idx = Math.min(size_each_thread * (i + 1), queryString.size());
        //    results.add(exec.submit(new MultiThread(docIndexer, queryString.subList(begin_idx, end_idx), features, topk)));
        //}
        //System.out.println("done");
        //exec.shutdown();
        int sizeEachThread = queryStrings.size() / Configuration.THREAD_NUMBER;
        Thread[] threads = new Thread[Configuration.THREAD_NUMBER];

        ArrayList<String> savePathList = new ArrayList<>();
        for (int i = 0; i < Configuration.THREAD_NUMBER; i++) {
            int begin_idx = sizeEachThread * i;
            int end_idx = Math.min(sizeEachThread * (i + 1), queryStrings.size());

            String savePathEach = savePath + "_part" + i;
            savePathList.add(savePathEach);

            MultiThread thread = new MultiThread(docIndexer, queryStrings.subList(begin_idx, end_idx), features, savePathEach, topk);
            threads[i] = new Thread(thread);
            threads[i].start();
        }
        for (int i = 0; i < Configuration.THREAD_NUMBER; i++) {
            threads[i].join();
        }
        Util.mergeFile(savePathList, savePath);
    }

}

class MultiThread implements Runnable {
    private DocIndexer docIndexer;
    private List<String> queryString;
    private ArrayList<Feature> features;
    private String savePath;
    private  int topk;

    MultiThread(DocIndexer doc, List<String> query, ArrayList<Feature> features, String savePath, int topk) {
        this.docIndexer = doc;
        this.queryString = query;
        this.features = features;
        this.topk = topk;
        this.savePath = savePath;
    }

    @Override
    public void run() {
        try {
            ArrayList<String> result = new ArrayList<>();
            for (String dialog: queryString) {
                String[] split_words = dialog.split("\t");
                String query = split_words[0];
                String gt = split_words[1];
                String id = split_words[3];

                query = query.replaceAll("[\\p{P}+~$`^=|<>～｀＄＾＋＝｜＜＞￥×]", "");
                query = query.replaceAll("\\t|\\r|\\n", "");
                List<String> strings = Arrays.asList(query.split(" "));
                ArrayList<Double> score = new ArrayList<>();
                ArrayList<Integer> coresponding_id = new ArrayList<>();

                Map<String, Double> query_tfidf = docIndexer.getSearchTextTfIdf(query);
                String str = split_words[0];
                int idx = str.lastIndexOf(":");

                for (int i = 0; i < features.size(); i++) {
                    Feature feature = features.get(i);
                    if (id.equals(feature.conversation_id))
                        continue;
                    score.add(Util.cosineSimilarity(query_tfidf, feature.tf_idf));
                    System.out.println(Util.cosineSimilarity(query_tfidf, feature.tf_idf));
                    coresponding_id.add(i);
                }

                for (int i = 0; i < topk; i++) {
                    double best_score = -1;
                    int best_id = 0;
                    for (int j = 0; j < score.size(); j++) {
                        if (best_score < score.get(j)) {
                            best_score = score.get(j);
                            best_id = j;
                        }
                    }
                    int feature_id = coresponding_id.get(best_id);
                    try {
                        String response = docIndexer.getReader().document(feature_id).get("response");
                        String conversation_id = docIndexer.getReader().document(feature_id).get("conversation_id");
                        result.add(response);
                        //System.out.println("query: " + query + "   " + "response:" + response + " score:" + score);
                    } catch (IOException ex) {
                        //do nothing
                    }
                    //System.out.println("gt_response:" + gt + "  pred_response:" + response);
                    //System.out.println("gt_conversation_id:" + id + "  pred_conversation_id:" + conversation_id);
                    //record.write("top" + Integer.toString(i) +  " gt_response:" + gt + "  pred_response:" + response + "score:" + best_score + "\n");

                    score.remove(best_id);
                    coresponding_id.remove(best_id);

                    if (score.size() == 0) {
                        break;
                    }
                }
            }
            Util.writeFile(result, savePath);
        }
        catch (IOException ex) {

        }
    }
}