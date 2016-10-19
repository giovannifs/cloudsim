package org.cloudbus.cloudsim.preemption.datastore;

import org.cloudbus.cloudsim.preemption.PreemptableVm;
import org.cloudbus.cloudsim.preemption.PreemptiveHost;
import org.junit.*;
import org.mockito.Mockito;

import java.io.File;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by Alessandro Lia Fook Santos and Joao Victor Mafra on 19/10/16.
 */
public class PreemptableVmDataStoreTest {

    public static final int TIME = 1;

    private static String databaseFile = "VmDataStoreTest.sqlite3";
    private static String databaseURL = "jdbc:sqlite:" + databaseFile;
    private static Properties properties;

    PreemptableVmDataStore dataStore;

    private SortedSet<PreemptableVm> running;
    private SortedSet<PreemptableVm> waiting;

    private PreemptiveHost host1, host2;

    private PreemptableVm vm1, vm2, vm3, vm4, vm5;

    private int id, priority;
    private static final int USER_ID = 0;
    private double cpuReq, memReq, submitTime, runtime;



    @Before
    public void setUp(){

        // creating the dataStore
        properties = new Properties();
        properties.setProperty(PreemptableVmDataStore.CHECKPOINT_DIR_PROP, databaseURL);

        // creating data store
        dataStore = new PreemptableVmDataStore(properties, TIME);

        // creating structures were the vms are inserted
        running = new TreeSet<>();
        waiting = new TreeSet<>();

        //creating hosts
        host1 = Mockito.mock(PreemptiveHost.class);
        host2 = Mockito.mock(PreemptiveHost.class);

        // creating vms
        id = 0;
        priority = 1;
        cpuReq = 0.002;
        memReq = 0.001;
        submitTime = 0;
        runtime = 1.25;

        vm1 = new PreemptableVm(id++, USER_ID, cpuReq, memReq, submitTime, priority - 1, runtime);
        vm2 = new PreemptableVm(id++, USER_ID, cpuReq, memReq, submitTime, priority - 1, runtime);
        vm3 = new PreemptableVm(id++, USER_ID, cpuReq, memReq, submitTime, priority, runtime);
        vm4 = new PreemptableVm(id++, USER_ID, cpuReq, memReq, submitTime, priority + 1, runtime);

        //setting host for the vms
        vm1.setHost(host1);
        vm2.setHost(host2);
        vm3.setHost(host1);
        vm4.setHost(host2);

        // mocking the id of the host
        Mockito.when(vm1.getHost().getId()).thenReturn(1);
        Mockito.when(vm2.getHost().getId()).thenReturn(2);
        Mockito.when(vm3.getHost().getId()).thenReturn(1);
        Mockito.when(vm4.getHost().getId()).thenReturn(2);


        // assert initial state
        Assert.assertEquals(0, dataStore.getAllRunningVms().size());
        Assert.assertEquals(0, dataStore.getAllWaitingVms().size());
    }

    @After
    public void tearDown() {
        new File(properties.getProperty(PreemptableVmDataStore.CHECKPOINT_DIR_PROP)
                + "vms-" + properties.getProperty("number_of_hosts") + "-hosts-" + String.valueOf(TIME)).delete();
    }

    @Test
    public void testAddSetEqualsNull() {

        SortedSet<PreemptableVm> setNull = null;

        Assert.assertFalse(dataStore.addWaitingVms(setNull));
        Assert.assertFalse(dataStore.addRunningVms(setNull));

    }

    @Test
    public void testAddEmptySet() {

        Assert.assertTrue(dataStore.addWaitingVms(waiting));
        Assert.assertTrue(dataStore.addRunningVms(running));

        Assert.assertEquals(0, dataStore.getAllRunningVms().size());
        Assert.assertEquals(0, dataStore.getAllWaitingVms().size());
    }

    @Test
    public void testAddRunning() {

        running.add(vm1);

        Assert.assertTrue(dataStore.addWaitingVms(waiting));
        Assert.assertTrue(dataStore.addRunningVms(running));

        Assert.assertEquals(1, dataStore.getAllRunningVms().size());
        Assert.assertEquals(0, dataStore.getAllWaitingVms().size());

        for (PreemptableVm vm: dataStore.getAllRunningVms()) {
            Assert.assertTrue(running.contains(vm));
        }
    }
}