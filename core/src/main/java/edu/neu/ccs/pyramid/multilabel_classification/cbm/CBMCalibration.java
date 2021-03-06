package edu.neu.ccs.pyramid.multilabel_classification.cbm;

import edu.neu.ccs.pyramid.configuration.Config;
import edu.neu.ccs.pyramid.dataset.DataSetType;
import edu.neu.ccs.pyramid.dataset.MultiLabel;
import edu.neu.ccs.pyramid.dataset.MultiLabelClfDataSet;
import edu.neu.ccs.pyramid.dataset.TRECFormat;
import edu.neu.ccs.pyramid.multilabel_classification.cbm.AccPredictor;
import edu.neu.ccs.pyramid.multilabel_classification.cbm.CBM;
import edu.neu.ccs.pyramid.multilabel_classification.imlgb.BucketInfo;
import edu.neu.ccs.pyramid.multilabel_classification.imlgb.IMLGradientBoosting;
import edu.neu.ccs.pyramid.regression.IsotonicRegression;
import edu.neu.ccs.pyramid.util.CalibrationDisplay;
import edu.neu.ccs.pyramid.util.Pair;
import edu.neu.ccs.pyramid.util.Serialization;

import java.io.File;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CBMCalibration {
    public static void main(String[] args) throws Exception{
        if (args.length !=1){
            throw new IllegalArgumentException("Please specify a properties file.");
        }

        Config config = new Config(args[0]);

        MultiLabelClfDataSet valid = TRECFormat.loadMultiLabelClfDataSet(config.getString("input.valid"), DataSetType.ML_CLF_SEQ_SPARSE,true);
        MultiLabelClfDataSet test = TRECFormat.loadMultiLabelClfDataSet(config.getString("input.test"), DataSetType.ML_CLF_SEQ_SPARSE,true);
        CBM cbm = (CBM) Serialization.deserialize(config.getString("input.cbm"));
        List<MultiLabel> support = (List) Serialization.deserialize(config.getString("input.support"));


//        System.out.println("cardinality based");

//        Map<Integer,IsotonicRegression> calibrations = cardinalityBased(cbm,support,valid);
//        System.out.println("calibration done");
        //todo
//        Map<Integer,IsotonicRegression> calibrations = (Map<Integer,IsotonicRegression>) Serialization.deserialize("/mnt/home/zhenming/cbm_cali/17/cbm_cali_by_card");
//        displayCardinalityBased(cbm, test,calibrations);
//        Serialization.serialize(calibrations,"/mnt/home/zhenming/cbm_cali/17/cbm_cali_by_card");


        System.out.println("calibrated probability");
        String out = config.getString("out");
        calibrated(cbm, support, valid, test, out);
//        System.out.println("uncalibrated probability");
//        uncalibrated(cbm,test);


    }


    private static void calibrated(CBM cbm, List<MultiLabel> support, MultiLabelClfDataSet valid, MultiLabelClfDataSet test, String out)throws Exception{
        IsotonicRegression isotonicRegression = trainIso(cbm, support, valid);
        Serialization.serialize(isotonicRegression, Paths.get(out,"calibration").toFile());
        AccPredictor accPredictor = new AccPredictor(cbm);
        accPredictor.setComponentContributionThreshold(0.001);
        Stream<Pair<Double,Integer>> stream = IntStream.range(0, test.getNumDataPoints()).parallel().mapToObj(i->{
            MultiLabel pre = accPredictor.predict(test.getRow(i));
            double pro = cbm.predictAssignmentProb(test.getRow(i),pre,0.001);
            int correct = 0;
            if (pre.equals(test.getMultiLabels()[i])){
                correct = 1;
            }
            Pair<Double,Integer>  pair = new Pair<>(pro,correct);
            return pair;
        });
        System.out.println(isotonicRegression.displayCalibrationResult(stream));
    }



    public static Map<Integer,IsotonicRegression> cardinalityBased(CBM cbm, List<MultiLabel> support, MultiLabelClfDataSet valid) {
        Map<Integer,IsotonicRegression> calibrations = new HashMap<>();
        AccPredictor accPredictor = new AccPredictor(cbm);
        accPredictor.setComponentContributionThreshold(0.001);
        Set<Integer> cardinalities = new HashSet<>();
        for (MultiLabel multiLabel: support){
            cardinalities.add(multiLabel.getNumMatchedLabels());
        }

        for (int cardinality: cardinalities){

            Stream<Pair<Double,Integer>> stream =  IntStream.range(0, valid.getNumDataPoints()).parallel()
                    .boxed().flatMap(i-> {
                        MultiLabel pre = accPredictor.predict(valid.getRow(i));
                        Set<MultiLabel> copy = new HashSet<>(support);
                        if (!copy.contains(pre)) {
                            copy.add(pre);
                        }
                        List<MultiLabel> candidate = new ArrayList<>(copy);
                        final List<MultiLabel>  filtered = candidate.stream().filter(a->a.getNumMatchedLabels()==cardinality).collect(Collectors.toList());
                        double[] probs = cbm.predictAssignmentProbs(valid.getRow(i), filtered, 0.001);

                        Stream<Pair<Double,Integer>> pairs = IntStream.range(0, filtered.size())
                                .mapToObj(a -> {
                                    Pair<Double, Integer> pair = new Pair<>();
                                    pair.setFirst(probs[a]);
                                    pair.setSecond(0);
                                    if (filtered.get(a).equals(valid.getMultiLabels()[i])) {
                                        pair.setSecond(1);
                                    }
                                    return pair;
                                });
                        return pairs;
                    });
            calibrations.put(cardinality, new IsotonicRegression(stream));
        }

        return calibrations;
    }


    public static void displayCardinalityBased(CBM cbm, MultiLabelClfDataSet test, Map<Integer,IsotonicRegression> calibrations){
        AccPredictor accPredictor = new AccPredictor(cbm);
        accPredictor.setComponentContributionThreshold(0.001);
        Stream<Pair<Double,Integer>> stream =  IntStream.range(0, test.getNumDataPoints()).parallel()
                .boxed().map(i-> {
                    MultiLabel pre = accPredictor.predict(test.getRow(i));
                    double pro = cbm.predictAssignmentProb(test.getRow(i),pre,0.001);
                    double calibrated = calibrations.get(pre.getNumMatchedLabels()).predict(pro);
                    int correct = 0;
                    if (pre.equals(test.getMultiLabels()[i])){
                        correct = 1;
                    }
                    Pair<Double,Integer>  pair = new Pair<>(calibrated,correct);
                    return pair;
                });

        System.out.println(CalibrationDisplay.displayCalibrationResult(stream));
    }


    private static void uncalibrated(CBM cbm, MultiLabelClfDataSet test){
        AccPredictor accPredictor = new AccPredictor(cbm);
        accPredictor.setComponentContributionThreshold(0.001);
        Stream<Pair<Double,Integer>> stream = IntStream.range(0, test.getNumDataPoints()).parallel().mapToObj(i->{
            MultiLabel pre = accPredictor.predict(test.getRow(i));
            double pro = cbm.predictAssignmentProb(test.getRow(i),pre,0.001);
            int correct = 0;
            if (pre.equals(test.getMultiLabels()[i])){
                correct = 1;
            }
            Pair<Double,Integer>  pair = new Pair<>(pro,correct);
            return pair;
        });

        System.out.println(display(stream));
    }

    private static IsotonicRegression trainIso(CBM cbm, List<MultiLabel> support, MultiLabelClfDataSet valid) {
        AccPredictor accPredictor = new AccPredictor(cbm);
        accPredictor.setComponentContributionThreshold(0.001);
        Stream<Pair<Double, Integer>> stream = IntStream.range(0, valid.getNumDataPoints()).parallel().boxed().
                flatMap(i -> {
                    MultiLabel pre = accPredictor.predict(valid.getRow(i));
                    Set<MultiLabel> copy = new HashSet<>(support);
                    if (!copy.contains(pre)) {
                        copy.add(pre);
                    }
                    List<MultiLabel> candidate = new ArrayList<>(copy);
                    double[] probs = cbm.predictAssignmentProbs(valid.getRow(i), candidate, 0.001);
                    Stream<Pair<Double, Integer>> pairs = IntStream.range(0, candidate.size()).mapToObj(c -> {
                        double pro = probs[c];
                        int correct = 0;
                        if (candidate.get(c).equals(valid.getMultiLabels()[i])) {
                            correct = 1;
                        }
                        return new Pair<Double, Integer>(pro, correct);
                    });
                    return pairs;
                });
        IsotonicRegression isotonicRegression = new IsotonicRegression(stream);
        return isotonicRegression;
    }



//    private static IsotonicRegression trainIso(CBM cbm, List<MultiLabel> support, MultiLabelClfDataSet valid) {
//        AccPredictor accPredictor = new AccPredictor(cbm);
//        Stream<Pair<Double, Integer>> stream = IntStream.range(0, valid.getNumDataPoints()).parallel().boxed().
//                map(i-> {
//                    MultiLabel pre = accPredictor.predict(valid.getRow(i));
//                    double pro = cbm.predictAssignmentProb(valid.getRow(i),pre);
//                    Pair<Double,Integer> pair = new Pair<>();
//                    pair.setFirst(pro);
//                    pair.setSecond(0);
//                    if (pre.equals(valid.getMultiLabels()[i])){
//                        pair.setSecond(1);
//                    }
//                    return pair;
//                }
//                );
//        IsotonicRegression isotonicRegression = new IsotonicRegression(stream);
//        return isotonicRegression;
//    }

    public static String display(Stream<Pair<Double, Integer>> stream){
        final int numBuckets = 10;
        double bucketLength = 1.0/numBuckets;

        BucketInfo empty = new BucketInfo(numBuckets);
        BucketInfo total;
        total = stream.map(doubleIntegerPair -> {
            double probs = doubleIntegerPair.getFirst();
            double[] sum = new double[numBuckets];
            double[] sumProbs = new double[numBuckets];
            double[] count = new double[numBuckets];
            int index = (int)Math.floor(probs/bucketLength);
            if (index<0){
                index=0;
            }
            if (index>=numBuckets){
                index = numBuckets-1;
            }
            count[index] += 1;
            sumProbs[index] += probs;
            sum[index]+=doubleIntegerPair.getSecond();
            return new BucketInfo(count, sum,sumProbs);
        }).collect(()->new BucketInfo(numBuckets), BucketInfo::addAll, BucketInfo::addAll);
        double[] counts = total.getCounts();
        double[] correct = total.getSums();
        double[] sumProbs = total.getSumProbs();
        double[] accs = new double[counts.length];
        double[] average_confidence = new double[counts.length];

        for (int i = 0; i < counts.length; i++) {
            accs[i] = correct[i] / counts[i];
        }
        for (int j = 0; j < counts.length; j++) {
            average_confidence[j] = sumProbs[j] / counts[j];
        }

        DecimalFormat decimalFormat = new DecimalFormat("#0.0000");
        StringBuilder sb = new StringBuilder();
        sb.append("interval\t\t").append("total\t\t").append("correct\t\t").append("incorrect\t\t").append("accuracy\t\t").append("average confidence\n");
        for (int i = 0; i < 10; i++) {
            sb.append("[").append(decimalFormat.format(i * 0.1)).append(",")
                    .append(decimalFormat.format((i + 1) * 0.1)).append("]")
                    .append("\t\t").append(counts[i]).append("\t\t").append(correct[i]).append("\t\t")
                    .append(counts[i] - correct[i]).append("\t\t").append(decimalFormat.format(accs[i])).append("\t\t")
                    .append(decimalFormat.format(average_confidence[i])).append("\n");

        }

        String result = sb.toString();
        return result;

    }
}
