package org.apache.storm.starter.DataStructure;

public class TypeConstant {
    // 操作类型
    public static final int Insert_Subscription = 1;
    public static final int Insert_Attribute_Subscription = 2;
    public static final int Update_Attribute_Subscription = 3;
    public static final int Delete_Attribute_Subscription = 4;
    public static final int Delete_Subscription = 5;
    public static final int Event_Match_Subscription = 6;
    public static final int Null_Operation = 7;

    // 平凡参数
    public static final int subSetSize = 200000;
    public static final int numAttributeType=50;
    public static final int maxNumSubscriptionPerPacket = 100;
    public static final int maxNumAttributePerSubscription = 10;
    public static final int maxNumEventPerPacket=100;
    public static final int maxNumAttributePerEvent=30;


    // 实验模型参数
    //rein
    public static final int numBucket=5;
    //MPM
    public static final int numExecutorperMatchBolt=6;
    public static final int redundancy=3;
}
