package org.sjtu.swhua.storm.MatchAlgorithm.bolt;

//import com.esotericsoftware.kryo.Kryo;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.sjtu.swhua.storm.MatchAlgorithm.DataStructure.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SimpleMatchBolt extends BaseRichBolt {
    private OutputToFile output;

    private HashMap<Integer, Subscription> mapIDtoSub;
    private OutputCollector collector;
    private StringBuilder log;
    private StringBuilder matchResult;
    private StringBuilder speedReport;
    private TopologyContext boltContext;

    private Integer numSubPacket;
    private Integer numEventPacket;
    static private Integer numSubInserted;
    static private Integer numEventMatched;
    //    private Integer numExecutor;
    private String boltName;

    private long runTime;
    //    private long insertSubTime;
//    private long matchEventTime;
    final private long beginTime;
    final private long intervalTime; // The interval between two calculations of speed
    private long speedTime;  // The time to calculate and record speed
    private long startTime;  // a temp variable

    public SimpleMatchBolt(String boltName) {
        this.boltName = boltName;
//        this.numExecutor=numExecutor;
        numSubPacket = 0;
        numEventPacket = 0;
        numSubInserted = 0;
        numEventMatched = 0;
//        insertSubTime = 1;// 1ns avoid divide by zero
//        matchEventTime = 1;
        runTime = 1;
        beginTime = System.nanoTime();
        intervalTime = 60000000000L;  // 1 minute
        speedTime = System.nanoTime() + intervalTime;
        startTime = 0;
    }

    @Override
    public void prepare(Map<String, Object> map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.boltContext = topologyContext;
        this.collector = outputCollector;
        output = new OutputToFile();
        mapIDtoSub = new HashMap<>();

        log = new StringBuilder();
        matchResult = new StringBuilder();
        speedReport = new StringBuilder();

        //boltContext.getThisTaskId
        try {
            log = new StringBuilder(boltName);
            log.append(" ThreadNum: " + Thread.currentThread().getName() + "\n" + boltName + " " + boltContext.getThisComponentId() + ":");
            List<Integer> taskIds = boltContext.getComponentTasks(boltContext.getThisComponentId());
            Iterator taskIdsIter = taskIds.iterator();
            while (taskIdsIter.hasNext())
                log.append(" " + String.valueOf(taskIdsIter.next()));
            log.append("\nThisTaskId: ");
            log.append(boltContext.getThisTaskId());  // Get the current thread number
            log.append("\n\n");
            output.otherInfo(log.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(Tuple tuple) {
// Solution A: catch Exception to find whether the tuple is a subPacket or a eventPacket
//        try {
//            ArrayList<Subscription> subPacket=(ArrayList<Subscription>) tuple.getValueByField("SubscriptionPacket");
//        }
//        catch (IllegalArgumentException e) {
//        }
//        try {
//         ArrayList<Event> eventPacket = (ArrayList<Event>) tuple.getValueByField("EventPacket");
//        }
//        catch (IllegalArgumentException e){
//        }

        // Solution B: get the operation type to find what the tuple is
        Integer threadNumber = boltContext.getThisTaskId();
        int type = (int) tuple.getValue(0);
        try {
            switch (type) {
                case TypeConstant.Insert_Subscription: {
                    startTime = System.nanoTime();
                    int subID;
                    numSubPacket++;
                    log = new StringBuilder(boltName);
                    log.append(" Thread ");
                    log.append(threadNumber);
                    log.append(": SubPacket ");
                    log.append(numSubPacket);
                    log.append(" is received.\n");
                    output.writeToLogFile(log.toString());
//                    output.writeToLogFile(boltName + ": SubPacket" + String.valueOf(numSubPacket) + " is received.\n");

                    ArrayList<Subscription> subPacket = (ArrayList<Subscription>) tuple.getValueByField("SubscriptionPacket");
                    for (int i = 0; i < subPacket.size(); i++) {
                        subID = subPacket.get(i).getSubID();
                        if(!mapIDtoSub.containsKey(subID))
                            numSubInserted++;
                        mapIDtoSub.put(subID, subPacket.get(i));
//                        System.out.println("\n\n\nSubscription " + String.valueOf(subID) + " is inserted." + "\n\n\n");
                        log = new StringBuilder(boltName);
                        log.append(" Thread ");
                        log.append(threadNumber);
                        log.append(": Sub ");
                        log.append(subID);
                        log.append(" is inserted.\n");
                        output.writeToLogFile(log.toString());
//                        output.writeToLogFile(boltName + ": Subscription " + String.valueOf(subID) + " is inserted.\n");
                    }
                    collector.ack(tuple);
//                    insertSubTime += System.nanoTime() - startTime;
                    break;
                }
                case TypeConstant.Insert_Attribute_Subscription: {
                    break;
                }
                case TypeConstant.Update_Attribute_Subscription: {
                    break;
                }
                case TypeConstant.Delete_Attribute_Subscription: {
                    break;
                }
                case TypeConstant.Delete_Subscription: {
                    break;
                }
                case TypeConstant.Event_Match_Subscription: {
                    startTime = System.nanoTime();
                    numEventPacket++;
                    log = new StringBuilder(boltName);
                    log.append(" Thread ");
                    log.append(threadNumber);
                    log.append(": EventPacket ");
                    log.append(numEventPacket);
                    log.append(" is received.\n");
                    output.writeToLogFile(log.toString());
//                    output.writeToLogFile(boltName + ": EventPacket" + String.valueOf(numEventPacket) + " is received.\n");
                    ArrayList<Event> eventPacket = (ArrayList<Event>) tuple.getValueByField("EventPacket");
                    for (int i = 0; i < eventPacket.size(); i++) {
                        int eventID = eventPacket.get(i).getEventID();
                        int matchNum = 0;
                        matchResult = new StringBuilder(boltName);
                        matchResult.append(" Thread ");
                        matchResult.append(threadNumber);
                        matchResult.append(" - EventID: ");
                        matchResult.append(eventID);
                        matchResult.append("; SubNum:");
                        matchResult.append(mapIDtoSub.size());
                        matchResult.append("; SubID:");
//                        String matchResult = boltName + " - EventID: " + String.valueOf(eventID) + "; SubNum:" + String.valueOf(mapIDtoSub.size()) + "; SubID:";

                        if (mapIDtoSub.size() == 0) {
                            log = new StringBuilder(boltName);
                            log.append(" Thread ");
                            log.append(threadNumber);
                            log.append(": EventID ");
                            log.append(eventID);
                            log.append(" matching task is done.\n");
                            output.writeToLogFile(log.toString());
//                            output.writeToLogFile(boltName + ": Event " + String.valueOf(eventID) + " matching task is done.\n");
                            matchResult.append(" ; MatchedSubNum: 0.\n");
                            output.saveMatchResult(matchResult.toString());
//                            output.saveMatchResult(matchResult + " ; MatchedSubNum: 0.\n");
                            continue;
                        }
//                        System.out.println("\n\n\n" + String.valueOf(eventID) + " begins to match." + "\n\n\n");

                        HashMap<Integer, Double> eventAttributeIDToValue = eventPacket.get(i).getAttributeIDToValue();
                        Iterator<HashMap.Entry<Integer, Subscription>> subIterator = mapIDtoSub.entrySet().iterator();

                        while (subIterator.hasNext()) {
                            HashMap.Entry<Integer, Subscription> subEntry = subIterator.next();
                            Integer subID = subEntry.getKey();
                            Iterator<HashMap.Entry<Integer, Pair<Double, Double>>> subAttributeIterator = subEntry.getValue().getMap().entrySet().iterator();

                            Boolean matched = true;
                            while (subAttributeIterator.hasNext()) {
                                HashMap.Entry<Integer, Pair<Double, Double>> subAttributeEntry = subAttributeIterator.next();
                                Integer subAttributeID = subAttributeEntry.getKey();
                                if (!eventAttributeIDToValue.containsKey(subAttributeID)) {
                                    matched = false;
                                    break;
                                }

                                Double low = subAttributeEntry.getValue().getFirst();
                                Double high = subAttributeEntry.getValue().getSecond();
                                Double eventValue = eventAttributeIDToValue.get(subAttributeID);
                                if (eventValue < low || eventValue > high) {
                                    matched = false;
                                    break;
                                }
                            }
                            if (matched) {   // Save this subID to MatchResult
                                matchNum++;
                                matchResult.append(" ");
                                matchResult.append(subID);
//                                matchResult += " " + String.valueOf(subID);
                            }
                        }
                        log = new StringBuilder(boltName);
                        log.append(" Thread ");
                        log.append(threadNumber);
                        log.append(": EventID ");
                        log.append(eventID);
                        log.append(" matching task is done.\n");
                        output.writeToLogFile(log.toString());
//                        output.writeToLogFile(boltName + ": Event " + String.valueOf(eventID) + " matching task is done.\n");
                        matchResult.append("; MatchedSubNum: ");
                        matchResult.append(matchNum);
                        matchResult.append(".\n");
                        output.saveMatchResult(matchResult.toString());
//                        output.saveMatchResult(matchResult + "; MatchedSubNum: " + String.valueOf(matchNum) + ".\n");
                    }
                    collector.ack(tuple);
                    numEventMatched += eventPacket.size();
//                    matchEventTime += System.nanoTime() - startTime;
                    break;
                }
                default:
                    collector.fail(tuple);
                    log = new StringBuilder(boltName);
                    log.append(" Thread ");
                    log.append(threadNumber);
                    log.append(": Wrong operation type is detected.\n");
                    output.writeToLogFile(log.toString());
//                    output.writeToLogFile(boltName + ": Wrong operation type is detected.\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (System.nanoTime() > speedTime) {

            runTime = System.nanoTime() - beginTime;

            speedReport = new StringBuilder(boltName);
            speedReport.append(" Thread ");
            speedReport.append(threadNumber);
            speedReport.append(" - RunTime: ");

            speedReport.append(runTime / intervalTime);
            speedReport.append("min. numSubInserted: ");
            speedReport.append(numSubInserted);
            speedReport.append("; InsertSpeed: ");

//            speedReport.append(numSubInserted*1000000000/insertSubTime);   // per/s
            speedReport.append(runTime / numSubInserted / 1000);           // us/per
            speedReport.append(". numEventMatched: ");
            speedReport.append(numEventMatched);
            speedReport.append("; MatchSpeed: ");
//            speedReport.append(numEventMatched*1000000000/matchEventTime); // per/s
            speedReport.append(runTime / numEventMatched / 1000);         // us/per
            speedReport.append(".\n");
            try {
                output.recordSpeed(speedReport.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            speedTime = System.nanoTime() + intervalTime;
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
//        outputFieldsDeclarer.declare(new Fields("MatchResult"));
    }
}