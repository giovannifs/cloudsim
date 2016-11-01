package org.cloudbus.cloudsim.preemption;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Vm;

public class PreemptableVm extends Vm implements Comparable<PreemptableVm> {

	public static final int NOT_EXECUTING_TIME = -1;
	private int priority;
	private double submitTime;
	private double runtime;
	private double startExec;
	private double actualRuntime;
	private int numberOfPreemptions;
	private int numberOfBackfillingChoice;
	private int hostId;

	public PreemptableVm(int id, int userId, double cpuReq, double memReq, double submitTime, int priority, double runtime) {
		super(id, userId, cpuReq, 1, (int) memReq, 0, 0, "default", new CloudletSchedulerTimeShared());

		setSubmitTime(submitTime);
		setPriority(priority);
		setRuntime(runtime);
		setStartExec(NOT_EXECUTING_TIME);
		setNumberOfPreemptions(0);
		setNumberOfBackfillingChoice(0);
		setHostId(-1);
		actualRuntime = 0;
	}

	@Override
	public int compareTo(PreemptableVm otherVm) {
		if (getPriority() < otherVm.getPriority()) {
			return -1;
		} else if (getPriority() > otherVm.getPriority()) {
			return 1;
		} else if (getSubmitTime() < otherVm.getSubmitTime()) {
			return -1;
		} else if (getSubmitTime() == otherVm.getSubmitTime()) {
			return new Integer(getId()).compareTo(new Integer(otherVm.getId()));
		}
		return 1;
	}
	
	public void preempt(double currentTime) {
		actualRuntime += (currentTime - getStartExec());		
		setStartExec(NOT_EXECUTING_TIME);
		setNumberOfPreemptions(getNumberOfPreemptions() + 1);
	}

	public double getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(double submitTime) {
		this.submitTime = submitTime;
	}
	
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public double getRuntime() {
		return runtime;
	}

	public void setRuntime(double runtime) {
		this.runtime = runtime;
	}

	public double getStartExec() {
		return startExec;
	}

	public void setStartExec(double startExec) {
		this.startExec = startExec;
	}
	
	public void setActualRuntime(double actualRuntime) {
		this.actualRuntime = actualRuntime;
	}

	public double getActualRuntime(double currentTime) {
		if (getStartExec() != NOT_EXECUTING_TIME) {
			return actualRuntime + (currentTime - getStartExec());
		}
		return actualRuntime;
	}

	public boolean achievedRuntime(double currentTime) {
		return getActualRuntime(currentTime) >= getRuntime();
	}

	public int getNumberOfPreemptions() {
		return numberOfPreemptions;
	}

	public void setNumberOfPreemptions(int numberOfPreemptions) {
		this.numberOfPreemptions = numberOfPreemptions;
	}

	public int getNumberOfBackfillingChoice() {
		return numberOfBackfillingChoice;
	}

	public void setNumberOfBackfillingChoice(int numberOfBackfillingchoice) {
		this.numberOfBackfillingChoice = numberOfBackfillingchoice;
	}

	public int getHostId(){
		return hostId;
	}

	public void setHostId(int hostId){
		this.hostId = hostId;
	}


}
