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
//		int tIndex = rand.nextInt(newPlan1.size());		
//		Task removedTask = newPlan1.removeTask(tIndex);
//		double baseCostChange = newPlan1.getCost() - plan1.getCost();

		List<VehiclePlan> vehicleChanges = changeVehicles(plan1);
		List<VehiclePlan> orderChanges = plan1.changeOrders();

		if (rand.nextDouble() > prob) {
			if (rand.nextDouble() > .5) {
				int index = rand.nextInt(orderChanges.size());
				planChange(orderChanges.get(index));
			} else {
				int index = rand.nextInt(vehicleChanges.size());
				int remove = vehicleChanges.get(index).lastAdded;
				newPlan1.removeTask(remove);
				planChange(newPlan1, vehicleChanges.get(index));
				//				replaceWith(vehicleChanges.get(index));
				//				replaceWith(newPlan1);
			}

		} else {
			VehiclePlan bestVehicleChange = vehicleChanges.get(0);
			int p1Index = bestVehicleChange.getIndex();
			int remove = bestVehicleChange.lastAdded;
			newPlan1.removeTask(remove);
			double costChangeV = bestVehicleChange.getCost()-plans.get(p1Index).getCost()
					+newPlan1.getCost()-plan1.getCost();


			VehiclePlan bestOrderChange = orderChanges.get(0);
			int p2Index = bestOrderChange.getIndex();
			double costChangeO = bestOrderChange.getCost()-plans.get(p2Index).getCost();

			// termination condition
			if (costChangeV >= 0 && costChangeO >= 0) {
				return true;
			}

			if (costChangeV < costChangeO) {
				planChange(newPlan1, bestVehicleChange);
				//				replaceWith(bestPlan2a);
				//				replaceWith(newPlan1);
			} else if (costChangeV == costChangeO) {
				if(rand.nextBoolean()) {
					planChange(newPlan1, bestVehicleChange);
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
	
	private void planChange(VehiclePlan replacementPlan1, VehiclePlan replacementPlan2) {
		plans.remove(replacementPlan2.getIndex());
		plans.add(replacementPlan2.getIndex(), replacementPlan2);
		
		plans.remove(replacementPlan1.getIndex());
		plans.add(replacementPlan1.getIndex(), replacementPlan1);
	}


	/**
	 * @return 
	 */
	private List<VehiclePlan> changeVehicles(VehiclePlan plan1) {

		List<VehiclePlan> vehicleChanges = new ArrayList<VehiclePlan>();

		Double bestCostImprove = null;

		for (int j = 0; j < plan1.size(); j++) {
			Task removedTask = plan1.getTask(j);
			for (int i = 0; i < plans.size(); i++) {
				VehiclePlan plan2 = plans.get(i);
				plan2.lastAdded = j;
				if (plan2.getIndex() != plan1.getIndex()) {

					VehiclePlan newPlan2 = new VehiclePlan(plan2);
					int tIndex = rand.nextInt(newPlan2.size()+1);

					if (newPlan2.addTask(tIndex, removedTask)) {
						double origCost = plan2.getCost();
						double newCost = newPlan2.getCost();
						double costImporve = newCost-origCost;

						if (bestCostImprove == null) {
							bestCostImprove = costImporve;
						} else if ( costImporve < bestCostImprove) {
							bestCostImprove = costImporve;
							vehicleChanges.add(0, newPlan2);
						} else if ( costImporve == bestCostImprove) {
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
