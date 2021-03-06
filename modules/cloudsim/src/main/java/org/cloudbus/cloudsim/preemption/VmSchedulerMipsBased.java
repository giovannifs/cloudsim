package org.cloudbus.cloudsim.preemption;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.preemption.util.DecimalUtil;

import java.util.List;

/**
 * Created by Alessandro Lia Fook Santos and João Victor Mafra on 01/09/16.
 */
public class VmSchedulerMipsBased extends VmScheduler{

    double totalMips;

    /**
     * Creates a new VmScheduler.
     *
     * @param pelist the list of PEs of the host where the VmScheduler is associated to.
     * @pre peList != $null
     * @post $none
     */
    public VmSchedulerMipsBased(List<? extends Pe> pelist) {
        super(pelist);
        double availableMips = 0;
        for (Pe pe : pelist) {
			availableMips += pe.getPeProvisioner().getMips();
		}
        setAvailableMips(availableMips);
        setTotalMips(getAvailableMips());
    }

    @Override
    public boolean allocatePesForVm(Vm vm, List<Double> mipsShare) {

        double totalRequestedMips = 0;
        double peMips = getPeCapacity();
        //calculate total mips requested by vm
        for (Double mips : mipsShare) {
            // each virtual PE of a VM must require not more than the capacity of a physical PE
            if (mips > peMips) {
                return false;
            }
            totalRequestedMips += mips;
        }

        //test if the host support the vm
        if (totalRequestedMips <= getAvailableMips()) {

            getMipsMap().put(vm.getUid(), mipsShare);
            double newAvailableMips = getAvailableMips() - totalRequestedMips;
            setAvailableMips(newAvailableMips);

            return true;
        }

        else {
            return false;
        }
    }

    
    @Override
	public double getPeCapacity() {
		if (getPeList() == null) {
			Log.printLine("Pe list is empty");
			return 0;
		}
		return getPeList().get(0).getPeProvisioner().getMips();
	}

    @Override
    public void deallocatePesForVm(Vm vm) {

        if(getMipsMap().containsKey(vm.getUid())){

            List<Double> mipsReleased = getMipsMap().get(vm.getUid());
            double totalReleasedMips = 0;

            //calculate total mips of the vm
            for (Double mips : mipsReleased) {
                totalReleasedMips += mips;

            }

            getMipsMap().remove(vm.getUid());
            setAvailableMips(getAvailableMips() + totalReleasedMips);
        }
    }

    public double getMipsInUse(){ return getTotalMips() - getAvailableMips(); }

    public double getTotalMips() { return totalMips; }

    public void setTotalMips(double totalMips) { this.totalMips = DecimalUtil.format(totalMips); }

    @Override
    protected void setAvailableMips(double availableMips) {
        super.setAvailableMips(DecimalUtil.format(availableMips));
    }

    @Override
    public double getMaxAvailableMips() {
        return getAvailableMips();
    }

}
