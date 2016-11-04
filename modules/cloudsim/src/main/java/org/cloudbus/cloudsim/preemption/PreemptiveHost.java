package org.cloudbus.cloudsim.preemption;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.preemption.policies.preemption.PreemptionPolicy;
import org.cloudbus.cloudsim.preemption.util.DecimalUtil;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class PreemptiveHost extends Host implements Comparable<Host> {
	
//	private Map<Integer, Double> priorityToInUseMips;
//	private Map<Integer, SortedSet<Vm>> priorityToVms;
//	private int numberOfPriorities;
	public static final int DECIMAL_ACCURACY = 9;
		
	private Map<Double, UsageInfo> usageMap;
	private PreemptionPolicy preemptionPolicy;

	public PreemptiveHost(int id, List<? extends Pe> peList, VmScheduler vmScheduler, PreemptionPolicy preemptionPolicy) {
		super(id, new RamProvisionerSimple(Integer.MAX_VALUE),
				new BwProvisionerSimple(Integer.MAX_VALUE), Integer.MAX_VALUE,
				peList, vmScheduler);
		
//		if (numberOfPriorities < 1) {
//			throw new IllegalArgumentException("Number of priorities must be bigger than zero.");
//		}
		
//		setPriorityToVms(new HashMap<Integer, SortedSet<Vm>>());
//		setPriorityToInUseMips(new HashMap<Integer, Double>());
		setUsageMap(new HashMap<Double, UsageInfo>());
		setPreemptionPolicy(preemptionPolicy);
		preemptionPolicy.setTotalMips(((VmSchedulerMipsBased) getVmScheduler())
				.getTotalMips());
//		setNumberOfPriorities(numberOfPriorities);
//		
//		// initializing maps
//		for (int priority = 0; priority < numberOfPriorities; priority++) {
//			getPriorityToVms().put(priority, new TreeSet<Vm>());
//			getPriorityToInUseMips().put(priority, new Double(0));
//		}
	}
    @Override
    public int compareTo(Host other) {
        /*
		 * If this object has bigger amount of available mips it should be
		 * considered before the other one.
		 */
        int result = (-1) * (new Double(getAvailableMips()).compareTo(new Double(other
                .getAvailableMips())));

        if (result == 0)
            return new Integer(getId()).compareTo(new Integer(other.getId()));

        return result;
    }

    @Override
    public int hashCode() {
        return getId();
    }
    
	public Vm nextVmForPreempting() {
//		for (int i = getNumberOfPriorities() - 1; i >= 0; i--) {
//		for (int i = preemptionPolicy.getNumberOfPriorities() - 1; i >= 0; i--) {
//			if (!getPriorityToVms().get(i).isEmpty()) {
//				return getPriorityToVms().get(i).last();
//			}
//		}
//		return null;
		return preemptionPolicy.nextVmForPreempting();
	}

	@Override
	public boolean isSuitableForVm(Vm vm) {

		if (vm == null) {
			return false;
		}

		else if (getVmScheduler().getAvailableMips() >= vm.getMips()) {
			return true;

		} 
//		else {			
//			PreemptableVm gVm = (PreemptableVm) vm;
//			double availableMips = getAvailableMipsByPriority(gVm.getPriority()) ;
//			return (availableMips >= vm.getMips());
//		}	
		return preemptionPolicy.isSuitableFor((PreemptableVm) vm);
	}
	
	@Override
	public boolean vmCreate(Vm vm) {
		/*
		 * TODO The Host class add the VM into a List. We don't need that list.
		 * We may optimize the code.
		 */
		if (vm == null) {
			return false;
		}
		PreemptableVm gVm = (PreemptableVm) vm;

		Log.printConcatLine(CloudSim.clock(), ": Creating VM#", vm.getId(), "(priority ", gVm.getPriority(),") on host #", getId());
		
		boolean result = super.vmCreate(vm);
		
		if (result) {
			// updating maps
			
//			getPriorityToVms().get(gVm.getPriority()).add(gVm);
//			double priorityCurrentUse = getPriorityToInUseMips().get(gVm.getPriority()); 
//			getPriorityToInUseMips().put( gVm.getPriority(),
//					DecimalUtil.format(priorityCurrentUse + gVm.getMips(), DECIMAL_ACCURACY));
			preemptionPolicy.allocating(gVm);
			
			double totalUsage = getTotalUsage();
			Log.printConcatLine(CloudSim.clock(), ": Host #", getId(), " currentTotalUsage=", totalUsage, ", currentAvailableMips=", getAvailableMips());

			if ((totalUsage - getTotalMips()) > 0.00001) {
				throw new SimulationException("The total usage (" + totalUsage
						+ ") on host #" + getId()
						+ " was bigger than the total capacity ("
						+ getTotalMips() + ") while creating VM #" + vm.getId()
						+ ".");
			}
		}
		return result;
	}
	
	public double getTotalUsage() {
		double totalUsage = 0;
		for (Integer priority : preemptionPolicy.getPriorityToInUseMips().keySet()) {
			totalUsage += preemptionPolicy.getPriorityToInUseMips().get(priority);
		}
		return DecimalUtil.format(totalUsage, DECIMAL_ACCURACY);
	}
	
	@Override
	public void vmDestroy(Vm vm) {
		super.vmDestroy(vm);

		// updating maps
		PreemptableVm gVm = (PreemptableVm) vm;
		
		preemptionPolicy.deallocating(gVm);
		
//		getPriorityToVms().get(gVm.getPriority()).remove(vm);
//		double priorityCurrentUse = getPriorityToInUseMips().get(gVm.getPriority()); 
//		
//		getPriorityToInUseMips().put( gVm.getPriority(),
//				DecimalUtil.format(priorityCurrentUse - gVm.getMips(), DECIMAL_ACCURACY));
	}

//	public Map<Integer, Double> getPriorityToInUseMips() {
//		return priorityToInUseMips;
//	}
//
//	protected void setPriorityToInUseMips(
//			Map<Integer, Double> priorityToMipsInUse) {
//		this.priorityToInUseMips = priorityToMipsInUse;
//	}
//
//	public Map<Integer, SortedSet<Vm>> getPriorityToVms() {
//		return priorityToVms;
//	}
//
//	protected void setPriorityToVms(Map<Integer, SortedSet<Vm>> priorityToVms) {
//		this.priorityToVms = priorityToVms;
//	}
//
	public int getNumberOfPriorities() {
		return preemptionPolicy.getNumberOfPriorities();
	}
//	
//	protected void setNumberOfPriorities(int numberOfPriorities) {
//		this.numberOfPriorities = numberOfPriorities;
//	}
	
	/*
	 * TODO we need to refactor this code. we should not use cast here We also
	 * need to check where getTotalMips from Host class is being used because
	 * its return is int type
	 */
	public double getAvailableMipsByPriority(int priority) {
//		double inUseByNonPreemptiveVms = 0;
//
//		for (int i = 0; i <= priority; i++) {
//			inUseByNonPreemptiveVms += getPriorityToInUseMips().get(i);
//		}
//
//		return DecimalUtil.format(((VmSchedulerMipsBased) getVmScheduler()).getTotalMips()
//				- inUseByNonPreemptiveVms, DECIMAL_ACCURACY);
		return preemptionPolicy.getAvailableMipsByPriority(priority);
	}

	@Override
	public double getTotalMips(){
		return ((VmSchedulerMipsBased) getVmScheduler()).getTotalMips();
	}
	
	public List<UsageEntry> getUsageEntries() {
		List<UsageEntry> usageEntries = new LinkedList<UsageEntry>();
		for (UsageInfo usageInfo : getUsageMap().values()) {
			usageEntries.addAll(usageInfo.getUsageEntries());
//			for (int priority = 0; priority < getNumberOfPriorities(); priority++) {
//				usageEntries.add(new UsageEntry(getId(), usageInfo.getTime(),
//						usageInfo.getUsageByPriority(priority), usageInfo
//								.getNumberOfVmsByPriority(priority), priority, usageInfo.getAvailableMips()));
//			}
		}
		return usageEntries;
	}
	
	private void setUsageMap(Map<Double, UsageInfo> usageMap) {
		this.usageMap = usageMap;
	}
	
	protected Map<Double, UsageInfo> getUsageMap() {
		return usageMap;
	}
	
	public void updateUsage(double time) {
		getUsageMap().put( time,
				new UsageInfo(getId(), time, preemptionPolicy
						.getPriorityToInUseMips(), preemptionPolicy
						.getPriorityToVms(), getTotalUsage(),
						getAvailableMips()));
	}

	public void resetUsageMap() {
		getUsageMap().clear();
	}
	
//	public double getUsageByPriority(int priority) {
//		if (getPriorityToInUseMips().get(priority) != null) {
//			return getPriorityToInUseMips().get(priority);
//		}
//		return 0;
//	}
	public PreemptionPolicy getPreemptionPolicy() {
		return preemptionPolicy;
	}
	
	private void setPreemptionPolicy(PreemptionPolicy preemptionPolicy) {
		this.preemptionPolicy = preemptionPolicy;
	}
}