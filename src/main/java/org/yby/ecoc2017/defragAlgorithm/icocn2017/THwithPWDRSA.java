package org.yby.ecoc2017.defragAlgorithm.icocn2017;

import com.google.common.collect.Lists;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.defragAlgorithm.boyuan.MLSPDRSA;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by yby on 2017/5/12.
 *
 */
public class THwithPWDRSA extends MLSPDRSA {

    // 离去业务的计数，超过阈值以后就会触发重构。
    private int leaveServiceCount = 0;

    public THwithPWDRSA(SimpleWeightedGraph<EonVertex, EonEdge> graph, ArrayList<Service> servicesQueue, ArrayList<Timestamp> servicesOrderedQueue) {
        super(graph, servicesQueue, servicesOrderedQueue);
    }


    // 存储每次重构时候的<重构发生的时间，<选择进行重构的业务数量，实际被重构的业务数量>>。
    public ArrayList<Pair<Calendar, Pair<Integer, Integer>>> shiftServiceCountList = Lists.newArrayListWithExpectedSize(
            getServicesQueue().size() / Configuration.TH + 1);

    @Override
    public void allocate() {
        for (int index=0; index<getServicesOrderedQueue().size(); index++) {
            Timestamp timestamp = getServicesOrderedQueue().get(index);
            Service serviceToBeAssigned = getServicesQueue().get(timestamp.getServiceIndex()-1);
            // if this timestamp is start-time.
            if (timestamp.isStartTime()) {
                EonVertex src = new EonVertex(serviceToBeAssigned.getSource());
                EonVertex dst = new EonVertex(serviceToBeAssigned.getDestination());
                GraphPath<EonVertex, EonEdge> path = getDijkstraShortestPath().getPath(src, dst);
                int subscript = findFirstAvailableSlot(path.getEdgeList(), serviceToBeAssigned.getRequiredSlotNum());
                // if there exists enough spectrum resource.
                if (subscript != BasicRSAAlg.UNAVAILABLE) {
                    execAllocation(path.getEdgeList(),
                            subscript,
                            serviceToBeAssigned);
                } else {
                    // if there isn't enough spectrum resource at specific shortest path.
                    addBlockedService(serviceToBeAssigned);
                }
            } else {
                // if this timestamp is end-time.
                handleServiceLeave(serviceToBeAssigned.getIndex());
                leaveServiceCount++;
                if (leaveServiceCount >= Configuration.TH) {
                    // 如果离去业务超过阈值，则进行重构
                    List<Timestamp> orderedList = subList(getServicesOrderedQueue(), index);
                    PWDAlg<EonEdge> pwdAlg = new PWDAlg<>(
                            getGraph(), getCurrentServices(), generateOccupiedSlotsNum(orderedList));
                    pwdAlg.defragment(null);
                    Pair<Integer, Integer> pair = new Pair<>(pwdAlg.getCheckedServiceNum(), pwdAlg.getShiftTime());
                    shiftServiceCountList.add(new Pair<>(serviceToBeAssigned.getEndTime(), pair));
                    // 重新开始计数
                    leaveServiceCount = 0;
                }
            }
        }
    }

}
