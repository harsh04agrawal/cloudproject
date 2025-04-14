package main.java;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * CloudSim Scheduling Algorithm Comparison
 * 
 * This simulation compares Time-Shared and Space-Shared scheduling algorithms
 * in terms of execution time, resource utilization, and response time.
 */
public class cloudproject {

    /** The cloudlet list. */
    private static List<Cloudlet> cloudletList;

    /** The vm list for time-shared scheduling. */
    private static List<Vm> vmListTimeShared;
    
    /** The vm list for space-shared scheduling. */
    private static List<Vm> vmListSpaceShared;

    private static final int NUM_HOSTS = 2;
    private static final int HOST_PES = 4;
    private static final int NUM_VMS = 4;
    private static final int VM_PES = 2;
    private static final int NUM_CLOUDLETS = 5;
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LENGTH = 10000;

    /**
     * Main method to run the simulation.
     */
    public static void main(String[] args) {
        try {
            // Initialize CloudSim
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            // Create Datacenters
            Datacenter datacenterTimeShared = createDatacenter("Datacenter_TimeShared");
            Datacenter datacenterSpaceShared = createDatacenter("Datacenter_SpaceShared");

            // Create Brokers
            DatacenterBroker brokerTimeShared = createBroker("Broker_TimeShared");
            int brokerTimeSharedId = brokerTimeShared.getId();

            DatacenterBroker brokerSpaceShared = createBroker("Broker_SpaceShared");
            int brokerSpaceSharedId = brokerSpaceShared.getId();

            // Create VMs and Cloudlets
            vmListTimeShared = createVMs(brokerTimeSharedId, true); // Time-shared VMs
            vmListSpaceShared = createVMs(brokerSpaceSharedId, false); // Space-shared VMs
            
            cloudletList = createCloudlets(brokerTimeSharedId, NUM_CLOUDLETS);
            List<Cloudlet> cloudletListSpaceShared = createCloudlets(brokerSpaceSharedId, NUM_CLOUDLETS);

            // Submit VM lists to the brokers
            brokerTimeShared.submitVmList(vmListTimeShared);
            brokerSpaceShared.submitVmList(vmListSpaceShared);

            // Submit cloudlet lists to the brokers
            brokerTimeShared.submitCloudletList(cloudletList);
            brokerSpaceShared.submitCloudletList(cloudletListSpaceShared);

            // Start the simulation
            CloudSim.startSimulation();

            // Stop the simulation
            CloudSim.stopSimulation();

            // Retrieve results
            List<Cloudlet> timeSharedResults = brokerTimeShared.getCloudletReceivedList();
            List<Cloudlet> spaceSharedResults = brokerSpaceShared.getCloudletReceivedList();
            
            // Print results
            System.out.println("=== Time-Shared Scheduling Algorithm Results ===");
            printCloudletList(timeSharedResults);
            
            System.out.println("\n=== Space-Shared Scheduling Algorithm Results ===");
            printCloudletList(spaceSharedResults);
            
            // Compare results
            compareResults(timeSharedResults, spaceSharedResults);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happened.");
        }
    }

    /**
     * Creates the datacenter.
     */
    private static Datacenter createDatacenter(String name) {
        // Create a list to store hosts
        List<Host> hostList = new ArrayList<Host>();

        // Create hosts and add them to the list
        for (int i = 0; i < NUM_HOSTS; i++) {
            int mips = 1000; // MIPS rating
            int ram = 2048; // host memory (MB)
            long storage = 1000000; // host storage (MB)
            int bw = 10000; // bandwidth

            // Create Processing Elements (PEs or CPUs/Cores)
            List<Pe> peList = new ArrayList<Pe>();
            for (int j = 0; j < HOST_PES; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(mips)));
            }

            // Create Host with its characteristics
            Host host = new Host(
                    i,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList)
            );
            hostList.add(host);
        }

        // Create a DatacenterCharacteristics object
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen"; // VMM
        double time_zone = 10.0; // time zone (GMT)
        double cost = 3.0; // the cost of using processing in this data center
        double costPerMem = 0.05; // the cost of using memory in this data center
        double costPerStorage = 0.001; // the cost of using storage in this data center
        double costPerBw = 0.0; // the cost of using bandwidth in this data center
        
        LinkedList<Storage> storageList = new LinkedList<Storage>();
        
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // Create a Datacenter object
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    /**
     * Creates the broker.
     */
    private static DatacenterBroker createBroker(String name) {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    /**
     * Creates the virtual machines.
     */
    private static List<Vm> createVMs(int brokerId, boolean isTimeShared) {
        // Create VM List
        List<Vm> vmList = new ArrayList<Vm>();

        // VM characteristics
        int mips = 1000;
        long size = 10000; // image size (MB)
        int ram = 512; // vm memory (MB)
        long bw = 1000;

        for (int i = 0; i < NUM_VMS; i++) {
            Vm vm;
            
            // Choose the cloudlet scheduler based on the algorithm
            if (isTimeShared) {
                vm = new Vm(i, brokerId, mips, VM_PES, ram, bw, size, "Xen", 
                        new CloudletSchedulerTimeShared());
            } else {
                vm = new Vm(i, brokerId, mips, VM_PES, ram, bw, size, "Xen", 
                        new CloudletSchedulerSpaceShared());
            }
            
            vmList.add(vm);
        }

        return vmList;
    }

    /**
     * Creates the cloudlets.
     */
    private static List<Cloudlet> createCloudlets(int brokerId, int numCloudlets) {
        List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();

        // Cloudlet properties
        long fileSize = 300;
        long outputSize = 300;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < numCloudlets; i++) {
            Cloudlet cloudlet = new Cloudlet(i, CLOUDLET_LENGTH, CLOUDLET_PES, fileSize, outputSize, 
                    utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }

        return cloudletList;
    }

    /**
     * Prints the Cloudlet objects.
     */
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        
        double totalExecutionTime = 0.0;
        
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()));
                
                totalExecutionTime += cloudlet.getActualCPUTime();
            }
        }
        
        Log.printLine("Total Execution Time: " + dft.format(totalExecutionTime));
        Log.printLine("Average Execution Time: " + dft.format(totalExecutionTime / size));
    }
    
    /**
     * Compares results between the two scheduling algorithms.
     */
    private static void compareResults(List<Cloudlet> timeSharedResults, List<Cloudlet> spaceSharedResults) {
        DecimalFormat dft = new DecimalFormat("###.##");
        
        double totalTimeSharedExecTime = 0.0;
        double totalSpaceSharedExecTime = 0.0;
        
        double timeSharedMakespan = 0.0;
        double spaceSharedMakespan = 0.0;
        
        for (Cloudlet cloudlet : timeSharedResults) {
            totalTimeSharedExecTime += cloudlet.getActualCPUTime();
            if (cloudlet.getFinishTime() > timeSharedMakespan) {
                timeSharedMakespan = cloudlet.getFinishTime();
            }
        }
        
        for (Cloudlet cloudlet : spaceSharedResults) {
            totalSpaceSharedExecTime += cloudlet.getActualCPUTime();
            if (cloudlet.getFinishTime() > spaceSharedMakespan) {
                spaceSharedMakespan = cloudlet.getFinishTime();
            }
        }
        
        Log.printLine("\n========== COMPARISON ==========");
        Log.printLine("Time-Shared vs Space-Shared Scheduling Algorithm Comparison:");
        Log.printLine("Total Execution Time (Time-Shared): " + dft.format(totalTimeSharedExecTime));
        Log.printLine("Total Execution Time (Space-Shared): " + dft.format(totalSpaceSharedExecTime));
        
        Log.printLine("Average Execution Time (Time-Shared): " + dft.format(totalTimeSharedExecTime / timeSharedResults.size()));
        Log.printLine("Average Execution Time (Space-Shared): " + dft.format(totalSpaceSharedExecTime / spaceSharedResults.size()));
        
        Log.printLine("Makespan (Time-Shared): " + dft.format(timeSharedMakespan));
        Log.printLine("Makespan (Space-Shared): " + dft.format(spaceSharedMakespan));
        
        double responseTimeTS = calculateAvgResponseTime(timeSharedResults);
        double responseTimeSS = calculateAvgResponseTime(spaceSharedResults);
        
        Log.printLine("Average Response Time (Time-Shared): " + dft.format(responseTimeTS));
        Log.printLine("Average Response Time (Space-Shared): " + dft.format(responseTimeSS));
        
        // Print summary and analysis
        Log.printLine("\n========== SUMMARY ==========");
        if (timeSharedMakespan < spaceSharedMakespan) {
            Log.printLine("Time-Shared algorithm completed all tasks faster overall (better makespan).");
        } else {
            Log.printLine("Space-Shared algorithm completed all tasks faster overall (better makespan).");
        }
        
        if (responseTimeTS < responseTimeSS) {
            Log.printLine("Time-Shared algorithm has better average response time.");
        } else {
            Log.printLine("Space-Shared algorithm has better average response time.");
        }
        
        Log.printLine("\nAnalysis:");
        Log.printLine("- Time-Shared scheduling allows multiple cloudlets to share CPU time slices");
        Log.printLine("- Space-Shared scheduling allocates dedicated CPU cores to cloudlets until completion");
        Log.printLine("- Time-Shared typically provides better resource utilization and response times");
        Log.printLine("- Space-Shared typically provides better execution times for CPU-intensive tasks");
    }
    
    /**
     * Calculates the average response time for a list of cloudlets.
     */
    private static double calculateAvgResponseTime(List<Cloudlet> cloudletList) {
        double totalResponseTime = 0.0;
        
        for (Cloudlet cloudlet : cloudletList) {
            // Response time = start execution time - submission time (in this case submission time is 0)
            totalResponseTime += cloudlet.getExecStartTime();
        }
        
        return totalResponseTime / cloudletList.size();
    }
}