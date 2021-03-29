package org.apache.storm.starter.bolt;

import org.apache.storm.shade.org.apache.commons.lang.StringUtils;
import org.apache.storm.starter.DataStructure.*;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.MessageId;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiPartitionMatchBolt extends BaseRichBolt {
    private OutputToFile output;
    private OutputCollector collector;
    private TopologyContext boltContext;
    private HashMap<Integer, Subscription> mapIDtoSub;
    static private ArrayList<String> VSSIDtoExecutorID;

    private StringBuilder log;
    private StringBuilder matchResult;

    private String boltName;
    private Integer numSubPacket;
    private Integer numEventPacket;
    private Integer numSubInserted;
    private Integer numEventMatched;
    final private Integer numVisualSubSet;
    final private Integer numExecutor;
    private Integer executorID;
    static private Integer boltIDAllocator;
    static private Integer redundancy;
    //    static private Integer beginExecutorID;
    private long runTime;
    private long speedTime;  // The time to calculate and record speed
    final private long beginTime;
    final private long intervalTime; // The interval between two calculations of speed

    public MultiPartitionMatchBolt(Integer num_executor,Integer redundancy_degree) {   // only execute one time for all executors!
        beginTime = System.nanoTime();
        intervalTime = 60000000000L;  // 1 minute
        boltIDAllocator=0;
        numSubPacket = 0;
        numEventPacket = 0;
        numSubInserted = 1;
        numEventMatched = 1;
        runTime = 1;
        numExecutor=num_executor;
        redundancy=redundancy_degree;

        //output = new OutputToFile();
        log = new StringBuilder();
        matchResult = new StringBuilder();

        // calculate the number of visual subset
        int n=1,m=1,nm=1;
        for(int i=2; i<=numExecutor; i++) {
            n *= i;
            if (i == redundancy)
                m = n;
            if (i == (numExecutor - redundancy))
                nm = n;
        }
        numVisualSubSet=n/m/nm;
        mpv=new HashMap<>();
        VSSIDtoExecutorID=SubsetCodeGeneration(redundancy,numExecutor);
        mpv=null;  //  Now is not needed.
    }

    public synchronized void allocateID(){
        executorID = boltIDAllocator++;//boltContext.getThisTaskId(); // Get the current thread number
    }

    static private HashMap<Pair<Integer, Integer>, ArrayList<String>> mpv;

    //  从K位里生成含k个1的字符串的集合
    ArrayList<String> SubsetCodeGeneration(int k, int K) {
        if (k == 0) return new ArrayList<String>(){{add(StringUtils.repeat("0", K));}};
        if (mpv.containsKey(Pair.of(k, K))) return mpv.get(Pair.of(k, K));
        ArrayList<String> strSet=new ArrayList<>();
        for (int i = k; i <= K; i++)  //  只有前 i 位有1且第 i 位必须是1
        {
            String highStr = StringUtils.repeat("0",K - i) + "1";
            //  从前i-1位里生成含k-1个1的字符串的集合
            ArrayList<String> lowPart = SubsetCodeGeneration(k - 1, i - 1);
            mpv.put(Pair.of(k - 1, i - 1),lowPart);
            for (int j=0;j<lowPart.size();j++)
                strSet.add(highStr + lowPart.get(j));
        }
        return strSet;
    }

    @Override
    public void prepare(Map<String, Object> map, TopologyContext topologyContext, OutputCollector outputCollector) { // execute one time for every executor!

        speedTime = System.nanoTime() + intervalTime;
        boltContext = topologyContext;
        collector = outputCollector;
        boltName = boltContext.getThisComponentId();

        allocateID();  // boltIDAllocator need to keep synchronized
        output = new OutputToFile();
        mapIDtoSub = new HashMap<>();

        if(executorID==0){
            log=new StringBuilder(boltName);
            log.append("MultiPartitionMatchBolt \nnumExecutor = ");
            log.append(numExecutor);
            log.append("\nredundancy = ");
            log.append(redundancy);
            log.append("\nnumVisualSubSet = ");
            log.append(numVisualSubSet);
            log.append("\nMap Table:\nID  ExecutorID");
            for(int i=0;i<VSSIDtoExecutorID.size();i++){
                log.append(String.format("\n%02d: ", i));
                log.append(VSSIDtoExecutorID.get(i));
            }
            log.append("\n\n");
            try {
                output.otherInfo(log.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            log = new StringBuilder(boltName);
            log.append(" ThreadNum: " + Thread.currentThread().getName() + "\n" + boltName + ":");
            List<Integer> taskIds = boltContext.getComponentTasks(boltContext.getThisComponentId());
//            numExecutor = taskIds.size();
            Iterator taskIdsIter = taskIds.iterator();
            int taskID;
//            beginExecutorID = 3;//=Integer.MAX_VALUE;
            while (taskIdsIter.hasNext()) {
                taskID = (Integer) taskIdsIter.next();
//                beginExecutorID = Math.min(taskID, beginExecutorID);
                log.append(" ");
                log.append(taskID);
            }
            log.append("\nThisTaskId: ");
            log.append(executorID);   // boltContext.getThisTaskId();
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

        //        final String threadName = Thread.currentThread().getName();
//        char singleDigit = threadName.charAt(threadName.length() - 2);
//        char tensDigit = threadName.charAt(threadName.length() - 3);
//        Integer threadNumber = (int)singleDigit - 0;
//        if(Character.isDigit(tensDigit))
//            threadNumber+= 10*(int)tensDigit;

        // Solution B: get the operation type to find what the tuple is
//        int type = (int) tuple.getValue(0);
        int type=tuple.getInteger(0);
        try {
            switch (type) {
                case TypeConstant.Insert_Subscription: {

                    Integer subPacketID=tuple.getInteger(1);
//                    if(subPacketID%boltIDAllocator!=executorID)
//                    {
//                        collector.ack(tuple);
//                        break;
//                    }

//                    startTime = System.nanoTime();
                    int subID;
                    numSubPacket++;
                    log = new StringBuilder(boltName);
                    log.append(" Thread ");
                    log.append(executorID);
                    log.append(": SubPacket ");
                    log.append(numSubPacket);
                    log.append(" is received.\n");
                    output.writeToLogFile(log.toString());

                    ArrayList<Subscription> subPacket = (ArrayList<Subscription>) tuple.getValueByField("SubscriptionPacket");
                    for (int i = 0; i < subPacket.size(); i++) {
                        subID = subPacket.get(i).getSubID();
                        if (VSSIDtoExecutorID.get(subID % numVisualSubSet).charAt(executorID)=='0')
                            continue;
                        mapIDtoSub.put(subID, subPacket.get(i));
                        numSubInserted++;
                        log = new StringBuilder(boltName);
                        log.append(" Thread ");
                        log.append(executorID);
                        log.append(": Sub ");
                        log.append(subID);
                        log.append(" is inserted.\n");
                        output.writeToLogFile(log.toString());
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

//                    Integer eventPacketID=(Integer)tuple.getValue(1);
//                    if(eventPacketID%boltIDAllocator!=executorID) {
//                        collector.ack(tuple);
//                        break;
//                    }

//                    startTime = System.nanoTime();
                    numEventPacket++;
                    log = new StringBuilder(boltName);
                    log.append(" Thread ");
                    log.append(executorID);
                    log.append(": EventPacket ");
                    log.append(numEventPacket);
                    log.append(" is received.\n");
                    output.writeToLogFile(log.toString());
                    ArrayList<Event> eventPacket = (ArrayList<Event>) tuple.getValueByField("EventPacket");
                    for (int i = 0; i < eventPacket.size(); i++) {
                        int eventID = eventPacket.get(i).getEventID();
//                        if(eventID%boltIDAllocator!=executorID)
//                            continue;
//                        matchResult = new StringBuilder(boltName);
//                        matchResult.append(" Thread ");
//                        matchResult.append(executorID);
//                        matchResult.append(" - EventID: ");
//                        matchResult.append(eventID);
//                        matchResult.append("; SubNum:");
//                        matchResult.append(mapIDtoSub.size());
//                        matchResult.append("; SubID:");

                        if (mapIDtoSub.size() == 0) {
                            log = new StringBuilder(boltName);
                            log.append(" Thread ");
                            log.append(executorID);
                            log.append(": EventID ");
                            log.append(eventID);
                            log.append(" matching task is done.\n");
                            output.writeToLogFile(log.toString());
//                            matchResult.append(" ; MatchedSubNum: 0.\n");
//                            output.saveMatchResult(matchResult.toString());
                            collector.emit(new Values(executorID, eventID, new ArrayList<>()));
                            continue;
                        }

                        ArrayList<Integer> matchedSubIDList = new ArrayList<Integer>();
                        HashMap<String, Double> eventAttributeNameToValue = eventPacket.get(i).getMap();
                        Iterator<HashMap.Entry<Integer, Subscription>> subIterator = mapIDtoSub.entrySet().iterator();

                        while (subIterator.hasNext()) {
                            HashMap.Entry<Integer, Subscription> subEntry = subIterator.next();
                            Integer subID = subEntry.getKey();
                            Iterator<HashMap.Entry<String, Pair<Double, Double>>> subAttributeIterator = subEntry.getValue().getMap().entrySet().iterator();

                            Boolean matched = true;
                            while (subAttributeIterator.hasNext()) {
                                HashMap.Entry<String, Pair<Double, Double>> subAttributeEntry = subAttributeIterator.next();
                                String subAttributeName = subAttributeEntry.getKey();
                                if (!eventAttributeNameToValue.containsKey(subAttributeName)) {
                                    matched = false;
                                    break;
                                }

                                Double low = subAttributeEntry.getValue().getFirst();
                                Double high = subAttributeEntry.getValue().getSecond();
                                Double eventValue = eventAttributeNameToValue.get(subAttributeName);
                                if (eventValue < low || eventValue > high) {
                                    matched = false;
                                    break;
                                }
                            }
                            if (matched) {   // Save this subID to MatchResult
//                                matchResult.append(" ");
//                                matchResult.append(subID);
                                matchedSubIDList.add(subID);
                            }
                        }
                        log = new StringBuilder(boltName);
                        log.append(" Thread ");
                        log.append(executorID);
                        log.append(": EventID ");
                        log.append(eventID);
                        log.append(" matching task is done.\n");
                        output.writeToLogFile(log.toString());
//                        matchResult.append("; MatchedSubNum: ");
//                        matchResult.append(matchedSubIDList.size());
//                        matchResult.append(".\n");
//                        output.saveMatchResult(matchResult.toString());
                        collector.emit(new Values(executorID, eventID, matchedSubIDList));
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
                    log.append(executorID);
                    log.append(": Wrong operation type is detected.\n");
                    output.writeToLogFile(log.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (System.nanoTime() > speedTime) {
            runTime = System.nanoTime() - beginTime;
            StringBuilder speedReport = new StringBuilder(boltName);
            speedReport.append(" Thread ");
            speedReport.append(executorID);
            speedReport.append(" - RunTime: ");
            speedReport.append(runTime / intervalTime);
            speedReport.append("min. numSubInserted: ");
            speedReport.append(numSubInserted); //mapIDtoSub.size()
            speedReport.append("; InsertSpeed: ");
            speedReport.append(runTime / numSubInserted / 1000);  // us/per
            speedReport.append(". numEventMatched: ");
            speedReport.append(numEventMatched);
            speedReport.append("; MatchSpeed: ");
            speedReport.append(runTime / numEventMatched / 1000); // us/per
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
        outputFieldsDeclarer.declare(new Fields("executorID", "eventID","subIDs"));
    }

    public Integer getNumExecutor(){
        return numExecutor;
//        return boltIDAllocator;   //  this variable may not be the last executor number.
    }
}
