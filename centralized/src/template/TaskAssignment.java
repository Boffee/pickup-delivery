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
	private List<VehiclePlan> bestPlans;

	private Random rand = new Random();

	private final static double prob = 0.5;


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

		double origPlan1Cost = plan1.getCost();


//		for (VehiclePlan plan: plans) {
//			System.out.print(plan.size() + ", ");
//		}
//		System.out.println();

		List<VehiclePlan> vehicleChanges = changeVehicles(plan1);
		List<VehiclePlan> orderChanges = plan1.changeOrders();

		if (rand.nextDouble() < prob) {
			int index = rand.nextInt(vehicleChanges.size());
			if (index < vehicleChanges.size()) {
				int remove = vehicleChanges.get(index).lastAdded;
				plan1.removeTask(remove);
				planChange(vehicleChanges.get(index));
			} else {
				index -= vehicleChanges.size();
				planChange(orderChanges.get(index));
			}
		} else {

			VehiclePlan bestOrderChange = orderChanges.get(0);
			int p2Index = bestOrderChange.getIndex();
			double costChangeO = bestOrderChange.getCost()-plans.get(p2Index).getCost();

			if (vehicleChanges.size() <= 0) {
				planChange(bestOrderChange);
				if (costChangeO >= 0) return true;
				else return false;
			}

			VehiclePlan bestVehicleChange = vehicleChanges.get(0);
			int p1Index = bestVehicleChange.getIndex();
			int remove = bestVehicleChange.lastAdded;
			Task removedTask = plan1.removeTask(remove);
			double costChangeV = bestVehicleChange.getCost()-plans.get(p1Index).getCost()
					+plan1.getCost()-origPlan1Cost;

			//			System.out.println("vehicle change: " + costChangeV + " order change: " + costChangeO);
			// termination condition
			if (costChangeV >= 0 && costChangeO >= 0) {
				plan1.addTask(remove, removedTask);
				return true;
			}

			if (costChangeV < costChangeO) {
				planChange(bestVehicleChange);
				//				replaceWith(bestPlan2a);
				//				replaceWith(newPlan1);
			} else if (costChangeV == costChangeO) {
				if(rand.nextBoolean()) {
					planChange(bestVehicleChange);
					//					replaceWith(bestPlan2a);
					//					replaceWith(newPlan1);
				}
				else {
					planChange(bestOrderChange); 
					//replaceWith(bestPlan2b);
				}
			}
			else {
				planChange(bestOrderChange);
				//replaceWith(bestPlan2b);
			}
		}

		return false;
	}

	private void planChange(VehiclePlan replacementPlan) {
		plans.remove(replacementPlan.getIndex());
		plans.add(replacementPlan.getIndex(), replacementPlan);
	}


	/**
	 * @return 
	 */
	private List<VehiclePlan> changeVehicles(VehiclePlan plan1) {

		List<VehiclePlan> vehicleChanges = new ArrayList<VehiclePlan>();

		Double bestCostImprove = null;

		for (int j = 0; j < plan1.size(); j++) {
			VehiclePlan newPlan1 = new VehiclePlan(plan1);
			Task removedTask = newPlan1.removeTask(j);

			double baseCostChange = newPlan1.getCost() - plan1.getCost();

			for (int i = 0; i < plans.size(); i++) {
				VehiclePlan plan2 = plans.get(i);
				if (plan2.getIndex() != plan1.getIndex()) {

					VehiclePlan newPlan2 = new VehiclePlan(plan2);
					newPlan2.lastAdded = j;
					int tIndex = rand.nextInt(newPlan2.size()+1);

					if (newPlan2.addTask(tIndex, removedTask)) {
						double origCost = plan2.getCost();
						double newCost = newPlan2.getCost();
						double costImprove = baseCostChange + newCost-origCost;

						if (bestCostImprove == null) {
							bestCostImprove = costImprove;
							vehicleChanges.add(newPlan2);
						} else if ( costImprove < bestCostImprove) {
							bestCostImprove = costImprove;
							vehicleChanges.add(0, newPlan2);
						} else if ( costImprove == bestCostImprove) {
							if(rand.nextBoolean()) {
								vehicleChanges.add(0, newPlan2);
							}
						} else {
							vehicleChanges.add(newPlan2);
						}
					}
				}
			}
		}
		return vehicleChanges;
	}


	public List<VehiclePlan> getPlans() {
		bestPlans = copyPlans(plans);
		boolean stop = false;
		int stops = 0;
		while (stops < 10000) {
			stop = chooseNeighbor();
			stops++;
			if (totalCost(plans) < totalCost(bestPlans)) {
				bestPlans = copyPlans(plans);
				System.out.println(totalCost(bestPlans));
			}
		}
		return bestPlans;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	private double totalCost(List<VehiclePlan> vPlans) {
		double cost = 0;
		for (VehiclePlan plan : vPlans) {
			cost += plan.getCost();
		}
		return cost;
	}

	public List<VehiclePlan> copyPlans(List<VehiclePlan> vPlans) {
		List<VehiclePlan> copies = new ArrayList<VehiclePlan>();
		for (VehiclePlan plan : vPlans) {
			VehiclePlan planCopy = new VehiclePlan(plan);
			copies.add(planCopy);
		}
		return copies;
	}

}
