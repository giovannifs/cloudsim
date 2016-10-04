package org.cloudbus.cloudsim.googletrace;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.cloudbus.cloudsim.Vm;

public class UsageEntry {

	private int hostId;
	private double time;
	private double totalUsage;
	private double availableMips;
	private Map<Integer, Double> priorityToUsage;
	private Map<Integer, Integer> priorityToNumberOfVms;
	
	public UsageEntry(int hostId, double time, Map<Integer, Double> priorityToInUseMips,
			Map<Integer, SortedSet<Vm>> priorityToVms, double totalUsage, double availableMips) {
		
		this.hostId = hostId;
		this.time = time;
		this.totalUsage = totalUsage;
		this.availableMips = availableMips;
		this.priorityToUsage =  new HashMap<Integer, Double>(priorityToInUseMips);
		this.priorityToNumberOfVms = new HashMap<Integer, Integer>();
		
		for (Integer priority : priorityToVms.keySet()) {
			priorityToNumberOfVms.put(priority, priorityToVms.get(priority).size());
		}
	}

	public UsageEntry(int hostId, double time, double p0Usage, double p1Usage,
					  double p2Usage, int p0Vms, int p1Vms, int p2Vms, double availableMips) {
		
		this.hostId = hostId;
		this.time = time;
		this.totalUsage = p0Usage + p1Usage + p2Usage;
		this.availableMips = availableMips;
		this.priorityToUsage =  new HashMap<Integer, Double>();
		priorityToUsage.put(0, p0Usage);
		priorityToUsage.put(1, p1Usage);
		priorityToUsage.put(2, p2Usage);
		
		this.priorityToNumberOfVms = new HashMap<Integer, Integer>();
		priorityToNumberOfVms.put(0, p0Vms);
		priorityToNumberOfVms.put(1, p1Vms);
		priorityToNumberOfVms.put(2, p2Vms);
	}

	public int getHostId() {
		return hostId;
	}

	public double getTime() {
		return time;
	}

	public double getTotalUsage() {
		return totalUsage;
	}

	public double getAvailableMips() {
		return availableMips;
	}

	public double getUsageByPriority(int priority) {
		return priorityToUsage.get(priority);
	}

	public int getNumberOfVmsByPriority(int priority) {
		return priorityToNumberOfVms.get(priority);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UsageEntry that = (UsageEntry) o;

		if (hostId != that.hostId) return false;
		if (Double.compare(that.time, time) != 0) return false;
		if (Double.compare(that.totalUsage, totalUsage) != 0) return false;
		if (Double.compare(that.availableMips, availableMips) != 0) return false;
		if (!priorityToUsage.equals(that.priorityToUsage)) return false;
		return priorityToNumberOfVms.equals(that.priorityToNumberOfVms);

	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = hostId;
		temp = Double.doubleToLongBits(time);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(totalUsage);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(availableMips);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + priorityToUsage.hashCode();
		result = 31 * result + priorityToNumberOfVms.hashCode();
		return result;
	}
	
}
