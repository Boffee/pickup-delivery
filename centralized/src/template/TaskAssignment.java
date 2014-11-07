package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class TaskAssignment {

	private List<VehiclePlan> plans = new ArrayList<VehiclePlan>();
	private List<Task> tasks = new ArrayList<Task>();

	private Random rand = new Random();

	private final static double prob = 0.4;

	public TaskAssignment(List<Vehicle> _vehicles, TaskSet _tasks) {
		Vehicle largestVehicle = _vehicles.get(0);
		Integer largestVehicleIndex = 0;
		for (int i = 0; i < _vehicles.size(); i++) {
			Vehicle vehicle = _vehicles.get(i);
			plans.add(new VehiclePlan(vehicle, null, i));
			if (vehicle.capacity() > largestVehicle.capacity()) {
				largestVehicle = vehicle;
				largestVehicleIndex = i;
			}
		}
		
		for (Task task : _tasks) {
			tasks.add(task);
			plans.get(largestVehicleIndex).addTask(task);
		}
	}

	public TaskAssignment(TaskAssignment ta) {
		plans = new ArrayList<VehiclePlan>(ta.getPlans());
		tasks = ta.getTasks();
	}

	private boolean chooseNeighbor() {

		VehiclePlan plan1;
		// pick a random non-empty plan as plan1
		do {
			int pIndex = rand.nextInt(plans.size());
			plan1 = plans.get(pIndex);
		} while (plan1.size() == 0);
		
		for (VehiclePlan plan: plans) {
				System.out.print(plan.size() + ", ");
		}
		System.out.println();

		// pick a random task in plan1 and remove it
		VehiclePlan newPlan1 = new VehiclePlan(plan1);
		int tIndex = rand.nextInt(newPlan1.size());		
		Task removedTask = newPlan1.removeTask(tIndex);
		double baseCostChange = newPlan1.getCost() - plan1.getCost();
	
		List<VehiclePlan> vehicleChanges = changeVehicles(plan1, removedTask);
		List<VehiclePlan> orderChanges = plan1.changeOrders();

		if (rand.nextDouble() > prob) {
			if (rand.nextDouble() > .5) {
				int index = rand.nextInt(orderChanges.size());
				replaceWith(orderChanges.get(index));
			} else {
				int index = rand.nextInt(vehicleChanges.size());
				replaceWith(vehicleChanges.get(index));
				replaceWith(newPlan1);
			}

		} else {
			VehiclePlan bestPlan1 = vehicleChanges.get(0);
			int p1Index = bestPlan1.getIndex();
			double costChange1 = bestPlan1.getCost()-plans.get(p1Index).getCost()+baseCostChange;

			VehiclePlan bestPlan2 = orderChanges.get(0);
			int p2Index = bestPlan2.getIndex();
			double costChange2 = bestPlan2.getCost()-plans.get(p2Index).getCost();

			// termination condition
			if (costChange1 >= 0 && costChange2 >= 0) {
				return true;
			}

			if (costChange1 < costChange2) {
				replaceWith(bestPlan1);
				replaceWith(newPlan1);
			} else if (costChange1 == costChange2) {
				if(rand.nextBoolean()) {
					replaceWith(bestPlan1);
					replaceWith(newPlan1);
				}
				else replaceWith(bestPlan2);
			}
			else {
				replaceWith(bestPlan2);
			}
		}
		
		return false;
	}
	
	private void replaceWith(VehiclePlan replacementPlan) {
		int planIndex = replacementPlan.getIndex();
		plans.remove(planIndex);
		plans.add(planIndex, replacementPlan);
	}


	/**
	 * @return 
	 */
	private List<VehiclePlan> changeVehicles(VehiclePlan plan1, Task removedTask) {

		List<VehiclePlan> vehicleChanges = new ArrayList<VehiclePlan>();

		Double bestCostChange = null;

		for (int i = 0; i < plans.size(); i++) {
			VehiclePlan plan2 = plans.get(i);
			if (plan2.getIndex() != plan1.getIndex()) {

				VehiclePlan newPlan2 = new VehiclePlan(plan2);
				int tIndex = rand.nextInt(newPlan2.size()+1);

				if (newPlan2.addTask(tIndex, removedTask)) {
					double origCost = plan2.getCost();
					double newCost = newPlan2.getCost();
					double costChange = newCost-origCost;

					if (bestCostChange == null) {
						bestCostChange = costChange;
					} else if ( costChange < bestCostChange) {
						bestCostChange = costChange;
						vehicleChanges.add(0, newPlan2);
					} else if ( costChange == bestCostChange) {
						if(rand.nextBoolean()) {
							vehicleChanges.add(0, newPlan2);
						}
					} else {
						vehicleChanges.add(newPlan2);
					}
				}
			}
		}

		return vehicleChanges;
	}

	public List<VehiclePlan> getPlans() {
		boolean stop = false;
		while (!stop) {
			stop = chooseNeighbor();
		}
		return plans;
	}

	public List<Task> getTasks() {
		return tasks;
	}


}
