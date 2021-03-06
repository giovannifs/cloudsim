/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.preemption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.lists.PeList;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.preemption.PreemptableVm;
import org.cloudbus.cloudsim.preemption.VmSchedulerMipsBased;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class VmSchedulerMipsBasedTest {

    private static final double MIPS = 1000;
    private static final double ACCEPTABLE_DIFFERENCE = 0.00000001;

    private VmSchedulerMipsBased vmScheduler;

    private VmSchedulerMipsBased googleVMScheduler;

    private List<Pe> peList;

    private List<Pe> googlePeList;

    private Vm vm1;

    private Vm vm2;

    private Vm vm3, vm4, vm5, vm6, vm7, vm8;

    @Before
    public void setUp() throws Exception {
        peList = new ArrayList<Pe>();
        peList.add(new Pe(0, new PeProvisionerSimple(MIPS)));
        peList.add(new Pe(1, new PeProvisionerSimple(MIPS)));
        vmScheduler = new VmSchedulerMipsBased(peList);
        vm1 = new Vm(0, 0, MIPS / 4, 1, 0, 0, 0, "", null);
        vm2 = new Vm(1, 0, MIPS / 2, 2, 0, 0, 0, "", null);


        // googleSetUp
        googlePeList = new ArrayList<Pe>();
        googlePeList.add(new Pe(2, new PeProvisionerSimple(MIPS)));
        googleVMScheduler = new VmSchedulerMipsBased(googlePeList);
        vm3 = new Vm(3, 0, MIPS / 5, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared()); // 200 mips
        vm4 = new Vm(4, 0, MIPS / 4, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared()); // 250 mips
        vm5 = new Vm(5, 0, MIPS / 10, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared()); // 100 mips
        vm6 = new Vm(6, 0, MIPS / 2, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared()); // 500 mips
        vm7 = new Vm(7, 0, MIPS / 5, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared()); // 200 mips
        vm8 = new Vm(8, 0, MIPS / 4, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared()); // 250 mips
    }

    @Test
    public void testInit() {
        assertSame(peList, vmScheduler.getPeList());
        assertEquals(PeList.getTotalMips(peList), vmScheduler.getAvailableMips(), 0);
        assertEquals(PeList.getTotalMips(peList), vmScheduler.getMaxAvailableMips(), 0);
        assertEquals(0, vmScheduler.getTotalAllocatedMipsForVm(vm1), 0);
    }

    @Test
    public void testGoogleScenarioInit() {
        assertEquals(PeList.getTotalMips(googlePeList), googleVMScheduler.getAvailableMips(), 0);
        assertEquals(PeList.getTotalMips(googlePeList), googleVMScheduler.getMaxAvailableMips(), 0);
        assertEquals(PeList.getTotalMips(googlePeList), googleVMScheduler.getTotalMips(), 0);
        assertEquals(0, googleVMScheduler.getTotalAllocatedMipsForVm(vm3), 0);
        assertEquals(0, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
    }

    @Test
    public void testGoogleScenarioAllocate() {

        // allocate Vm3
        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm3, vm3.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(200, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(800, googleVMScheduler.getAvailableMips(), 0);

        // allocate Vm4
        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm4, vm4.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(450, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(550, googleVMScheduler.getAvailableMips(), 0);


        // allocate Vm5
        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm5, vm5.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(550, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(450, googleVMScheduler.getAvailableMips(), 0);


        // // can not alocatte Vm6
        Assert.assertFalse(googleVMScheduler.allocatePesForVm(vm6, vm6.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(550, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(450, googleVMScheduler.getAvailableMips(), 0);

        // allocate Vm7
        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm7, vm7.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(750, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(250, googleVMScheduler.getAvailableMips(), 0);

        // allocate Vm8
        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm8, vm8.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(1000, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(0, googleVMScheduler.getAvailableMips(), 0);
    }

    @Test
    public void testGoogleScenarioDeallocate() {
        // allocate vm3
        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm3, vm3.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(200, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(800, googleVMScheduler.getAvailableMips(), 0);

        // allocate Vm4
        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm4, vm4.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(450, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(550, googleVMScheduler.getAvailableMips(), 0);

        // try to remove vm1, but it doesn't exists
        googleVMScheduler.deallocatePesForVm(vm1);
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(450, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(550, googleVMScheduler.getAvailableMips(), 0);

        // deallocate vm3
        googleVMScheduler.deallocatePesForVm(vm3);
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(250, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(750, googleVMScheduler.getAvailableMips(), 0);

        // try to remove vm3, but it doesn't exist
        googleVMScheduler.deallocatePesForVm(vm3);
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(250, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(750, googleVMScheduler.getAvailableMips(), 0);

        // deallocate vm4
        googleVMScheduler.deallocatePesForVm(vm4);
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(0, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(1000, googleVMScheduler.getAvailableMips(), 0);

        // allocate vm3, vm4 and vm5
        googleVMScheduler.allocatePesForVm(vm3, vm3.getCurrentRequestedMips());
        googleVMScheduler.allocatePesForVm(vm4, vm4.getCurrentRequestedMips());
        googleVMScheduler.allocatePesForVm(vm5, vm5.getCurrentRequestedMips());

        // can not allocate vm6
        Assert.assertFalse(googleVMScheduler.allocatePesForVm(vm6, vm6.getCurrentRequestedMips()));
        assertEquals(0, googleVMScheduler.getTotalAllocatedMipsForVm(vm6), 0);

        // deallocate vm5
        googleVMScheduler.deallocatePesForVm(vm5);
        assertEquals(0, googleVMScheduler.getTotalAllocatedMipsForVm(vm5), 0);

        // now, we can allocate vm6
        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm6, vm6.getCurrentRequestedMips()));
        assertEquals(500, googleVMScheduler.getTotalAllocatedMipsForVm(vm6), 0);


        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(950, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(50, googleVMScheduler.getAvailableMips(), 0);

        // can not allocate vm5
        Assert.assertFalse(googleVMScheduler.allocatePesForVm(vm5, vm5.getCurrentRequestedMips()));
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(950, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(50, googleVMScheduler.getAvailableMips(), 0);

        // now, vm5 doesn't exist
        googleVMScheduler.deallocatePesForVm(vm5);
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(950, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(50, googleVMScheduler.getAvailableMips(), 0);


        // deallocate all Vms
        googleVMScheduler.deallocatePesForVm(vm3);
        googleVMScheduler.deallocatePesForVm(vm4);
        googleVMScheduler.deallocatePesForVm(vm6);
        assertEquals(1000, googleVMScheduler.getTotalMips(), 0);
        assertEquals(0, googleVMScheduler.getMipsInUse(), 0);
        assertEquals(1000, googleVMScheduler.getAvailableMips(), 0);

    }

    @Test
    public void testAllocatePesForVm() {

        List<Double> mipsShare1 = new ArrayList<Double>();
        mipsShare1.add(MIPS / 4);

        assertTrue(vmScheduler.allocatePesForVm(vm1, mipsShare1));

        assertEquals(PeList.getTotalMips(peList) - MIPS / 4, vmScheduler.getAvailableMips(), 0);
        assertEquals(PeList.getTotalMips(peList) - MIPS / 4, vmScheduler.getMaxAvailableMips(), 0);
        assertEquals(MIPS / 4, vmScheduler.getTotalAllocatedMipsForVm(vm1), 0);

        List<Double> mipsShare2 = new ArrayList<Double>();
        mipsShare2.add(MIPS / 2);
        mipsShare2.add(MIPS / 8);

        assertTrue(vmScheduler.allocatePesForVm(vm2, mipsShare2));

        assertEquals(
                PeList.getTotalMips(peList) - MIPS / 4 - MIPS / 2 - MIPS / 8,
                vmScheduler.getAvailableMips(),
                0);
        assertEquals(
                PeList.getTotalMips(peList) - MIPS / 4 - MIPS / 2 - MIPS / 8,
                vmScheduler.getMaxAvailableMips(),
                0);
        assertEquals(MIPS / 2 + MIPS / 8, vmScheduler.getTotalAllocatedMipsForVm(vm2), 0);

        // List<Double> mipsShare3 = new ArrayList<Double>();
        // mipsShare3.add(MIPS);
        // mipsShare3.add(MIPS);
        //
        // assertTrue(vmScheduler.allocatePesForVm(vm3, mipsShare3));
        //
        // assertEquals(0, vmScheduler.getAvailableMips(), 0);
        // assertEquals(0, vmScheduler.getMaxAvailableMips(), 0);
        // assertEquals(MIPS / 4 - (MIPS / 4 + MIPS / 2 + MIPS / 8 + MIPS + MIPS - MIPS * 2) / 5,
        // vmScheduler.getTotalAllocatedMipsForVm(vm1), 0);
        // assertEquals(MIPS / 2 + MIPS / 8 - (MIPS / 4 + MIPS / 2 + MIPS / 8 + MIPS + MIPS - MIPS *
        // 2) * 2 / 5, vmScheduler.getTotalAllocatedMipsForVm(vm2), 0);
        // assertEquals(MIPS * 2 - (MIPS / 4 + MIPS / 2 + MIPS / 8 + MIPS + MIPS - MIPS * 2) * 2 /
        // 5, vmScheduler.getTotalAllocatedMipsForVm(vm3), 0);
        //
        // vmScheduler.deallocatePesForVm(vm1);
        //
        // assertEquals(0, vmScheduler.getAvailableMips(), 0);
        // assertEquals(0, vmScheduler.getMaxAvailableMips(), 0);
        // assertEquals(MIPS / 2 + MIPS / 8 - (MIPS / 2 + MIPS / 8 + MIPS + MIPS - MIPS * 2) * 2 /
        // 4, vmScheduler.getTotalAllocatedMipsForVm(vm2), 0);
        // assertEquals(MIPS * 2 - (MIPS / 2 + MIPS / 8 + MIPS + MIPS - MIPS * 2) * 2 / 4,
        // vmScheduler.getTotalAllocatedMipsForVm(vm3), 0);
        //
        // vmScheduler.deallocatePesForVm(vm3);
        //
        // assertEquals(MIPS * 2 - MIPS / 2 - MIPS / 8, vmScheduler.getAvailableMips(), 0);
        // assertEquals(MIPS * 2 - MIPS / 2 - MIPS / 8, vmScheduler.getMaxAvailableMips(), 0);
        // assertEquals(0, vmScheduler.getTotalAllocatedMipsForVm(vm3), 0);
        //
        // vmScheduler.deallocatePesForVm(vm2);

        vmScheduler.deallocatePesForAllVms();

        assertEquals(PeList.getTotalMips(peList), vmScheduler.getAvailableMips(), 0);
        assertEquals(PeList.getTotalMips(peList), vmScheduler.getMaxAvailableMips(), 0);
        assertEquals(0, vmScheduler.getTotalAllocatedMipsForVm(vm2), 0);

        // test allocate Vm with requested mips is bigger than capacity
        Vm vm9 = new Vm(9, 0, MIPS + 0.1, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared());

        Assert.assertFalse(vmScheduler.allocatePesForVm(vm9, vm9.getCurrentRequestedMips()));
        assertEquals(PeList.getTotalMips(peList), vmScheduler.getAvailableMips(), 0);
        assertEquals(PeList.getTotalMips(peList), vmScheduler.getMaxAvailableMips(), 0);

        Assert.assertFalse(googleVMScheduler.allocatePesForVm(vm9, vm9.getCurrentRequestedMips()));
        assertEquals(PeList.getTotalMips(googlePeList), googleVMScheduler.getAvailableMips(), 0);
        assertEquals(PeList.getTotalMips(googlePeList), googleVMScheduler.getMaxAvailableMips(), 0);


        // test allocate vms in limit of capacity
        Vm vm10 = new Vm(10, 0, 0.1, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared());
        Vm vm11 = new Vm(11, 0, MIPS - 0.1, 1, 0, 0, 0, "", new CloudletSchedulerTimeShared());

        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm11, vm11.getCurrentRequestedMips()));
        Assert.assertEquals(MIPS - (MIPS - 0.1), googleVMScheduler.getMaxAvailableMips(), ACCEPTABLE_DIFFERENCE);
        Assert.assertEquals(MIPS - (MIPS - 0.1), googleVMScheduler.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertTrue(googleVMScheduler.allocatePesForVm(vm10, vm10.getCurrentRequestedMips()));
        assertEquals(0, googleVMScheduler.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        assertEquals(0, googleVMScheduler.getMaxAvailableMips(), ACCEPTABLE_DIFFERENCE);

    }

    @Test
    public void testAllocatePesForVM2(){

        double ACCETABLE_DIFFERENCE = 0.000000000000001;

        int id = 0;
        int userId = 1;
        double cpuReq = 0.00000001;
        double memReq = 0;
        double subTime = 0;
        int priority = 1;
        double runTime = 0.4;

        List<Pe> peList1 = new ArrayList<Pe>();
        peList1.add(new Pe(0, new PeProvisionerSimple(5 * cpuReq)));
        VmScheduler schedulerMipsBased = new VmSchedulerMipsBased(peList1);

        Vm vm1 = new PreemptableVm(id++, userId, 1 * cpuReq, memReq, subTime, priority - 1, runTime);
        Vm vm2 = new PreemptableVm(id++, userId, 5 * cpuReq, memReq, subTime, priority - 1 , runTime);
        Vm vm3 = new PreemptableVm(id++, userId, 2 * cpuReq, memReq, subTime, priority, runTime);
        Vm vm4 = new PreemptableVm(id++, userId, 3 * cpuReq, memReq, subTime, priority, runTime);
        Vm vm5 = new PreemptableVm(id++, userId, 4 * cpuReq, memReq, subTime, priority + 1, runTime);
        Vm vm6 = new PreemptableVm(id++, userId, 6 * cpuReq, memReq, subTime, priority + 1, runTime);

        Assert.assertEquals(5*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm1, vm1.getCurrentRequestedMips()));
        Assert.assertEquals(4*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        Assert.assertFalse(schedulerMipsBased.allocatePesForVm(vm2, vm2.getCurrentRequestedMips()));
        Assert.assertEquals(4*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm3, vm3.getCurrentRequestedMips()));
        Assert.assertEquals(2*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        schedulerMipsBased.deallocatePesForVm(vm3);
        Assert.assertEquals(4*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm5, vm5.getCurrentRequestedMips()));
        Assert.assertEquals(0*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        schedulerMipsBased.deallocatePesForVm(vm1);
        Assert.assertEquals(1*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        schedulerMipsBased.deallocatePesForVm(vm5);
        Assert.assertEquals(5*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        Assert.assertFalse(schedulerMipsBased.allocatePesForVm(vm6, vm6.getCurrentRequestedMips()));
        Assert.assertEquals(5*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm4, vm4.getCurrentRequestedMips()));
        Assert.assertEquals(2*cpuReq, schedulerMipsBased.getAvailableMips(), ACCETABLE_DIFFERENCE);
    }


    @Test
    public void testAllocatePesForVm3() {

        double ACCEPTABLE_DIFFERENCE = 0.000000001;

        int id = 0;
        int userId = 1;
        double cpuReq = 0.00000001;
        double memReq = 0;
        double subTime = 0;
        int priority = 1;
        double runTime = 0.4;

        double cpuCapacity = 6603.25;

        List<Pe> peList1 = new ArrayList<Pe>();
        peList1.add(new Pe(0, new PeProvisionerSimple(cpuCapacity)));
        VmScheduler schedulerMipsBased = new VmSchedulerMipsBased(peList1);

        Vm vm1 = new PreemptableVm(id++, userId, 1 * cpuReq, memReq, subTime, priority - 1, runTime);
        Vm vm2 = new PreemptableVm(id++, userId, 5 * cpuReq, memReq, subTime, priority - 1 , runTime);
        Vm vm3 = new PreemptableVm(id++, userId, 2 * cpuReq, memReq, subTime, priority, runTime);
        Vm vm4 = new PreemptableVm(id++, userId, 3 * cpuReq, memReq, subTime, priority, runTime);
        Vm vm5 = new PreemptableVm(id++, userId, 4 * cpuReq, memReq, subTime, priority + 1, runTime);
        Vm vm6 = new PreemptableVm(id++, userId, 6 * cpuReq, memReq, subTime, priority + 1, runTime);

        Assert.assertEquals(cpuCapacity, schedulerMipsBased.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm1, vm1.getCurrentRequestedMips()));
        Assert.assertEquals(cpuCapacity - cpuReq, schedulerMipsBased.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm2, vm2.getCurrentRequestedMips()));
        Assert.assertEquals(cpuCapacity - (6 * cpuReq), schedulerMipsBased.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm3, vm3.getCurrentRequestedMips()));
        Assert.assertEquals(cpuCapacity - (8 * cpuReq), schedulerMipsBased.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        schedulerMipsBased.deallocatePesForVm(vm2);
        Assert.assertEquals(cpuCapacity - (3 * cpuReq), schedulerMipsBased.getAvailableMips(), ACCEPTABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm4, vm4.getCurrentRequestedMips()));
        Assert.assertEquals(cpuCapacity - (6 * cpuReq), schedulerMipsBased.getAvailableMips(), this.ACCEPTABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm5, vm5.getCurrentRequestedMips()));
        Assert.assertEquals(cpuCapacity - (10 * cpuReq), schedulerMipsBased.getAvailableMips(), this.ACCEPTABLE_DIFFERENCE);

        schedulerMipsBased.deallocatePesForVm(vm1);
        Assert.assertEquals(cpuCapacity - (9 * cpuReq), schedulerMipsBased.getAvailableMips(), this.ACCEPTABLE_DIFFERENCE);

        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm6, vm6.getCurrentRequestedMips()));
        Assert.assertEquals(cpuCapacity - (15 * cpuReq), schedulerMipsBased.getAvailableMips(), this.ACCEPTABLE_DIFFERENCE);

        schedulerMipsBased.deallocatePesForVm(vm3);
        schedulerMipsBased.deallocatePesForVm(vm4);
        schedulerMipsBased.deallocatePesForVm(vm5);
        schedulerMipsBased.deallocatePesForVm(vm6);

        // the list of getCurrentRequestedMips is empty because the vm is not being instantiated
        vm6.setBeingInstantiated(false);
        //the vm is allocated but the available mips is not changed
        Assert.assertTrue(schedulerMipsBased.allocatePesForVm(vm6, vm6.getCurrentRequestedMips()));
        Assert.assertEquals(cpuCapacity, schedulerMipsBased.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
        //the vm is deallocate and the available mips is not changed
        schedulerMipsBased.deallocatePesForVm(vm6);
        Assert.assertEquals(cpuCapacity, schedulerMipsBased.getAvailableMips(), ACCEPTABLE_DIFFERENCE);
    }
        @Test
    public void testGetPeCapacity(){

        double peCapacity = 1000.0;

       Assert.assertEquals(peCapacity, vmScheduler.getPeCapacity(), ACCEPTABLE_DIFFERENCE);
       Assert.assertEquals(peCapacity, googleVMScheduler.getPeCapacity(), ACCEPTABLE_DIFFERENCE);

    }
}
