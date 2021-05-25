package org.sjtu.swhua.storm.MatchAlgorithm;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.sjtu.swhua.storm.MatchAlgorithm.DataStructure.*;
import org.sjtu.swhua.storm.MatchAlgorithm.bolt.*;
import org.sjtu.swhua.storm.MatchAlgorithm.spout.EventSpout;
import org.sjtu.swhua.storm.MatchAlgorithm.spout.SubscriptionSpout;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.utils.Utils;
import org.sjtu.swhua.storm.MatchAlgorithm.bolt.MultiPartitionMergerBolt;
import org.sjtu.swhua.storm.MatchAlgorithm.bolt.TamaMPMatchBolt;

//storm local ./storm-2021-5-4.jar  org.sjtu.swhua.storm.MatchAlgorithm.SimpleMatchTopology
public class SimpleMatchTopology {
    public static void main(String[] args) throws Exception {

        Integer numExecutorInAMatchBolt = TypeConstant.numExecutorperMatchBolt;
        Integer redundancy = TypeConstant.redundancy;

        MyUtils utils = new MyUtils(numExecutorInAMatchBolt, redundancy);

        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("SubSpout", new SubscriptionSpout(), 1);
        builder.setSpout("EventSpout", new EventSpout(1), 1);

        builder.setBolt("TamaMPMBolt0",new TamaMPMatchBolt(0,numExecutorInAMatchBolt, redundancy, utils.getNumVisualSubSet(), utils.getVSSIDtoExecutorID()), numExecutorInAMatchBolt).allGrouping("SubSpout").allGrouping("EventSpout");
        builder.setBolt("MPMergerBolt0", new MultiPartitionMergerBolt(numExecutorInAMatchBolt, redundancy, utils.getExecutorCombination()), 1).allGrouping("TamaMPMBolt0");

//        builder.setBolt("ReinMPMBolt0", new ReinMPMatchBolt(0,numExecutorInAMatchBolt, redundancy, utils.getNumVisualSubSet(), utils.getVSSIDtoExecutorID()), numExecutorInAMatchBolt).allGrouping("SubSpout").allGrouping("EventSpout");//.setNumTasks(2);
//        builder.setBolt("ReinMPMBolt1", new ReinMPMatchBolt(1,numExecutorInAMatchBolt, redundancy, utils.getNumVisualSubSet(), utils.getVSSIDtoExecutorID()), numExecutorInAMatchBolt).allGrouping("SubSpout").allGrouping("EventSpout");//.setNumTasks(2);
//        builder.setBolt("MPMergerBolt0", new MultiPartitionMergerBolt(numExecutorInAMatchBolt, redundancy, utils.getExecutorCombination()), 1).allGrouping("ReinMPMBolt0");
//        builder.setBolt("MPMergerBolt1", new MultiPartitionMergerBolt(numExecutorInAMatchBolt, redundancy, utils.getExecutorCombination()), 1).allGrouping("ReinMPMBolt1");

//        builder.setBolt("MPMBolt1", new MultiPartitionMatchBolt(0,numExecutorInAMatchBolt, redundancy, utils.getNumVisualSubSet(), utils.getVSSIDtoExecutorID()),numExecutorInAMatchBolt).allGrouping("SubSpout");
//        builder.setBolt("MergerBolt1",new MultiPartitionMergerBolt(numExecutorInAMatchBolt,redundancy,utils.getExecutorCombination()),1).allGrouping("MPMBolt1");

//        builder.setBolt("TDMBolt0", new ThreadDivisionMatchBolt(numExecutorInAMatchBolt),6).allGrouping("SubSpout").allGrouping("EventSpout");//.setNumTasks(2);
//        builder.setBolt("MergerBolt1",new MergerBolt(numExecutorInAMatchBolt),1).allGrouping("TDMBolt0");

//        builder.setBolt("TDMBolt1", new ThreadDivisionMatchBolt(),1).allGrouping("SubSpout").allGrouping("EventSpout");
//        builder.setBolt("TDMBolt2", new ThreadDivisionMatchBolt(),1).allGrouping("SubSpout").allGrouping("EventSpout");
//        builder.setBolt("TDMBolt3", new ThreadDivisionMatchBolt(),1).allGrouping("SubSpout").allGrouping("EventSpout");
//        builder.setBolt("SMBolt", new SimpleMatchBolt("SimpleMatchBolt1"), 1).shuffleGrouping("SubSpout").allGrouping("EventSpout");//
//        builder.setBolt("SMBolt", new SimpleMatchBolt("SimpleMatchBolt2"), 4).allGrouping("SubSpout").shuffleGrouping("EventSpout");

        Config conf = new Config();
        Config.setFallBackOnJavaSerialization(conf, false); // Don't use java's serialization.
        conf.registerSerialization(OutputToFile.class);
        conf.registerSerialization(Pair.class);
        conf.registerSerialization(Event.class);
        conf.registerSerialization(Subscription.class);
        conf.registerSerialization(Double.class);
//        conf.registerSerialization(Rein.class);

        conf.setDebug(false); // True will print all sub, event and match data.
        conf.setNumWorkers(6);
        conf.setMaxTaskParallelism(12);
        conf.put(Config.TOPOLOGY_ACKER_EXECUTORS, 10);// 设置acker的数量, default: 1
        conf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 90);
        conf.put(Config.TOPOLOGY_MAX_SPOUT_PENDING, 1000);//设置一个spout task上面最多有多少个没有处理的tuple（没有ack/failed）回复，以防止tuple队列爆掉
        // conf.put(Config.TOPOLOGY_WORKER_CHILDOPTS,"-Xmx%HEAP-MEM%m -XX:+PrintGCDetails -Xloggc:artifacts/gc.log  -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=1M -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=artifacts/heapdump");

        // -XX:+PrintGCDateStamps is omitted, because it will lead to a log: "[INFO] Unrecognized VM option 'PrintGCDateStamps'"
        String topoName = "SimpleMatchTopology";
        if (args != null && args.length > 0) {
            topoName = args[0];
        }

        LocalCluster localCluster = new LocalCluster();
        localCluster.submitTopology(topoName, conf, builder.createTopology());
//        StormSubmitter.submitTopologyWithProgressBar(topoName, conf, builder.createTopology());
        Utils.sleep(60000000);
        localCluster.killTopology(topoName);
        localCluster.shutdown();
    }
}
