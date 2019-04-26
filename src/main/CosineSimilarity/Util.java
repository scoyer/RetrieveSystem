package main.CosineSimilarity;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class Util {
    /**
    读文本文件，返回一个list的串
     */
    public static ArrayList<String> fileReader(File readFile)
    {
        ArrayList<String> resultStrings = new ArrayList<>();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(readFile));

            String line = null;
            while ((line = reader.readLine()) != null)
            {
                resultStrings.add(line);
            }
            reader.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
        }

        return resultStrings;
    }

    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); //删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
    //删除指定文件夹下所有文件
    //param path 文件夹完整绝对路径
     */
    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            //如果不存在文件夹则创建一个
            file.mkdir();
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);//先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]);//再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 计算余弦相似度
     * @param searchTextTfIdfMap 查找文本的向量
     * @param docScoreMap 所有文本向量
     * @return 计算出当前查询文本与所有文本的相似度
     */
    public static Double cosineSimilarity(Map<String, Double> searchTextTfIdfMap, Map<String, Double> docScoreMap)
    {
        //key是相似的文档名称，value是与当前文档的相似度
        // Map<String,Double> similarityMap = new HashMap<>();

        //计算查找文本向量绝对值
        double searchValue = 0;
        for (Map.Entry<String, Double> entry : searchTextTfIdfMap.entrySet())
        {
            searchValue += entry.getValue() * entry.getValue();
        }

        //for (Map.Entry<String, Map<String, Double>> docEntry : allTfIdfMap.entrySet())
        //{
        //String docName = docEntry.getKey();
        //Map<String, Double> docScoreMap = docEntry.getValue();

        double termValue = 0;
        double acrossValue = 0;
        for (Map.Entry<String, Double> termEntry : docScoreMap.entrySet())
        {
            if (searchTextTfIdfMap.get(termEntry.getKey()) != null)
            {
                acrossValue += termEntry.getValue() * searchTextTfIdfMap.get(termEntry.getKey());
            }

            termValue += termEntry.getValue() * termEntry.getValue();
        }

        return acrossValue/(Math.sqrt(termValue) * Math.sqrt(searchValue));
    }

    /**
     * 合并文件
     */
    public static void mergeFile(ArrayList<String> src_path, String savePath) throws IOException{
        ArrayList<String> total_result = new ArrayList();
        for (String src: src_path) {
            File src_file = new File(src);
            total_result.addAll(fileReader(src_file));
            src_file.delete();
        }
        writeFile(total_result, savePath);
    }

    /**
     * BM25 Algorithm
     */

    public static double BM25(double idf, int docFreq, double k1, double b) {

    }


    /**
    将一串文本逐行写入文本文件
     */
    public  static void writeFile(ArrayList<String> result, String savePath) throws IOException {
        FileWriter record = new FileWriter(savePath);
        for (String s : result) {
            record.write(s + "\n");
        }
        record.close();
    }

}
