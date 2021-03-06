package org.sjtu.swhua.storm.MatchAlgorithm.DataStructure;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Event implements Serializable {
//    private int maxNumAttributes;
    @JSONField(name="0")
    private int eventID;
    @JSONField(serialize=false)
    private OutputToFile output;
    //private HashMap<String, Double> attributeNameToValue;
    @JSONField
    private HashMap<Integer, Double> attributeIDToValue;

    public Event() {
//        maxNumAttributes = 0;
        eventID = -1;
        output = new OutputToFile();
        attributeIDToValue = new HashMap<>();
    }

    public Event(final int ID, int num_attributes, ArrayList<Integer> attributeID, ArrayList<Double> values) throws IOException {
        eventID = ID;
//        maxNumAttributes = num_attributes;
        output = new OutputToFile();
        if (attributeID.size() < values.size()) {
            output.writeToLogFile("The number of value is larger than the number of attributes, event construct failed.\n");
            return;
        }

        int size=attributeID.size();
        for (int i = 0; i < size; i++) {
            if (attributeIDToValue.containsKey(attributeID.get(i))) {
                output.writeToLogFile("Attribte name duplicate, event construct failed.\n");
                //return;
            }
            attributeIDToValue.put(attributeID.get(i), values.get(i));
        }
    }

    public Event( final Integer ID, int num_attributes, HashMap<Integer, Double> mapIDToValue) throws IOException {
        eventID = ID;
        output = new OutputToFile();
        if (num_attributes < mapIDToValue.size()) {
            output.writeToLogFile("The number of values is larger than the max number of attributes, event construct failed.\n");
            return;
        }
//        maxNumAttributes = num_attributes;
        attributeIDToValue = mapIDToValue;
    }

    @JSONCreator
    public Event(@JSONField final Integer ID,  @JSONField HashMap<Integer, Double> mapIDToValue) throws IOException {
        eventID = ID;
        output = new OutputToFile();
        attributeIDToValue = mapIDToValue;
    }

    // 专门为反序列化而设置
    public void setEventID(int id){
        eventID=id;
    }

    public Double getAttributeValue(Integer attributeID) {
        return attributeIDToValue.get(attributeID);
    }

    public int getEventID() {
        return eventID;
    }

    public int getNumAttribute(){return attributeIDToValue.size();}

    public HashMap<Integer, Double> getAttributeIDToValue() {
        return attributeIDToValue;
    }

    public Boolean insertAttribute(Integer attributeID, Double d) throws IOException {
        if (attributeIDToValue.containsKey(attributeID)) {
            output.writeToLogFile("Already exists such a attribute name, event insert failed.\n");
            return false;
        }
//        if (maxNumAttributes == attributeIDToValue.size()) {
//            output.writeToLogFile("Number of attributes is full, event insert failed.\n");
//            return false;
//        }
        attributeIDToValue.put(attributeID, d);
        return true;
    }

    public Boolean deleteAttribute(String attributeName) throws IOException {
        if (attributeIDToValue.containsKey(attributeName)) {
            attributeIDToValue.remove(attributeName);
            return true;
        }
        output.writeToLogFile("No such an attribute name, event delete failed.\n");
        return false;
    }

    public Boolean updateAttribute(Integer attributeID, Double d) throws IOException {
        if (attributeIDToValue.containsKey(attributeID)) {
            attributeIDToValue.put(attributeID, d);
            return true;
        }
        output.writeToLogFile("No such a attribute name, event update failed.\n");
        return false;
    }
}