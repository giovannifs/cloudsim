package org.cloudbus.cloudsim.preemption;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.preemption.policies.preemption.FCFSBasedPreemptionPolicy;
import org.cloudbus.cloudsim.preemption.policies.preemption.TTVBasedPreemptionPolicy;
import org.cloudbus.cloudsim.preemption.policies.vmallocation.PreemptableVmAllocationPolicy;
import org.cloudbus.cloudsim.preemption.policies.vmallocation.WorstFitAvailabilityAwareVmAllocationPolicy;
import org.cloudbus.cloudsim.preemption.policies.vmallocation.WorstFitPriorityBasedVmAllocationPolicy;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.fail;

/**
 * Created by jvmafra on 22/03/17.
 */
public class SystemTestWorstFitTTV {

    private static final double ACCEPTABLE_DIFFERENCE = 0.000001;
    private static final int PROD = 0;
    private static final int BATCH = 1;
    private static final int FREE = 2;
    private double hostCapacity;
    private Properties properties;

    private PreemptiveDatacenter datacenter;
    private SimEvent event;
    private PreemptiveHost host;
    private SimulationTimeUtil timeUtil;
    private PreemptableVmAllocationPolicy preemptableVmAllocationPolicy;
    private DatacenterCharacteristics characteristics;

    private int NUMBER_OF_VMS_P0 = 10;
    private int NUMBER_OF_VMS_P1 = 20;
    private int NUMBER_OF_VMS_P2 = 30;
    private List<Vm> vmsP0;
    private List<Vm> vmsP1;
    private List<Vm> vmsP2;


    private String datacenterOutputFile;
    private String datacenterInputFile;
    private String datacenterInputUrl;
    private String datacenterOutputUrl;


    @Before
    public void setUp() throws Exception {

        Log.disable();

        event = Mockito.mock(SimEvent.class);

        int num_user = 1; // number of grid users
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false; // mean trace events

        // Initialize the CloudSim library
        CloudSim.init(num_user, calendar, trace_flag);


        // inicializating the lists of vms
        vmsP0 = new ArrayList<>(NUMBER_OF_VMS_P0);
        vmsP1 = new ArrayList<>(NUMBER_OF_VMS_P1);
        vmsP2 = new ArrayList<>(NUMBER_OF_VMS_P2);

        // mocking properties to config simulation
        datacenterInputFile = "new_trace_test.sqlite3";
        datacenterInputUrl = "jdbc:sqlite:" + datacenterInputFile;

        datacenterOutputFile = "outputUtilizationTest.sqlite3";
        datacenterOutputUrl = "jdbc:sqlite:" + datacenterOutputFile;

        properties = new Properties();
        properties.setProperty(FCFSBasedPreemptionPolicy.NUMBER_OF_PRIORITIES_PROP, "3");
        // time in micro
        properties.setProperty("logging", "yes");
        properties.setProperty("input_trace_database_url", datacenterInputUrl);
        properties.setProperty("loading_interval_size", "300000000");
        properties.setProperty("storing_interval_size", "240000000");
        properties.setProperty("output_tasks_database_url", datacenterOutputUrl);
        properties.setProperty("utilization_database_url", datacenterOutputUrl);
        properties.setProperty("utilization_storing_interval_size", "480000000");
        properties.setProperty("datacenter_database_url", datacenterOutputUrl);
        properties.setProperty("collect_datacenter_summary_info", "yes");
        properties.setProperty("datacenter_storing_interval_size", "300000000");
        properties.setProperty("datacenter_collect_info_interval_size", "240000000");
        properties.setProperty("make_checkpoint", "no");
        properties.setProperty("checkpoint_interval_size", "300000000");
        properties.setProperty("checkpoint_dir", datacenterOutputUrl);
        properties.setProperty("preemption_policy_class", "org.cloudbus.cloudsim.preemption.policies.preemption.FCFSBasedPreemptionPolicy");
        properties.setProperty("end_of_simulation_time", "600000000");
        properties.setProperty("update_quota_interval_size", "600000000");
        properties.setProperty("number_of_priorities", "3");
        properties.setProperty("slo_availability_target_priority_0", "1");
        properties.setProperty("slo_availability_target_priority_1", "0.9");
        properties.setProperty("slo_availability_target_priority_2", "0.5");

        // creating host
        List<Pe> peList1 = new ArrayList<Pe>();
        hostCapacity = 10;
        peList1.add(new Pe(0, new PeProvisionerSimple(hostCapacity)));

        host = new PreemptiveHost(1, peList1, new VmSchedulerMipsBased(
                peList1), new FCFSBasedPreemptionPolicy(properties));

        // creating list of hosts
        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        // mocking the characteristics for data center
        characteristics = Mockito.mock(DatacenterCharacteristics.class);
        Mockito.when(characteristics.getHostList()).thenReturn(hostList);

        Mockito.when(characteristics.getNumberOfPes()).thenReturn(1);

        timeUtil = Mockito.mock(SimulationTimeUtil.class);
        Mockito.when(timeUtil.clock()).thenReturn(0d);

        List<PreemptiveHost> googleHostList = new ArrayList<PreemptiveHost>();
        for (Host host : hostList) {
            googleHostList.add((PreemptiveHost) host);
        }
        preemptableVmAllocationPolicy = new WorstFitPriorityBasedVmAllocationPolicy(googleHostList);

        // creating data center
        datacenter = new PreemptiveDatacenter("datacenter", characteristics, preemptableVmAllocationPolicy,
                new LinkedList<Storage>(), 0, properties);

        datacenter.setSimulationTimeUtil(timeUtil);

        Assert.assertTrue(datacenter.getVmsForScheduling().isEmpty());
        Assert.assertTrue(datacenter.getVmsRunning().isEmpty());
    }

    private void advanceTime(double time) {

        while (CloudSim.clock() < time) {
            CloudSim.runClockTick();
        }

        if (CloudSim.clock() == time)
            CloudSim.runClockTick();
    }


    private void populateVmListsNewTrace() {

        int vmId = 0;
        int priority = 0;
        double runtime = 7;
        double submitTime = 0;
        double cpuReq = 0.5;
        double availabilityTarget = 1;

        for (int i = 0; i < NUMBER_OF_VMS_P0; i++) {

            if (i == NUMBER_OF_VMS_P0 - 1) {
                submitTime = 1;
            }

            if (i >= 3) {
                runtime = 6;
            }

            Vm vm = new PreemptableVm(vmId++, 0, cpuReq, 0, submitTime, priority, runtime, availabilityTarget);
            vmsP0.add(vm);
        }

        // creating vms model P1S0, total of vms 6603
        // with cpu total requisition of 1980.9

        priority = 1;
        runtime = 4;
        cpuReq = 0.3;
        submitTime = 0;
        availabilityTarget = 0.9;

        for (int i = 0; i < NUMBER_OF_VMS_P1; i++) {

            if (i >= 10) {
                cpuReq = 0.5;
                submitTime = 1;
            }

            Vm vm = new PreemptableVm(vmId++, 0, cpuReq, 0, submitTime, priority, runtime, availabilityTarget);
            vmsP1.add(vm);
        }

        // creating vms model P2S0, total of vms 6603
        // with cpu total requisition of 1320.6

        priority = 2;
        runtime = 2;
        cpuReq = 0.2;
        submitTime = 0;
        availabilityTarget = 0.5;

        for (int i = 0; i < NUMBER_OF_VMS_P2; i++) {

            if (i >= 10 && i <= 19) {
                submitTime = 1;
            } else if (i >= 20 && i <= 29) {
                submitTime = 2;
            }

            Vm vm = new PreemptableVm(vmId++, 0, cpuReq, 0, submitTime, priority, runtime, availabilityTarget);
            vmsP2.add(vm);
        }
    }

    private void submitEventsNewTrace() {

        // time = 0
        int NUMBER_OF_VMS_P0 = 10;
        int NUMBER_OF_VMS_P1 = 20;
        int NUMBER_OF_VMS_P2 = 30;

        for (int i = 0; i < NUMBER_OF_VMS_P0; i++) {

            PreemptableVm vm = (PreemptableVm) vmsP0.get(i);

            CloudSim.send(datacenter.getId(), datacenter.getId(), vm.getSubmitTime(), CloudSimTags.VM_CREATE, vmsP0.get(i));
        }

        for (int i = 0; i < NUMBER_OF_VMS_P1; i++) {

            PreemptableVm vm = (PreemptableVm) vmsP1.get(i);

            CloudSim.send(datacenter.getId(), datacenter.getId(), vm.getSubmitTime(), CloudSimTags.VM_CREATE, vmsP1.get(i));
        }

        for (int i = 0; i < NUMBER_OF_VMS_P2; i++) {

            PreemptableVm vm = (PreemptableVm) vmsP2.get(i);

            CloudSim.send(datacenter.getId(), datacenter.getId(), vm.getSubmitTime(), CloudSimTags.VM_CREATE, vmsP2.get(i));
        }


    }

    @Test
    public void testSystemSingleHostWithWorstFitTTVPolicy() throws Exception{
        Log.enable();

        hostCapacity = 10;

        // creating host
        List<Pe> peList1 = new ArrayList<Pe>();
        peList1.add(new Pe(0, new PeProvisionerSimple(hostCapacity)));
        properties.setProperty("preemption_policy_class",
                "org.cloudbus.cloudsim.preemption.policies.preemption.TTVBasedPreemptionPolicy");

        host = new PreemptiveHost(1, peList1, new VmSchedulerMipsBased(
                peList1), new TTVBasedPreemptionPolicy(properties));

        // creating list of hosts
        List<PreemptiveHost> hostList = new ArrayList<PreemptiveHost>();
        hostList.add(host);


        preemptableVmAllocationPolicy = new WorstFitAvailabilityAwareVmAllocationPolicy(hostList);

        // First step: Initialize the CloudSim package. It should be called
        // before creating any entities.
        int num_user = 1; // number of grid users
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false; // mean trace events

        // Initialize the CloudSim library
        CloudSim.init(num_user, calendar, trace_flag);
        CloudSim.runStart();

        // creating data center
            datacenter = new PreemptiveDatacenter("datacenter", characteristics, preemptableVmAllocationPolicy,
                    new LinkedList<Storage>(), 0, properties);

        //asserting host on data center and host total capacity
        Assert.assertEquals(host, datacenter.getHostList().get(0));
        Assert.assertEquals(hostCapacity, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        // creating vms model P0S0, total of vms 6603
        // with cpu total requisition of 3301.5
        populateVmListsNewTrace();

        submitEventsNewTrace();
        CloudSim.runClockTick();


        advanceTime(0.0);
        //allocating vms with submit time 0
        testSimulationRuntime0();

        advanceTime(1.0);
        //allocating vms with submit time 1
        testSimulationRuntime1();

        advanceTime(2.0);
        //allocating vms with submit time 2
        testSimulationRuntime2();

        advanceTime(3.0);
        testSimulationRuntime3();

        advanceTime(4.0);
        testSimulationRuntime4();

        advanceTime(5.0);
        testSimulationRuntime5();

        advanceTime(6.0);
        testSimulationRuntime6();

        advanceTime(7.0);
        testSimulationRuntime7();

        advanceTime(8.0);
        testSimulationRuntime8();

        advanceTime(9.0);
        testSimulationRuntime9();

        verifyAvailabilityOfSingleHost();
    }

    private void verifyAvailabilityOfSingleHost() {

        double finishedTime = 0;

        for (Vm vm : vmsP0) {

            PreemptableVm pVm = (PreemptableVm) vm;
            Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());

            if (pVm.getId() >= 0 && pVm.getId() <= 8) {
                Assert.assertEquals(0.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            } else {
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            }

            if (pVm.getId() < 3 || pVm.getId() == 9) {
                finishedTime = 7.0;
            } else {
                finishedTime = 6.0;
            }

            Assert.assertEquals(1.0, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
        }

        for (Vm vm : vmsP1) {

            PreemptableVm pVm = (PreemptableVm) vm;
            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());


            if (pVm.getId() >= 10 && pVm.getId() <= 19) {
                finishedTime = 8.0;
                Assert.assertEquals(0.5, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());
                Assert.assertEquals(0.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            } else {
                finishedTime = 5.0;
                Assert.assertEquals(1.0, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            }
        }

        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;
            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());


            if (pVm.getId() >= 30 && pVm.getId() <= 39) {
                finishedTime = 7.0;
                Assert.assertEquals(0.285714286, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());
                Assert.assertEquals(0.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            } else if (pVm.getId() >= 40 && pVm.getId() <= 49) {
                finishedTime = 7.0;
                Assert.assertEquals(0.333333333, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertEquals(5.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            } else if (pVm.getId() >= 50 && pVm.getId() <= 54) {
                finishedTime = 8.0;
                Assert.assertEquals(0.333333333, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertEquals(6.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            } else {
                finishedTime = 9.0;
                Assert.assertEquals(0.285714286, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertEquals(7.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            }
        }


    }

    private void testSimulationRuntime9() {
        Assert.assertEquals(hostCapacity, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size());
        Assert.assertEquals(0, datacenter.getVmsRunning().size());
    }

    private void testSimulationRuntime8() {
        Assert.assertEquals(9, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1, host.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(5, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime8();
    }

    private void testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime8() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {

            Assert.assertEquals(0, vm.getNumberOfPreemptions());
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }

    }

    private void testSimulationRuntime7() {

        Assert.assertEquals(5, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 3, host.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 3 - 2, host.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(20, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime7();
    }

    private void testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime7() {
        for (PreemptableVm vm : datacenter.getVmsRunning()) {

            if (vm.getId() >= 10 && vm.getId() <= 19) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }
    }

    private void testSimulationRuntime6() {
        Assert.assertEquals(0, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2, host.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 3, host.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 3 - 5, host.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(5, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(39, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime6();

    }

    private void testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime6() {
        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            if ((vm.getId() >= 30 && vm.getId() <= 39) || (vm.getId() >= 10 && vm.getId() <= 19)) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {

            Assert.assertEquals(0, vm.getNumberOfPreemptions());
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }
    }

    private void testSimulationRuntime5() {
        Assert.assertEquals(0, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5, host.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5 - 3, host.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5 - 3 - 2, host.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(20, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(30, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime5();
    }

    private void testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime5() {
        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            if ((vm.getId() >= 0 && vm.getId() <= 9) || (vm.getId() >= 40 && vm.getId() <= 49)) {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            }
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {

            if (vm.getId() >= 30 && vm.getId() <= 39) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }

    }

    private void testSimulationRuntime4() {
        testSimulationRuntime2();
    }

    private void testSimulationRuntime3() {
        testSimulationRuntime2();
    }

    private void testSimulationRuntime2() {
        Assert.assertEquals(0, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5, host.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5 - 5, host.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5 - 5, host.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(40, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(20, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime2();
    }

    private void testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime2() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, vm.getNumberOfPreemptions());
        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {

            if (vm.getId() >= 40 && vm.getId() <= 59) {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }

    }

    private void testSimulationRuntime1() {
        Assert.assertEquals(0, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5, host.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5 - 5, host.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 5 - 5, host.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(30, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(20, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime1();

    }

    private void testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime1() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, vm.getNumberOfPreemptions());
        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {

            if (vm.getId() >= 40 && vm.getId() <= 49) {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }

    }

    private void testSimulationRuntime0() {
        Assert.assertEquals(0.5, host.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 4.5, host.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 4.5 - 3, host.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 4.5 - 3 - 2, host.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(29, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime0();

    }

    private void testNumberOfPreemptionsAndBackfillingOfSingleHostWithTTVPolicyTime0() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, vm.getNumberOfPreemptions());
        }

        Assert.assertTrue(datacenter.getVmsForScheduling().isEmpty());
    }


    @Test
    public void testSystemThreeHostsWithWorstFitTTVPolicy() throws Exception{
        Log.enable();

        hostCapacity = 3.3;
        properties.setProperty("preemption_policy_class",
                "org.cloudbus.cloudsim.preemption.policies.preemption.TTVBasedPreemptionPolicy");

        // creating host
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList1 = new ArrayList<>();
        peList1.add(new Pe(0, new PeProvisionerSimple(hostCapacity)));

        PreemptiveHost host1 = new PreemptiveHost(1, peList1, new VmSchedulerMipsBased(
                peList1), new TTVBasedPreemptionPolicy(properties));
        PreemptiveHost host2 = new PreemptiveHost(2, peList1, new VmSchedulerMipsBased(
                peList1), new TTVBasedPreemptionPolicy(properties));
        PreemptiveHost host3 = new PreemptiveHost(3, peList1, new VmSchedulerMipsBased(
                peList1), new TTVBasedPreemptionPolicy(properties));

        hostList.add(host1);
        hostList.add(host2);
        hostList.add(host3);

        List<PreemptiveHost> preemptiveHostList = new ArrayList<>();
        for (Host host : hostList) {
            preemptiveHostList.add((PreemptiveHost) host);
        }

        preemptableVmAllocationPolicy = new WorstFitAvailabilityAwareVmAllocationPolicy(preemptiveHostList);

        Mockito.when(characteristics.getHostList()).thenReturn(hostList);

        // First step: Initialize the CloudSim package. It should be called
        // before creating any entities.
        int num_user = 1; // number of grid users
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false; // mean trace events

        // Initialize the CloudSim library
        CloudSim.init(num_user, calendar, trace_flag);
        CloudSim.runStart();

        // creating data center
            datacenter = new PreemptiveDatacenter("datacenter", characteristics, preemptableVmAllocationPolicy,
                    new LinkedList<Storage>(), 0, properties);

        //asserting hosts in datacenter and host total capacity
        Assert.assertEquals(host1.getId(), datacenter.getHostList().get(0).getId());
        Assert.assertEquals(host2.getId(), datacenter.getHostList().get(1).getId());
        Assert.assertEquals(host3.getId(), datacenter.getHostList().get(2).getId());

        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(hostCapacity, datacenter.getHostList().get(i).getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        }

        // creating vms model P0S0, total of vms 6603
        // with cpu total requisition of 3301.5
        populateVmListsNewTrace();

        submitEventsNewTrace();
        CloudSim.runClockTick();


        advanceTime(0.0);
        //allocating vms with submit time 0
        testSimulationThreeHostsRuntime0();

        advanceTime(1.0);
        //allocating vms with submit time 1
        testSimulationThreeHostsRuntime1();

        advanceTime(2.0);
        //allocating vms with submit time 2
        testSimulationThreeHostsRuntime2();

        advanceTime(3.0);
        //nothing happens
        testSimulationThreeHostsRuntime3();

        advanceTime(4.0);
        //allocating vms with submit time 1
        testSimulationThreeHostsRuntime4();

        advanceTime(5.0);
        testSimulationThreeHostsRuntime5();

        advanceTime(6.0);
        testSimulationThreeHostsRuntime6();

        advanceTime(7.0);
        testSimulationThreeHostsRuntime7();

        advanceTime(8.0);
        testSimulationThreeHostsRuntime8();

        advanceTime(9.0);
        testSimulationThreeHostsRuntime9();

        verifyAvailabilityOfThreeHosts();
    }

    private void testSimulationThreeHostsRuntime0() {

        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(0.2, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0.1, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0.1, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.2, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.2 - 0.4, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 0.9, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 0.9 - 0.8, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 0.9, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 0.9 - 0.8, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(29, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime0();
    }

    private void testSimulationThreeHostsRuntime1() {
        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(0, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 2, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 1 - 0.3, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 1.3, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.5 - 0.3, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.5 - 0.3, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.5 - 0.3, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.5 - 0.3, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(29, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(21, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime1();
    }

    private void testSimulationThreeHostsRuntime2() {
        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(0, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 2, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 1 - 0.3, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 1.3, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.5 - 0.3, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.5 - 0.3, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.5 - 0.3, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.5 - 0.3, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(39, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(21, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime2();
    }


    private void testSimulationThreeHostsRuntime3() {
        testSimulationThreeHostsRuntime2();
    }

    private void testSimulationThreeHostsRuntime4() {
        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(0, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 2, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 0.9, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 0.9 - 0.4, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.6, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.6 - 0.2, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.6, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.6 - 0.2, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(32, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(25, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime4();

    }

    private void testSimulationThreeHostsRuntime5() {
        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(0, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 2, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 0.9, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 2 - 0.9 - 0.4, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.6, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.6 - 0.2, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1.5, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.6, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.5 - 1.6 - 0.2, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(30, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(25, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime5();
    }

    private void testSimulationThreeHostsRuntime6() {
        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(0.1, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0.1, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(0, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 1, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1 - 1.4, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1 - 1.4 - 0.8, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 0.5, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.5 - 2.1, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.5 - 2.1 - 0.6, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity - 0.5, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.5 - 1.6, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.5 - 1.6 - 1.2, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(13, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(30, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime6();
    }

    private void testSimulationThreeHostsRuntime7() {
        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(1.7, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(1.6, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(1.8, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 1.6, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.5, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.5 - 1.2, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.5, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.5 - 1, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size()); //14305
        Assert.assertEquals(21, datacenter.getVmsRunning().size()); //12107

        testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime7();
    }

    private void testSimulationThreeHostsRuntime8() {
        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(2.5, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(2.5, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(2.9, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.8, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.8, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity - 0.4, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size());
        Assert.assertEquals(10, datacenter.getVmsRunning().size());

        testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime8();
    }

    private void testSimulationThreeHostsRuntime9() {
        PreemptiveHost host1 = (PreemptiveHost) datacenter.getHostList().get(0);
        PreemptiveHost host2 = (PreemptiveHost) datacenter.getHostList().get(1);
        PreemptiveHost host3 = (PreemptiveHost) datacenter.getHostList().get(2);

        Assert.assertEquals(hostCapacity, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host1.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host1.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host1.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host2.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host2.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host2.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(hostCapacity, host3.getAvailableMipsByPriority(0), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host3.getAvailableMipsByPriority(1), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(hostCapacity, host3.getAvailableMipsByPriority(2), ACCEPTABLE_DIFFERENCE);

        //testing size of lists
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size());
        Assert.assertEquals(0, datacenter.getVmsRunning().size());
    }

    private void testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime0() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, vm.getNumberOfPreemptions());
        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {
            if (vm.getId() >= 30 && vm.getId() >= 39) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
        }

    }

    private void testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime1() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, vm.getNumberOfPreemptions());
        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

            if (vm.getId() == 28 || vm.getId() == 29 || (vm.getId() >= 40 && vm.getId() <= 49)) {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());

            } else {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            }

        }

    }

    private void testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime2() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, vm.getNumberOfPreemptions());
        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

            if (vm.getId() == 28 || vm.getId() == 29 || (vm.getId() >= 40 && vm.getId() <= 59)) {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());

            } else {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            }

        }

    }

    private void testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime4() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            if (vm.getId() >= 13 && vm.getId() <= 19) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

            if ((vm.getId() >= 22 && vm.getId() <= 27) || (vm.getId() >= 30 && vm.getId() <= 39)) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());

            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

        }

    }

    private void testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime5() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            if ((vm.getId() >= 13 && vm.getId() <= 19) || (vm.getId() == 22 || vm.getId() == 23)) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

            if ((vm.getId() >= 24 && vm.getId() <= 27) || (vm.getId() >= 30 && vm.getId() <= 39)) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());

            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

        }

    }

    private void testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime6() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            if ((vm.getId() >= 13 && vm.getId() <= 19) || (vm.getId() >= 24 && vm.getId() <= 27) || (vm.getId() >= 30 && vm.getId() <= 39)) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

        }

        for (PreemptableVm vm : datacenter.getVmsForScheduling()) {
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

            if (vm.getId() >= 37 && vm.getId() <= 39) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());

            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

        }

    }

    private void testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime7() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            if ((vm.getId() >= 37 && vm.getId() <= 39)) {
                Assert.assertEquals(1, vm.getNumberOfPreemptions());
            } else {
                Assert.assertEquals(0, vm.getNumberOfPreemptions());
            }

            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

        }

        Assert.assertTrue(datacenter.getVmsForScheduling().isEmpty());

    }

    private void testNumberOfPreemptionsAndBackfillingOfThreeHostWithTTVPolicyTime8() {

        for (PreemptableVm vm : datacenter.getVmsRunning()) {
            Assert.assertEquals(0, vm.getNumberOfPreemptions());
            Assert.assertEquals(0, vm.getNumberOfBackfillingChoice());

        }

        Assert.assertTrue(datacenter.getVmsForScheduling().isEmpty());

    }

    private void verifyAvailabilityOfThreeHosts() {
        double finishedTime = 0;

        for (Vm vm : vmsP0) {

            PreemptableVm pVm = (PreemptableVm) vm;
            Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());

            // verifying first time allocated
            // ---------------------------------------------
            if (pVm.getId() >= 0 && pVm.getId() <= 8) {
                Assert.assertEquals(0.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            } else {
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            }
            // ----------------------------------------------

            if (pVm.getId() < 3 || pVm.getId() == 9) {
                finishedTime = 7.0;
            } else {
                finishedTime = 6.0;
            }

            Assert.assertEquals(1.0, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
        }

        for (Vm vm : vmsP1) {

            PreemptableVm pVm = (PreemptableVm) vm;


            // verifying first time allocated
            // ---------------------------------------------
            if (pVm.getId() >= 10 && pVm.getId() <= 19) {
                Assert.assertEquals(0.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            } else if (pVm.getId() >= 20 && pVm.getId() <= 27) {
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            } else {
                Assert.assertEquals(4.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            }
            // ----------------------------------------------


            if (pVm.getId() >= 10 && pVm.getId() <= 12) {

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                finishedTime = 4.0;
                Assert.assertEquals(1, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(0, pVm.getNumberOfMigrations());


            } else if (pVm.getId() >= 13 && pVm.getId() <= 19) {

                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

                finishedTime = 7.0;
                Assert.assertEquals(0.571428571, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(0, pVm.getNumberOfMigrations());

            } else if (pVm.getId() >= 20 && pVm.getId() <= 21) {

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                finishedTime = 5.0;
                Assert.assertEquals(1.0, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(0, pVm.getNumberOfMigrations());

            } else if (pVm.getId() >= 22 && pVm.getId() <= 23) {
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

                finishedTime = 6.0;
                Assert.assertEquals(0.8, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(1, pVm.getNumberOfMigrations());

            } else if (pVm.getId() >= 24 && pVm.getId() <= 27) {

                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

                finishedTime = 7.0;
                Assert.assertEquals(0.666666667, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(1, pVm.getNumberOfMigrations());

            } else if (pVm.getId() >= 28 && pVm.getId() <= 29) {

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                finishedTime = 8.0;
                Assert.assertEquals(0.571428571, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
            } else {
                fail("Some VMP1 was not verifyed");
            }


        }

        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;

            // verifying first time allocated
            // ---------------------------------------------
            if (pVm.getId() >= 30 && pVm.getId() <= 39) {
                Assert.assertEquals(0.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            } else if (pVm.getId() >= 40 && pVm.getId() <= 43) {
                Assert.assertEquals(4.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            } else if (pVm.getId() >= 44 && pVm.getId() <= 49) {
                Assert.assertEquals(6.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            } else {
                Assert.assertEquals(7.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            }
            // ----------------------------------------------

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            if (pVm.getId() >= 30 && pVm.getId() <= 39) {

                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

                if (pVm.getId() >= 30 && pVm.getId() <= 36) {
                    finishedTime = 7.0;
                    Assert.assertEquals(0.285714286, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                } else {
                    finishedTime = 8.0;
                    Assert.assertEquals(0.25, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                }

                if (pVm.getId() == 30 || pVm.getId() == 31 || pVm.getId() == 38 || pVm.getId() == 39) {
                    Assert.assertEquals(1, pVm.getNumberOfMigrations());
                } else {
                    Assert.assertEquals(0, pVm.getNumberOfMigrations());
                }

            } else if (pVm.getId() >= 40 && pVm.getId() <= 43) {
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                finishedTime = 6.0;

                Assert.assertEquals(0.4, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            } else if (pVm.getId() >= 44 && pVm.getId() <= 49) {

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                finishedTime = 8.0;

                Assert.assertEquals(0.285714286, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            } else if (pVm.getId() >= 50 && pVm.getId() <= 59) {

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                finishedTime = 9.0;

                Assert.assertEquals(0.285714286, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            } else {
                fail("Some VMP2 was not verifyied");
            }
        }
    }

    @Test
    public void testSystem10HostsWithWorstFitTTVPolicy() throws Exception{
        Log.enable();

        hostCapacity = 1;
        properties.setProperty("preemption_policy_class",
                "org.cloudbus.cloudsim.preemption.policies.preemption.TTVBasedPreemptionPolicy");

        // creating host
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList1 = new ArrayList<>();
        peList1.add(new Pe(0, new PeProvisionerSimple(hostCapacity)));

        for (int id = 0; id < 10; id++) {

            PreemptiveHost host = new PreemptiveHost(id, peList1, new VmSchedulerMipsBased(
                    peList1), new TTVBasedPreemptionPolicy(properties));
            hostList.add(host);
        }

        List<PreemptiveHost> preemptiveHostList = new ArrayList<>();
        for (Host host : hostList) {
            preemptiveHostList.add((PreemptiveHost) host);
        }

        preemptableVmAllocationPolicy = new WorstFitAvailabilityAwareVmAllocationPolicy(preemptiveHostList);

        Mockito.when(characteristics.getHostList()).thenReturn(hostList);

        // First step: Initialize the CloudSim package. It should be called
        // before creating any entities.
        int num_user = 1; // number of grid users
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false; // mean trace events

        // Initialize the CloudSim library
        CloudSim.init(num_user, calendar, trace_flag);
        CloudSim.runStart();

        // creating data center
            datacenter = new PreemptiveDatacenter("datacenter", characteristics, preemptableVmAllocationPolicy,
                    new LinkedList<Storage>(), 0, properties);

        //asserting hosts in datacenter and host total capacity

        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(hostList.get(i), datacenter.getHostList().get(i));
            Assert.assertEquals(hostCapacity, datacenter.getHostList().get(i).getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        }

        populateVmListsNewTrace();

        submitEventsNewTrace();
        CloudSim.runClockTick();


        advanceTime(0.0);
        //allocating vms with submit time 0
        testSimulation10HostsRuntime0();

        advanceTime(1.0);
        //allocating vms with submit time 1
        testSimulation10HostsRuntime1();

        advanceTime(2.0);
        //allocating vms with submit time 2
        testSimulation10HostsRuntime2();

        advanceTime(3.0);
        //nothing happens
        testSimulation10HostsRuntime2();

        advanceTime(4.0);
        //allocating vms with submit time 1
        testSimulation10HostsRuntime2();

        advanceTime(5.0);
        testSimulation10HostsRuntime5();

        advanceTime(6.0);
        testSimulation10HostsRuntime6();

        advanceTime(7.0);
        testSimulation10HostsRuntime7();

        advanceTime(8.0);
        testSimulation10HostsRuntime8();

        advanceTime(9.0);
        testSimulation10HostsRuntime9();

        verifyAvailabilityOf10Hosts();
    }

    private void testSimulation10HostsRuntime0() {

        Assert.assertEquals(29, datacenter.getVmsRunning().size());
        Assert.assertTrue(datacenter.getVmsForScheduling().isEmpty());

        for (Host host : datacenter.getHostList()) {

            PreemptiveHost pHost = (PreemptiveHost) host;

            if (pHost.getId() < 7) {
                Assert.assertEquals(0d, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.2, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0d, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);

            } else if (pHost.getId() == 7) {
                Assert.assertEquals(0.2, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.2, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.2, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);

            } else if (pHost.getId() == 8) {
                Assert.assertEquals(0.1, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.1, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);

            } else {
                Assert.assertEquals(0.2, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1.0, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.4, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.2, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);
            }
        }
        testVmsStatisticsRuntime0And10Hosts();
    }

    private void testVmsStatisticsRuntime0And10Hosts() {

        for (Vm vm : vmsP0) {
            if (vm.getId() != 9) {

                PreemptableVm pVm = (PreemptableVm) vm;

                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                Assert.assertEquals(pVm.getId(), pVm.getHost().getId());
            }
        }


        int hostIdex = 0;
        for (Vm vm : vmsP1) {

            if (vm.getId() < 20) {
                PreemptableVm pVm = (PreemptableVm) vm;

                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                if (pVm.getId() == 10 || pVm.getId() == 11) {
                    Assert.assertTrue(pVm.getHost().equals(datacenter.getHostList().get(9)));
                } else {
                    Assert.assertTrue(pVm.getHost().equals(datacenter.getHostList().get(hostIdex++)));
                }
            }
        }

        hostIdex = 0;

        for (Vm vm : vmsP2) {

            if (vm.getId() < 40) {
                PreemptableVm pVm = (PreemptableVm) vm;

                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                if (pVm.getId() == 30 || pVm.getId() == 32) {
                    Assert.assertTrue(pVm.getHost().equals(datacenter.getHostList().get(8)));
                } else if (pVm.getId() == 31) {
                    Assert.assertTrue(pVm.getHost().equals(datacenter.getHostList().get(9)));
                } else {
                    Assert.assertTrue(pVm.getHost().equals(datacenter.getHostList().get(hostIdex++)));
                }
            }
        }
    }

    private void testSimulation10HostsRuntime1() {

        Assert.assertEquals(20, datacenter.getVmsRunning().size());
        Assert.assertEquals(30, datacenter.getVmsForScheduling().size());

        for (Host host : datacenter.getHostList()) {

            PreemptiveHost pHost = (PreemptiveHost) host;

            Assert.assertEquals(0d, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0d, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0d, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);

        }

        testVmsStatisticsRuntime1And10Hosts();
    }

    private void testVmsStatisticsRuntime1And10Hosts() {

        test10HostsVmsP0StatisticsTime1AndAfter();
        test10HostsVmsP1StatisticsBetweenTime1And5();
        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());

            Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));

            if (pVm.getId() < 50)
                Assert.assertTrue(datacenter.getVmsForScheduling().contains(pVm));

            if (vm.getId() < 40) {
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

            } else if (vm.getId() < 50) {
                Assert.assertEquals(-1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            }
        }
    }

    private void test10HostsVmsP2StatisticsBetweenTime2And5() {
        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());

            Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
            Assert.assertTrue(datacenter.getVmsForScheduling().contains(pVm));

            if (vm.getId() < 40) {
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

            } else if (vm.getId() < 50) {
                Assert.assertEquals(-1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            }
        }
    }

    private void test10HostsVmsP1StatisticsBetweenTime1And5() {
        for (Vm vm : vmsP1) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());

            if (vm.getId() < 20) {

                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());
                Assert.assertTrue(datacenter.getVmsForScheduling().contains(pVm));
                Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));

            } else {

                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));
                Assert.assertEquals(pVm.getId() - 20, pVm.getHost().getId());
            }
        }
    }

    private void test10HostsVmsP0StatisticsTime1AndAfter() {
        for (Vm vm : vmsP0) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());
            Assert.assertEquals(0, pVm.getNumberOfPreemptions());

            Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
            Assert.assertEquals(pVm.getId(), pVm.getHost().getId());

            if (pVm.getId() < 9)
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
        }
    }

    private void testSimulation10HostsRuntime2() {

        Assert.assertEquals(20, datacenter.getVmsRunning().size());
        Assert.assertEquals(40, datacenter.getVmsForScheduling().size());

        for (Host host : datacenter.getHostList()) {

            PreemptiveHost pHost = (PreemptiveHost) host;

            Assert.assertEquals(0d, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0d, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0d, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);
        }

        test10HostsVmsP0StatisticsTime1AndAfter();
        test10HostsVmsP1StatisticsBetweenTime1And5();
        test10HostsVmsP2StatisticsBetweenTime2And5();
    }

    private void testSimulation10HostsRuntime5() {

        Assert.assertEquals(30, datacenter.getVmsRunning().size());
        Assert.assertEquals(20, datacenter.getVmsForScheduling().size());

        for (Host host : datacenter.getHostList()) {

            PreemptiveHost pHost = (PreemptiveHost) host;

            Assert.assertEquals(0d, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0.2, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0d, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);
        }

        test10HostsVmsStatisticsTime5();
    }

    private void test10HostsVmsStatisticsTime5() {

        test10HostsVmsP0StatisticsTime1AndAfter();
        test10HostsVmsP1StatisticsTime5To7();

        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());


            if (vm.getId() >= 40 && vm.getId() < 50) {

                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));

            } else {

                Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
                Assert.assertTrue(datacenter.getVmsForScheduling().contains(pVm));
            }

            if (vm.getId() < 40) {
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

            } else {

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                if (vm.getId() < 50) {
                    Assert.assertEquals(5.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                    Assert.assertEquals(pVm.getId() - 40, pVm.getHost().getId(), ACCEPTABLE_DIFFERENCE);

                } else
                    Assert.assertEquals(-1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            }
        }
    }

    private void test10HostsVmsP1StatisticsTime5To7() {
        for (Vm vm : vmsP1) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            if (vm.getId() < 20) {

                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));
                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                Assert.assertEquals(pVm.getId() - 10, pVm.getHost().getId());
                Assert.assertEquals(1, pVm.getNumberOfMigrations());

            } else {
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));
            }
        }
    }

    private void testSimulation10HostsRuntime6() {

        Assert.assertEquals(36, datacenter.getVmsRunning().size());
        Assert.assertEquals(8, datacenter.getVmsForScheduling().size());

        for (Host host : datacenter.getHostList()) {

            PreemptiveHost pHost = (PreemptiveHost) host;

            if (pHost.getId() < 3 || pHost.getId() == 9) {

                Assert.assertEquals(0d, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.2, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0d, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);

            } else {

                Assert.assertEquals(0.1, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1.0, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.7, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.1, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);
            }
        }

        test10HostsVmsStatisticsTime6();
    }

    private void test10HostsVmsStatisticsTime6() {

        for (Vm vm : vmsP0) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());
            Assert.assertEquals(0, pVm.getNumberOfPreemptions());

            if (pVm.getId() < 3 || pVm.getId() == 9) {

                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));
                Assert.assertEquals(pVm.getId(), pVm.getHost().getId());

            } else {

                Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));
            }

            if (pVm.getId() < 9)
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
        }

        test10HostsVmsP1StatisticsTime5To7();

        int hostId = 3;

        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            if (pVm.getId() >= 40) {

                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                if (pVm.getId() < 52) {

                    Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                    Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));

                    if (pVm.getId() == 50)
                        Assert.assertEquals(7, pVm.getHost().getId());

                    else if (pVm.getId() == 51)
                        Assert.assertEquals(8, pVm.getHost().getId());

                    else
                        Assert.assertEquals(pVm.getId() - 40, pVm.getHost().getId());

                } else {

                    Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
                    Assert.assertTrue(datacenter.getVmsForScheduling().contains(pVm));
                }

            } else {

                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

                if (pVm.getId() < 36)
                    Assert.assertEquals(1, pVm.getNumberOfMigrations());

                else
                    Assert.assertEquals(0, pVm.getNumberOfMigrations());

                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));
                Assert.assertEquals(hostId++, pVm.getHost().getId());

                if (hostId > 8) hostId = 3;
            }
        }
    }

    private void testSimulation10HostsRuntime7() {

        Assert.assertEquals(20, datacenter.getVmsRunning().size());
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size());

        for (Host host : datacenter.getHostList()) {

            PreemptiveHost pHost = (PreemptiveHost) host;

            Assert.assertEquals(0.5, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(1.0, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0.7, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(0.5, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);
        }

        test10HostsVmsStatisticsTime7();
    }

    private void test10HostsVmsStatisticsTime7() {

        test10HostsVmsP0StatisticsTime7AndAfter();
        test10HostsVmsP1StatisticsTime5To7();
        test10HostsVmsP1StatisticsTime7And8();
    }

    private void test10HostsVmsP1StatisticsTime7And8() {
        int hostId = 7;
        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            if (pVm.getId() >= 40)
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            else
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

            if (pVm.getId() >= 30 && pVm.getId() <= 35)
                Assert.assertEquals(1, pVm.getNumberOfMigrations());
            else
                Assert.assertEquals(0, pVm.getNumberOfMigrations());

            if (pVm.getId() >= 50) {

                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));

                if (pVm.getId() == 59)
                    Assert.assertEquals(9, pVm.getHost().getId());

                else {

                    Assert.assertEquals(hostId++, pVm.getHost().getId());
                    if (hostId > 8) hostId = 0;
                }

            } else {
                Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));
            }

            if (pVm.getId() < 40)
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else if (pVm.getId() < 50)
                Assert.assertEquals(5.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else if (pVm.getId() < 52)
                Assert.assertEquals(6.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else
                Assert.assertEquals(7.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
        }
    }

    private void test10HostsVmsP0StatisticsTime7AndAfter() {

        for (Vm vm : vmsP0) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());
            Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
            Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));

            if (pVm.getId() < 9)
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
        }

    }

    private void testSimulation10HostsRuntime8() {

        Assert.assertEquals(8, datacenter.getVmsRunning().size());
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size());

        for (Host host : datacenter.getHostList()) {

            PreemptiveHost pHost = (PreemptiveHost) host;

            if (pHost.getId() == 7 || pHost.getId() == 8) {

                Assert.assertEquals(1.0, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1.0, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1.0, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1.0, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);

            } else {

                Assert.assertEquals(0.8, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1.0, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1.0, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0.8, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);
            }
        }

        test10HostsVmsStatisticsTime8();

    }

    private void test10HostsVmsStatisticsTime8() {
        test10HostsVmsP0StatisticsTime7AndAfter();
        test10HostsVmsP1StatisticsTime8AndAfter();

        int hostId = 0;
        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            if (pVm.getId() >= 40)
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            else
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

            if (pVm.getId() >= 30 && pVm.getId() <= 35)
                Assert.assertEquals(1, pVm.getNumberOfMigrations());
            else
                Assert.assertEquals(0, pVm.getNumberOfMigrations());

            if (pVm.getId() >= 52) {

                Assert.assertTrue(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));

                if (pVm.getId() == 59)
                    Assert.assertEquals(9, pVm.getHost().getId());

                else
                    Assert.assertEquals(hostId++, pVm.getHost().getId());

            } else {
                Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
                Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));
            }

            if (pVm.getId() < 40)
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else if (pVm.getId() < 50)
                Assert.assertEquals(5.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else if (pVm.getId() < 52)
                Assert.assertEquals(6.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else
                Assert.assertEquals(7.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
        }
    }

    private void test10HostsVmsP1StatisticsTime8AndAfter() {
        for (Vm vm : vmsP1) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
            Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));

            if (vm.getId() < 20) {

                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());
                Assert.assertEquals(1, pVm.getNumberOfMigrations());

            } else {
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            }
        }
    }

    private void testSimulation10HostsRuntime9() {

        Assert.assertEquals(0, datacenter.getVmsRunning().size());
        Assert.assertEquals(0, datacenter.getVmsForScheduling().size());

        for (Host host : datacenter.getHostList()) {
            PreemptiveHost pHost = (PreemptiveHost) host;

            Assert.assertEquals(hostCapacity, pHost.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(hostCapacity, pHost.getAvailableMipsByPriority(PROD), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(hostCapacity, pHost.getAvailableMipsByPriority(BATCH), ACCEPTABLE_DIFFERENCE);
            Assert.assertEquals(hostCapacity, pHost.getAvailableMipsByPriority(FREE), ACCEPTABLE_DIFFERENCE);
        }

        test10HostsVmsStatisticsTime9();
    }

    private void test10HostsVmsStatisticsTime9() {

        test10HostsVmsP0StatisticsTime7AndAfter();
        test10HostsVmsP1StatisticsTime8AndAfter();

        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;
            Assert.assertFalse(datacenter.getVmsRunning().contains(pVm));
            Assert.assertFalse(datacenter.getVmsForScheduling().contains(pVm));

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            if (pVm.getId() >= 40) {
                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            }
            else {
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());
            }

            if (pVm.getId() >= 30 && pVm.getId() <= 35) {
                Assert.assertEquals(1, pVm.getNumberOfMigrations());
            }
            else {
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
            }

            if (pVm.getId() < 40)
                Assert.assertEquals(0d, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else if (pVm.getId() < 50)
                Assert.assertEquals(5.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else if (pVm.getId() < 52)
                Assert.assertEquals(6.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            else
                Assert.assertEquals(7.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
        }
    }

    private void verifyAvailabilityOf10Hosts() {


        testAvaiabilityVmsP0();
        testAvaiabilityVmsP1();

        double finishedTime = 0;
        for (Vm vm : vmsP2) {

            PreemptableVm pVm = (PreemptableVm) vm;

            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());

            if (pVm.getId() >= 30 && pVm.getId() <= 39) {

                finishedTime = 7.0;
                Assert.assertEquals(1, pVm.getNumberOfPreemptions());
                Assert.assertEquals(0.285714286, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

                if (pVm.getId() < 36) {
                    Assert.assertEquals(1, pVm.getNumberOfMigrations());
                }

                else {
                    Assert.assertEquals(0, pVm.getNumberOfMigrations());
                }

            } else if (pVm.getId() >= 40 && pVm.getId() <= 49) {

                finishedTime = 7.0;

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertEquals(0.333333333, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(5.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            } else if (pVm.getId() == 50 || pVm.getId() == 51) {

                finishedTime = 8.0;

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertEquals(0.333333333, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(6.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            } else { // 50 to 59
                finishedTime = 9.0;

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());
                Assert.assertEquals(0.285714286, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(7.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);
            }
        }
    }

    private void testAvaiabilityVmsP1() {
        double finishedTime;
        for (Vm vm : vmsP1) {

            PreemptableVm pVm = (PreemptableVm) vm;

            if (pVm.getId() >= 10 && pVm.getId() <= 19) {

                Assert.assertEquals(1, pVm.getNumberOfPreemptions());

                finishedTime = 8.0;
                Assert.assertEquals(0.5, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(1, pVm.getNumberOfMigrations());
                Assert.assertEquals(0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);


            } else { // 20-29

                Assert.assertEquals(0, pVm.getNumberOfPreemptions());

                finishedTime = 5.0;
                Assert.assertEquals(1, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
                Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
                Assert.assertEquals(0, pVm.getNumberOfMigrations());
                Assert.assertEquals(1.0, pVm.getFirstTimeAllocated(), ACCEPTABLE_DIFFERENCE);

            }
        }
    }

    private void testAvaiabilityVmsP0() {
        double finishedTime;
        for (Vm vm : vmsP0) {

            PreemptableVm pVm = (PreemptableVm) vm;
            Assert.assertEquals(0, pVm.getNumberOfPreemptions());
            Assert.assertEquals(0, pVm.getNumberOfBackfillingChoice());
            Assert.assertEquals(0, pVm.getNumberOfMigrations());

            if (pVm.getId() < 3 || pVm.getId() == 9) {
                finishedTime = 7.0;
            } else {
                finishedTime = 6.0;
            }

            Assert.assertEquals(1.0, pVm.getCurrentAvailability(finishedTime), ACCEPTABLE_DIFFERENCE);
        }
    }
}
