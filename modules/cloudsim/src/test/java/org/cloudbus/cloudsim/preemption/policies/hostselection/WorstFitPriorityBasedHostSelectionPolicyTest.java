package org.cloudbus.cloudsim.preemption.policies.hostselection;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.preemption.PreemptableVm;
import org.cloudbus.cloudsim.preemption.PreemptiveHost;
import org.cloudbus.cloudsim.preemption.VmSchedulerMipsBased;
import org.cloudbus.cloudsim.preemption.policies.preemption.FCFSBasedPreemptionPolicy;
import org.cloudbus.cloudsim.preemption.policies.preemption.PreemptionPolicy;
import org.cloudbus.cloudsim.preemption.util.PreemptiveHostComparator;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by Alessandro Lia Fook Santos on 23/11/16.
 */
public class WorstFitPriorityBasedHostSelectionPolicyTest {

    public final double ACCEPTABLE_DIFFERENCE = 0.00001;

    public PreemptiveHost host1;
    public PreemptiveHost host2;
    public PreemptiveHost host3;
    public PreemptiveHost host4;
    public PreemptiveHost host5;
    public PreemptiveHost host6;

    public Vm vm1000;
    public Vm vm500;
    public Vm vm250;
    public Vm vm125;
    public Vm vm62;
    public Vm vm0;
    public Vm vm1200;
    public SortedSet<PreemptiveHost> preemptiveHosts;
    public List<PreemptiveHost> hosts;
    public HostSelectionPolicy selectionPolicy;

    Properties properties;

    @Before
    public void setUp() {

        //creating lists of hosts
        preemptiveHosts = new TreeSet<>(new PreemptiveHostComparator(0));
        hosts = new ArrayList<>();

        // populating host list
        List<Pe> peList = new ArrayList<Pe>();
        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        properties = new Properties();
        properties.setProperty(PreemptionPolicy.NUMBER_OF_PRIORITIES_PROP, "3");

        host1 = new PreemptiveHost(1, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host1);
        hosts.add(host1);

        host2 = new PreemptiveHost(2, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host2);
        hosts.add(host2);

        host3 = new PreemptiveHost(3, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host3);
        hosts.add(host3);

        host4 = new PreemptiveHost(4, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host4);
        hosts.add(host4);

        host5 = new PreemptiveHost(5, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host5);
        hosts.add(host5);

        host6 = new PreemptiveHost(6, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host6);
        hosts.add(host6);

        // creating object under test
        selectionPolicy = new WorstFitPriorityBasedHostSelectionPolicy(hosts);

        // creating Vm's
        vm1000 = new PreemptableVm(1, 1, 1000, 0, 0, 0, 0);
        vm500 = new PreemptableVm(2, 1, 500, 0, 0, 0, 0);
        vm250 = new PreemptableVm(3, 1, 250, 0, 0, 0, 0);
        vm125 = new PreemptableVm(4, 1, 125, 0, 0, 0, 0);
        vm62 = new PreemptableVm(5, 1, 62.5, 0, 0, 0, 0);
        vm0 = new PreemptableVm(6, 1, 0, 0, 0, 0, 0);
        vm1200 = new PreemptableVm(7, 1, 1200, 0, 0, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVmEqualsNull() {
        SortedSet<PreemptiveHost> hostList2 = new TreeSet<>(new PreemptiveHostComparator(0));
        selectionPolicy.select(preemptiveHosts, null);
        selectionPolicy.select(hostList2, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHostListEqualsNull() {
        selectionPolicy = new WorstFitPriorityBasedHostSelectionPolicy(null);
        selectionPolicy.select(null, vm1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHostListEmpty() {
        selectionPolicy = new WorstFitPriorityBasedHostSelectionPolicy(new ArrayList<>());
        Assert.assertNull(selectionPolicy.select(new TreeSet<>(), vm1000));
    }


    @Test
    public void testVmBiggerThanFirstHost() {
        Assert.assertNull(selectionPolicy.select(preemptiveHosts, vm1200));
    }

    @Test
    public void testVmMipsEqualsZero() {
        Assert.assertEquals(host1.getId(), (selectionPolicy.select(preemptiveHosts, vm0)).getId());
        Assert.assertEquals(host1.getId(), preemptiveHosts.first().getId());
    }

    @Test
    public void testHostIsFull() {
        SortedSet<PreemptiveHost> hostList2 = new TreeSet<>(new PreemptiveHostComparator(0));
        // adding a single host in the list
        hostList2.add(host1);
        PreemptiveHost host = selectionPolicy.select(hostList2, vm1000);

        //allocate a vm that fills the host
        Assert.assertEquals(host1.getId(), host.getId());
        host.vmCreate(vm1000);

        // try allocate a vm in a list of full host
        Assert.assertNull(selectionPolicy.select(hostList2, vm500));

        // try allocate a vm if mips equals zero
        Assert.assertNotNull(selectionPolicy.select(hostList2, vm0));

    }

    @Test
    public void testAllocatingModifyingFirstHost() {

        PreemptiveHost host = selectionPolicy.select(preemptiveHosts, vm62);

        // test if the selected host is equals the first inserted
        Assert.assertEquals(host1.getId(), host.getId());

        //allocate Vm in the selected host
        preemptiveHosts.remove(host);
        selectionPolicy.removeHost(host);
        host.vmCreate(vm62);
        preemptiveHosts.add(host);
        selectionPolicy.addHost(host);

        // test if the last Host in the list is the host1 now
        Assert.assertEquals(host.getId(), (preemptiveHosts.last()).getId());
        Assert.assertEquals(host1.getId(), (preemptiveHosts.last()).getId());

        //test if the host1 suffer mips changes
        Assert.assertEquals((preemptiveHosts.last()).getAvailableMips(), 937.5, ACCEPTABLE_DIFFERENCE);

        // test if host2 is the new selected
        Assert.assertEquals(host2.getId(), (selectionPolicy.select(preemptiveHosts, vm250)).getId());
    }

    @Test
    public void testAllocatingMultiplesHosts(){

        for (int i = 0; i < 6; i++){
            PreemptiveHost otherHost = selectionPolicy.select(preemptiveHosts, vm1000);
            preemptiveHosts.remove(otherHost);
            selectionPolicy.removeHost(otherHost);
            otherHost.vmCreate(vm1000);
            preemptiveHosts.add(otherHost);
            selectionPolicy.addHost(otherHost);
        }

        // once all hosts are fully occupied test allocation of vm's
        Assert.assertNull(selectionPolicy.select(preemptiveHosts, vm1000));
        Assert.assertNull(selectionPolicy.select(preemptiveHosts, vm500));
        Assert.assertNull(selectionPolicy.select(preemptiveHosts, vm250));
        Assert.assertNull(selectionPolicy.select(preemptiveHosts, vm125));
        Assert.assertNull(selectionPolicy.select(preemptiveHosts, vm62));

        PreemptiveHost otherHost = selectionPolicy.select(preemptiveHosts, vm0);
        Assert.assertEquals(otherHost.getId(), host1.getId());
    }


    @Test
    public void testAllocatingVMsWhereFirstHostIsNotSuitable(){

        //creating hosts
        preemptiveHosts = new TreeSet<PreemptiveHost>(new PreemptiveHostComparator(0));
        hosts = new ArrayList<>();

        List<Pe> peList = new ArrayList<Pe>();
        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        host1 = new PreemptiveHost(1, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        hosts.add(host1);
        preemptiveHosts.add(host1);

        host2 = new PreemptiveHost(2, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        hosts.add(host2);
        preemptiveHosts.add(host2);

        // creating a VM with priority 0
        PreemptableVm vm500P0 = new PreemptableVm(1, 1, 500, 0, 0, 0, 0);

        Assert.assertEquals(host1, selectionPolicy.select(new TreeSet<>(), vm500P0));

        // creating vm and updating host1
        selectionPolicy.removeHost(host1);
        preemptiveHosts.remove(host1);
        host1.vmCreate(vm500P0);
        selectionPolicy.addHost(host1);
        preemptiveHosts.add(host1);

        // creating a VM with priority 2
        PreemptableVm vm700P2 = new PreemptableVm(2, 1, 700, 0, 0, 2, 0);

        Assert.assertEquals(host2, selectionPolicy.select(new TreeSet<>(), vm700P2));

        // creating vm and updating host2
        selectionPolicy.removeHost(host2);
        preemptiveHosts.remove(host2);
        host2.vmCreate(vm700P2);
        selectionPolicy.addHost(host2);
        preemptiveHosts.add(host2);

        // creating a VM with priority 1
        PreemptableVm vm700P1 = new PreemptableVm(3, 1, 700, 0, 0, 1, 0);

		/*
		 * besides host1 is that with more available mips, only host2 is
		 * suitable for vm with priority 1
		 */
        Assert.assertEquals(500, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(300, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertEquals(host2, selectionPolicy.select(new TreeSet<>(), vm700P1));

    }

    @Test
    public void testDoubleValues(){
        preemptiveHosts.clear();
        hosts.clear();

        // create host1 with capacity 62.501
        List<Pe> peList = new ArrayList<Pe>();
        double mips = 62.501;
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        host1 = new PreemptiveHost(1, peList, new VmSchedulerMipsBased(peList), new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host1);
        hosts.add(host1);

        // create host2 with capacity 62.5
        List<Pe> peList2 = new ArrayList<Pe>();
        double mips2 = 62.5;
        peList2.add(new Pe(1, new PeProvisionerSimple(mips2))); // need to store Pe id and MIPS Rating

        host2 = new PreemptiveHost(2, peList2, new VmSchedulerMipsBased(peList2),new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host2);
        hosts.add(host2);

        // create host3 with capacity 62.49
        List<Pe> peList3 = new ArrayList<Pe>();
        double mips3 = 62.49;
        peList3.add(new Pe(2, new PeProvisionerSimple(mips3))); // need to store Pe id and MIPS Rating

        host3 = new PreemptiveHost(3, peList3, new VmSchedulerMipsBased(peList3), new FCFSBasedPreemptionPolicy(properties));
        preemptiveHosts.add(host3);
        hosts.add(host3);

        selectionPolicy = new WorstFitPriorityBasedHostSelectionPolicy(hosts);

        // test if is possible allocate vm62 (with 62.5 mips required) at host1 (its capacity is 62.501)
        Assert.assertEquals(host1, selectionPolicy.select(preemptiveHosts, vm62));
        preemptiveHosts.remove(host1);
        selectionPolicy.removeHost(host1);
        host1.vmCreate(vm62);
        preemptiveHosts.add(host1);
        selectionPolicy.addHost(host1);
        Assert.assertEquals(0.001, host1.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        // test if is possible allocate vm62 (with 62.5 mips required) at host2 (its capacity is 62.5)
        Assert.assertEquals(host2, selectionPolicy.select(preemptiveHosts, vm62));
        preemptiveHosts.remove(host2);
        selectionPolicy.removeHost(host2);
        host2.vmCreate(vm62);
        preemptiveHosts.add(host2);
        selectionPolicy.addHost(host2);
        Assert.assertEquals(0, host2.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        // test if is not possible allocate vm62 (with 62.5 mips required) at host1 (its capacity is 62.49)
        Assert.assertNull(selectionPolicy.select(preemptiveHosts, vm62));
        Assert.assertEquals(62.49, host3.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
    }
}