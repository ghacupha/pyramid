package edu.neu.ccs.pyramid.dataset;

import edu.neu.ccs.pyramid.configuration.Config;
import org.apache.mahout.math.Vector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by chengli on 8/19/14.
 */
public class TRECFormat {
    /**
     * internally, trecFile is a directory with 5 files in it
     * one for sparse feature matrix, one for config,
     * one for data settings, one for feature settings,
     * one for dataset setting
     */
    private static final String TREC_MATRIX_FILE_NAME = "feature_matrix.txt";
    private static final String TREC_CONFIG_FILE_NAME = "config.txt";
    private static final String TREC_CONFIG_NUM_DATA_POINTS = "numDataPoints";
    private static final String TREC_CONFIG_NUM_FEATURES = "numFeatures";
    private static final String TREC_CONFIG_NUM_CLASSES = "numClasses";
    private static final String TREC_CONFIG_MISSING_VALUE = "missingValue";
    private static final String TREC_DATA_POINT_SETTINGS_FILE_NAME = "data_point_settings.ser";
    private static final String TREC_FEATURE_SETTINGS_FILE_NAME = "feature_settings.ser";
    private static final String TREC_DATASET_SETTINGS_FILE_NAME = "dataset_settings.ser";

    public static void save(ClfDataSet dataSet, String trecFile){
        save(dataSet,new File(trecFile));
    }

    public static void save(RegDataSet dataSet, String trecFile){
        save(dataSet,new File(trecFile));
    }

    public static void save(MultiLabelClfDataSet dataSet, String trecFile){
        save(dataSet, new File(trecFile));
    }

    public static void save(ClfDataSet dataSet, File trecFile){
        if (!trecFile.exists()){
            trecFile.mkdirs();
        }
        writeMatrixFile(dataSet, trecFile);
        writeConfigFile(dataSet, trecFile);
        writeDataPointSettings(dataSet,trecFile);
        writeFeatureSettings(dataSet,trecFile);
        writeDataSetSetting(dataSet,trecFile);
    }

    public static void save(MultiLabelClfDataSet dataSet, File trecFile){
        if (!trecFile.exists()){
            trecFile.mkdirs();
        }
        writeMatrixFile(dataSet, trecFile);
        writeConfigFile(dataSet, trecFile);
        writeDataPointSettings(dataSet,trecFile);
        writeFeatureSettings(dataSet,trecFile);
        writeDataSetSetting(dataSet,trecFile);
    }

    public static void save(RegDataSet dataSet, File trecFile) {
        if (!trecFile.exists()){
            trecFile.mkdirs();
        }
        writeMatrixFile(dataSet, trecFile);
        writeConfigFile(dataSet, trecFile);
        writeDataPointSettings(dataSet,trecFile);
        writeFeatureSettings(dataSet,trecFile);
        writeDataSetSetting(dataSet,trecFile);

    }

    public static ClfDataSet loadClfDataSet(String trecFile, DataSetType dataSetType,
                                            boolean loadSettings) throws IOException, ClassNotFoundException {
        return loadClfDataSet(new File(trecFile),dataSetType, loadSettings);
    }

    public static RegDataSet loadRegDataSet(String trecFile, DataSetType dataSetType,
                                            boolean loadSettings) throws IOException, ClassNotFoundException {
        return loadRegDataSet(new File(trecFile),dataSetType, loadSettings);
    }

    public static MultiLabelClfDataSet loaMultiLabelClfDataSet(String trecFile, DataSetType dataSetType,
                                            boolean loadSettings) throws IOException, ClassNotFoundException {
        return loadMultiLabelClfDataSet(new File(trecFile),dataSetType, loadSettings);
    }


    public static ClfDataSet loadClfDataSet(File trecFile, DataSetType dataSetType,
                                            boolean loadSettings) throws IOException, ClassNotFoundException {
        boolean legalArg = ((dataSetType == DataSetType.CLF_DENSE)
                ||(dataSetType==DataSetType.CLF_SPARSE));
        if (!legalArg){
            throw new IllegalArgumentException("illegal data set type");
        }
        int numDataPoints = parseNumDataPoints(trecFile);
        int numFeatures = parseNumFeaturess(trecFile);
        int numClasses = parseNumClasses(trecFile);
        boolean missingValue = parseMissingValue(trecFile);
        ClfDataSet clfDataSet = null;
        if (dataSetType==DataSetType.CLF_DENSE){
            clfDataSet = new DenseClfDataSet(numDataPoints,numFeatures,missingValue,numClasses);
        }
        if (dataSetType==DataSetType.CLF_SPARSE){
            clfDataSet = new SparseClfDataSet(numDataPoints,numFeatures,missingValue,numClasses);
        }
        fillClfDataSet(clfDataSet,trecFile);
        if (loadSettings){
            loadDataPointSettings(clfDataSet,trecFile);
            loadFeatureSettings(clfDataSet,trecFile);
            loadDataSetSetting(clfDataSet,trecFile);
        }

        return clfDataSet;
    }

    public static MultiLabelClfDataSet loadMultiLabelClfDataSet(File trecFile, DataSetType dataSetType,
                                            boolean loadSettings) throws IOException, ClassNotFoundException {
        boolean legalArg = ((dataSetType == DataSetType.ML_CLF_DENSE)
                ||(dataSetType==DataSetType.ML_CLF_SPARSE));
        if (!legalArg){
            throw new IllegalArgumentException("illegal data set type");
        }
        int numDataPoints = parseNumDataPoints(trecFile);
        int numFeatures = parseNumFeaturess(trecFile);
        int numClasses = parseNumClasses(trecFile);
        boolean missingValue = parseMissingValue(trecFile);
        MultiLabelClfDataSet dataSet = null;
        if (dataSetType==DataSetType.ML_CLF_DENSE){
            dataSet = new DenseMLClfDataSet(numDataPoints,numFeatures,missingValue,numClasses);
        }
        if (dataSetType==DataSetType.ML_CLF_SPARSE){
            dataSet = new SparseMLClfDataSet(numDataPoints,numFeatures,missingValue,numClasses);
        }
        fillMultiLabelClfDataSet(dataSet,trecFile);
        if (loadSettings){
            loadDataPointSettings(dataSet,trecFile);
            loadFeatureSettings(dataSet,trecFile);
            loadDataSetSetting(dataSet,trecFile);
        }

        return dataSet;
    }

    public static RegDataSet loadRegDataSet(File trecFile, DataSetType dataSetType,
                                            boolean loadSettings) throws IOException, ClassNotFoundException {
        boolean legalArg = ((dataSetType == DataSetType.REG_DENSE)
                ||(dataSetType==DataSetType.REG_SPARSE));
        if (!legalArg){
            throw new IllegalArgumentException("illegal data set type");
        }
        int numDataPoints = parseNumDataPoints(trecFile);
        int numFeatures = parseNumFeaturess(trecFile);
        boolean missingValue = parseMissingValue(trecFile);
        RegDataSet regDataSet = null;
        if (dataSetType==DataSetType.REG_DENSE){
            regDataSet = new DenseRegDataSet(numDataPoints,numFeatures,missingValue);
        }
        if (dataSetType==DataSetType.REG_SPARSE){
            regDataSet = new SparseRegDataSet(numDataPoints,numFeatures,missingValue);
        }
        fillRegDataSet(regDataSet, trecFile);
        if (loadSettings){
            loadDataPointSettings(regDataSet,trecFile);
            loadFeatureSettings(regDataSet,trecFile);
            loadDataSetSetting(regDataSet,trecFile);
        }

        return regDataSet;
    }

//    public static RankDataSet loadRankDataSet(File trecFile, DataSetType dataSetType) throws Exception{
//        boolean legalArg = ((dataSetType == DataSetType.RANK_DENSE)
//                ||(dataSetType==DataSetType.RANK_SPARSE));
//        if (!legalArg){
//            throw new IllegalArgumentException("illegal data set type");
//        }
//        int numDataPoints = parseNumDataPoints(trecFile);
//        int numFeatures = parseNumFeaturess(trecFile);
//        RankDataSet rankDataSet = null;
//        if (dataSetType==DataSetType.RANK_DENSE){
//            rankDataSet = new DenseRankDataSet(numDataPoints,numFeatures);
//        }
//        if (dataSetType==DataSetType.RANK_SPARSE){
//            rankDataSet = new SparseRankDataSet(numDataPoints,numFeatures);
//        }
//        fillRankDataSet(rankDataSet,trecFile);
    //todo load settings
//        return rankDataSet;
//    }

    /**
     * parse comments in a TREC file
     * @param trecFile
     * @return
     * @throws Exception
     */
    public static List<String> parseComments(File trecFile) {
        List<String> comments = new ArrayList<>();
        File matrixFile = new File(trecFile, TREC_MATRIX_FILE_NAME);
        try (BufferedReader br = new BufferedReader(new FileReader(matrixFile));
        ) {
            String line = null;
            while ((line=br.readLine())!=null){
                //ignore the # sign itself
                int startPos = line.lastIndexOf("#")+1;
                int endPos = line.length();
                String comment = line.substring(startPos,endPos);
                System.out.println(comment);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return comments;
    }



    //==========PRIVATE==========

    private static int parseNumDataPoints(File trecFile) throws IOException {
        File configFile = new File(trecFile, TREC_CONFIG_FILE_NAME);
        int numDataPoints;
        try(
                BufferedReader br = new BufferedReader(new FileReader(configFile));
        ){
            Config config = new Config(configFile);
            numDataPoints = config.getInt(TREC_CONFIG_NUM_DATA_POINTS);
        }
        return numDataPoints;
    }

    private static int parseNumFeaturess(File trecFile) throws IOException {
        File configFile = new File(trecFile, TREC_CONFIG_FILE_NAME);
        int numFeatures;
        try(
                BufferedReader br = new BufferedReader(new FileReader(configFile));
        ){
            Config config = new Config(configFile);
            numFeatures = config.getInt(TREC_CONFIG_NUM_FEATURES);
        }
        return numFeatures;
    }

    private static int parseNumClasses(File trecFile) throws IOException {
        File configFile = new File(trecFile, TREC_CONFIG_FILE_NAME);
        int numClasses;
        try(
                BufferedReader br = new BufferedReader(new FileReader(configFile));
        ){
            Config config = new Config(configFile);
            numClasses = config.getInt(TREC_CONFIG_NUM_CLASSES);
        }
        return numClasses;
    }

    private static boolean parseMissingValue(File trecFile) throws IOException {
        File configFile = new File(trecFile, TREC_CONFIG_FILE_NAME);
        boolean missingValue;
        try(
                BufferedReader br = new BufferedReader(new FileReader(configFile));
        ){
            Config config = new Config(configFile);
            missingValue = config.getBoolean(TREC_CONFIG_MISSING_VALUE);
        }
        return missingValue;
    }

    private static void fillClfDataSet(ClfDataSet dataSet, File trecFile) throws IOException {
        File matrixFile = new File(trecFile, TREC_MATRIX_FILE_NAME);
        try (BufferedReader br = new BufferedReader(new FileReader(matrixFile));
        ) {
            String line = null;
            int dataIndex = 0;
            while ((line=br.readLine())!=null){
                String[] lineSplit = line.split(" ");
                int label = Integer.parseInt(lineSplit[0]);
                dataSet.setLabel(dataIndex,label);
                for (int i=1;i<lineSplit.length;i++){
                    String pair = lineSplit[i];
                    // ignore things after #
                    if (pair.startsWith("#")){
                        break;
                    }
                    String[] pairSplit = pair.split(":");
                    int featureIndex = Integer.parseInt(pairSplit[0]);
                    double featureValue = Double.parseDouble(pairSplit[1]);
                    dataSet.setFeatureValue(dataIndex, featureIndex,featureValue);
                }
                dataIndex += 1;
            }
        }
    }

    private static void fillMultiLabelClfDataSet(MultiLabelClfDataSet dataSet, File trecFile) throws IOException {
        File matrixFile = new File(trecFile, TREC_MATRIX_FILE_NAME);
        try (BufferedReader br = new BufferedReader(new FileReader(matrixFile));
        ) {
            String line = null;
            int dataIndex = 0;
            while ((line=br.readLine())!=null){
                String[] lineSplit = line.split(" ");
                String multiLabelString = lineSplit[0];
                String[] multiLabelSplit = multiLabelString.split(Pattern.quote(","));
                for (String label: multiLabelSplit){
                    dataSet.addLabel(dataIndex,Integer.parseInt(label));
                }
                for (int i=1;i<lineSplit.length;i++){
                    String pair = lineSplit[i];
                    // ignore things after #
                    if (pair.startsWith("#")){
                        break;
                    }
                    String[] pairSplit = pair.split(":");
                    int featureIndex = Integer.parseInt(pairSplit[0]);
                    double featureValue = Double.parseDouble(pairSplit[1]);
                    dataSet.setFeatureValue(dataIndex, featureIndex,featureValue);
                }
                dataIndex += 1;
            }
        }
    }


    private static void fillRegDataSet(RegDataSet dataSet, File trecFile) throws IOException {
        File matrixFile = new File(trecFile, TREC_MATRIX_FILE_NAME);
        try (BufferedReader br = new BufferedReader(new FileReader(matrixFile));
        ) {
            String line = null;
            int dataIndex = 0;
            while ((line=br.readLine())!=null){
                String[] lineSplit = line.split(" ");
                double label = Double.parseDouble(lineSplit[0]);
                dataSet.setLabel(dataIndex,label);
                for (int i=1;i<lineSplit.length;i++){
                    String pair = lineSplit[i];
                    // ignore things after #
                    if (pair.startsWith("#")){
                        break;
                    }
                    String[] pairSplit = pair.split(":");
                    int featureIndex = Integer.parseInt(pairSplit[0]);
                    double featureValue = Double.parseDouble(pairSplit[1]);
                    dataSet.setFeatureValue(dataIndex, featureIndex,featureValue);
                }
                dataIndex += 1;
            }
        }
    }

    private static void fillRankDataSet(RankDataSet dataSet, File trecFile) throws IOException {
        File matrixFile = new File(trecFile, TREC_MATRIX_FILE_NAME);
        try (BufferedReader br = new BufferedReader(new FileReader(matrixFile));
        ) {
            String line = null;
            int dataIndex = 0;
            while ((line=br.readLine())!=null){
                String[] lineSplit = line.split(" ");
                int label = Integer.parseInt(lineSplit[0]);
                dataSet.setLabel(dataIndex,label);
                for (int i=1;i<lineSplit.length;i++){
                    String pair = lineSplit[i];
                    // ignore things after #
                    if (pair.startsWith("#")){
                        break;
                    }
                    String[] pairSplit = pair.split(":");
                    int featureIndex = Integer.parseInt(pairSplit[0]);
                    double featureValue = Double.parseDouble(pairSplit[1]);
                    dataSet.setFeatureValue(dataIndex, featureIndex,featureValue);
                }
                dataIndex += 1;
            }
        }
    }



    private static void writeConfigFile(ClfDataSet dataSet, File trecFile) {
        File configFile = new File(trecFile, TREC_CONFIG_FILE_NAME);
        Config config = new Config();
        config.setInt(TREC_CONFIG_NUM_DATA_POINTS,dataSet.getNumDataPoints());
        config.setInt(TREC_CONFIG_NUM_FEATURES,dataSet.getNumFeatures());
        config.setInt(TREC_CONFIG_NUM_CLASSES,dataSet.getNumClasses());
        config.setBoolean(TREC_CONFIG_MISSING_VALUE,dataSet.hasMissingValue());
        try {
            config.store(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeConfigFile(MultiLabelClfDataSet dataSet, File trecFile) {
        File configFile = new File(trecFile, TREC_CONFIG_FILE_NAME);
        Config config = new Config();
        config.setInt(TREC_CONFIG_NUM_DATA_POINTS,dataSet.getNumDataPoints());
        config.setInt(TREC_CONFIG_NUM_FEATURES,dataSet.getNumFeatures());
        config.setInt(TREC_CONFIG_NUM_CLASSES,dataSet.getNumClasses());
        config.setBoolean(TREC_CONFIG_MISSING_VALUE,dataSet.hasMissingValue());
        try {
            config.store(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeConfigFile(RegDataSet dataSet, File trecFile) {
        File configFile = new File(trecFile, TREC_CONFIG_FILE_NAME);
        Config config = new Config();
        config.setInt(TREC_CONFIG_NUM_DATA_POINTS,dataSet.getNumDataPoints());
        config.setInt(TREC_CONFIG_NUM_FEATURES,dataSet.getNumFeatures());
        config.setBoolean(TREC_CONFIG_MISSING_VALUE,dataSet.hasMissingValue());
        try {
            config.store(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeConfigFile(RankDataSet dataSet, File trecFile) {
        File configFile = new File(trecFile, TREC_CONFIG_FILE_NAME);
        Config config = new Config();
        config.setInt(TREC_CONFIG_NUM_DATA_POINTS,dataSet.getNumDataPoints());
        config.setInt(TREC_CONFIG_NUM_FEATURES,dataSet.getNumFeatures());
        config.setBoolean(TREC_CONFIG_MISSING_VALUE,dataSet.hasMissingValue());
        try {
            config.store(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeMatrixFile(ClfDataSet dataSet, File trecFile) {
        File matrixFile = new File(trecFile, TREC_MATRIX_FILE_NAME);
        int numDataPoints = dataSet.getNumDataPoints();
        int[] labels = dataSet.getLabels();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(matrixFile));
        ) {
            for (int i=0;i<numDataPoints;i++){
                int label = labels[i];
                bw.write(label+" ");
                Vector vector = dataSet.getRow(i);
                // only write non-zeros
                for (Vector.Element element: vector.nonZeroes()){
                    int featureIndex = element.index();
                    double featureValue = element.get();
                    bw.write(featureIndex+":"+featureValue+" ");
                }
                bw.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeMatrixFile(RegDataSet dataSet, File trecFile) {
        File matrixFile = new File(trecFile, TREC_MATRIX_FILE_NAME);
        int numDataPoints = dataSet.getNumDataPoints();
        double[] labels = dataSet.getLabels();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(matrixFile));
        ) {
            for (int i=0;i<numDataPoints;i++){
                double label = labels[i];
                bw.write(label+" ");
                Vector vector = dataSet.getRow(i);
                // only write non-zeros
                for (Vector.Element element: vector.nonZeroes()){
                    int featureIndex = element.index();
                    double featureValue = element.get();
                    bw.write(featureIndex+":"+featureValue+" ");
                }
                bw.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeMatrixFile(MultiLabelClfDataSet dataSet, File trecFile) {
        File matrixFile = new File(trecFile, TREC_MATRIX_FILE_NAME);
        int numDataPoints = dataSet.getNumDataPoints();
        MultiLabel[] multiLabels = dataSet.getMultiLabels();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(matrixFile));
        ) {
            for (int i=0;i<numDataPoints;i++){
                MultiLabel multiLabel = multiLabels[i];
                List<Integer> labels = multiLabel.getMatchedLabels().stream().sorted().collect(Collectors.toList());
                for (int l=0;l<labels.size();l++){
                    bw.write(labels.get(l).toString());
                    if (l!=labels.size()-1){
                        bw.write(",");
                    } else {
                        bw.write(" ");
                    }
                }
                Vector vector = dataSet.getRow(i);
                // only write non-zeros
                for (Vector.Element element: vector.nonZeroes()){
                    int featureIndex = element.index();
                    double featureValue = element.get();
                    bw.write(featureIndex+":"+featureValue+" ");
                }
                bw.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeDataPointSettings(ClfDataSet dataSet, File trecFile) {
        File file = new File(trecFile, TREC_DATA_POINT_SETTINGS_FILE_NAME);
        List<ClfDataPointSetting> dataPointSettings = new ArrayList<>();
        for (int i=0;i<dataSet.getNumDataPoints();i++){
            dataPointSettings.add(dataSet.getDataPointSetting(i));
        }
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
        ){
            objectOutputStream.writeObject(dataPointSettings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeDataPointSettings(RegDataSet dataSet, File trecFile) {
        File file = new File(trecFile, TREC_DATA_POINT_SETTINGS_FILE_NAME);
        List<RegDataPointSetting> dataPointSettings = new ArrayList<>();
        for (int i=0;i<dataSet.getNumDataPoints();i++){
            dataPointSettings.add(dataSet.getDataPointSetting(i));
        }
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
        ){
            objectOutputStream.writeObject(dataPointSettings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeDataPointSettings(MultiLabelClfDataSet dataSet, File trecFile) {
        File file = new File(trecFile, TREC_DATA_POINT_SETTINGS_FILE_NAME);
        List<MLClfDataPointSetting> dataPointSettings = new ArrayList<>();
        for (int i=0;i<dataSet.getNumDataPoints();i++){
            dataPointSettings.add(dataSet.getDataPointSetting(i));
        }
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
        ){
            objectOutputStream.writeObject(dataPointSettings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void writeFeatureSettings(DataSet dataSet, File trecFile) {
        File file = new File(trecFile,TREC_FEATURE_SETTINGS_FILE_NAME);
        List<FeatureSetting> featureSettings = new ArrayList<>();
        for (int i=0;i<dataSet.getNumFeatures();i++){
            featureSettings.add(dataSet.getFeatureSetting(i));
        }
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
        ){
            objectOutputStream.writeObject(featureSettings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeDataSetSetting(ClfDataSet dataSet, File trecFile){
        File file = new File(trecFile,TREC_DATASET_SETTINGS_FILE_NAME);

        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
        ){
            objectOutputStream.writeObject(dataSet.getSetting());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeDataSetSetting(RegDataSet dataSet, File trecFile){
        File file = new File(trecFile,TREC_DATASET_SETTINGS_FILE_NAME);

        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
        ){
            objectOutputStream.writeObject(dataSet.getSetting());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeDataSetSetting(MultiLabelClfDataSet dataSet, File trecFile){
        File file = new File(trecFile,TREC_DATASET_SETTINGS_FILE_NAME);

        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
        ){
            objectOutputStream.writeObject(dataSet.getSetting());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadDataPointSettings(ClfDataSet dataSet, File trecFile) throws IOException, ClassNotFoundException {
        File file = new File(trecFile, TREC_DATA_POINT_SETTINGS_FILE_NAME);
        ArrayList<ClfDataPointSetting> dataPointSettings;
        try(
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
        ){
            dataPointSettings = (ArrayList)objectInputStream.readObject();

        }
        for (int i=0;i<dataSet.getNumDataPoints();i++){
            dataSet.putDataPointSetting(i, dataPointSettings.get(i));
        }
    }

    private static void loadDataPointSettings(RegDataSet dataSet, File trecFile) throws IOException, ClassNotFoundException {
        File file = new File(trecFile, TREC_DATA_POINT_SETTINGS_FILE_NAME);
        ArrayList<RegDataPointSetting> dataPointSettings;
        try(
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
        ){
            dataPointSettings = (ArrayList)objectInputStream.readObject();

        }
        for (int i=0;i<dataSet.getNumDataPoints();i++){
            dataSet.putDataPointSetting(i, dataPointSettings.get(i));
        }
    }

    private static void loadDataPointSettings(MultiLabelClfDataSet dataSet, File trecFile) throws IOException, ClassNotFoundException {
        File file = new File(trecFile, TREC_DATA_POINT_SETTINGS_FILE_NAME);
        ArrayList<MLClfDataPointSetting> dataPointSettings;
        try(
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
        ){
            dataPointSettings = (ArrayList)objectInputStream.readObject();

        }
        for (int i=0;i<dataSet.getNumDataPoints();i++){
            dataSet.putDataPointSetting(i, dataPointSettings.get(i));
        }
    }

    private static void loadFeatureSettings(DataSet dataSet, File trecFile) throws IOException, ClassNotFoundException {
        File file = new File(trecFile,TREC_FEATURE_SETTINGS_FILE_NAME);
        ArrayList<FeatureSetting> featureSettings;
        try(
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
        ){
            featureSettings = (ArrayList)objectInputStream.readObject();

        }
        for (int i=0;i<dataSet.getNumFeatures();i++){
            dataSet.putFeatureSetting(i,featureSettings.get(i));
        }
    }

    private static void loadDataSetSetting(ClfDataSet dataSet, File trecFile) throws IOException, ClassNotFoundException {
        File file = new File(trecFile,TREC_DATASET_SETTINGS_FILE_NAME);
        ClfDataSetSetting dataSetSetting;
        try(    FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
        ){
            dataSetSetting = (ClfDataSetSetting)objectInputStream.readObject();
        }
        dataSet.putDataSetSetting(dataSetSetting);
    }

    private static void loadDataSetSetting(RegDataSet dataSet, File trecFile) throws IOException, ClassNotFoundException {
        File file = new File(trecFile,TREC_DATASET_SETTINGS_FILE_NAME);
        RegDataSetSetting dataSetSetting;
        try(    FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
        ){
            dataSetSetting = (RegDataSetSetting)objectInputStream.readObject();
        }
        dataSet.putDataSetSetting(dataSetSetting);
    }

    private static void loadDataSetSetting(MultiLabelClfDataSet dataSet, File trecFile) throws IOException, ClassNotFoundException {
        File file = new File(trecFile,TREC_DATASET_SETTINGS_FILE_NAME);
        MLClfDataSetSetting dataSetSetting;
        try(    FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
        ){
            dataSetSetting = (MLClfDataSetSetting)objectInputStream.readObject();
        }
        dataSet.putDataSetSetting(dataSetSetting);
    }
}
