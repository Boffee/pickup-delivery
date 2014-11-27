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

	private double prob = 0.5;
	int stuck = 0;


	/**
	 * constructor
	 * @param _vehicles
	 * @param _tasks
	 */
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

	/**
	 * copy constructor
	 * @param ta
	 */
	public TaskAssignment(TaskAssignment ta) {
		plans = new ArrayList<VehiclePlan>(ta.getPlans());
		tasks = ta.getTasks();
	}

	/**
	 * replace the current TaskAssignment with a neighbor
	 * choose 1 random non-empty VehiclePlan and find its neighbors:
	 * - all possible orderings
	 * - all possible single task reassignment to another vehicle
	 * either replace the current TaskAssignment with the best neighbor or a random neighbor with a specified probability
	 * @return could find a cheaper neighbor
	 */
	private boolean chooseNeighbor() {

		VehiclePlan plan1;

		// pick a random non-empty plan as plan1
		do {
			int pIndex = rand.nextInt(plans.size());
			plan1 = plans.get(pIndex);
		} while (plan1.size() == 0);

		double origPlan1Cost = plan1.getCost();
		// probability is proportional to the size of the plan being changed
		updateProb();

		//		for (VehiclePlan plan: plans) {
		//			System.out.print(plan.size() + ", ");
		//		}
		//		System.out.println();

		List<VehiclePlan> vehicleChanges = changeVehicles(plan1);
		List<VehiclePlan> orderChanges = plan1.changeOrders();

		// if less than prob, choose a random vehicle reassignment neighbor, else choose the best neighbor
		if (rand.nextDouble() < prob) {
			int index = rand.nextInt(vehicleChanges.size());
			int remove = vehicleChanges.get(index).lastAdded;
			plan1.removeTask(remove);
			planChange(vehicleChanges.get(index));

		} else {

			VehiclePlan bestOrderChange = orderChanges.get(0);
			int p2Index = bestOrderChange.getIndex();
			double costChangeO = bestOrderChange.getCost()-plans.get(p2Index).getCost();

			// vehicle reassignments could be empty. If it is, use the best order change plan
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

			// termination condition, if both costChange are >= 0, no improvements can be made in this iteration
			if (costChangeV >= 0 && costChangeO >= 0 && rand.nextDouble() > prob) {
				plan1.addTask(remove, removedTask);
				return true;
			}
			// get the better of the either order change or vehicle change
			if (costChangeV < costChangeO) {
				planChange(bestVehicleChange);
			} else if (costChangeV == costChangeO) {
				if(rand.nextBoolean()) {
					planChange(bestVehicleChange);
				}
				else {
					planChange(bestOrderChange); 
				}
			}
			else {
				planChange(bestOrderChange);
			}
		}
		return false;
	}

	/**
	 * replaces the corresponding plan in the current TaskAssignment with the replacementPlan
	 * @param replacementPlan
	 */
	private void planChange(VehiclePlan replacementPlan) {
		plans.remove(replacementPlan.getIndex());
		plans.add(replacementPlan.getIndex(), replacementPlan);
	}


	/**
	 * for every task inside plan1, try reassigning them to all over vehicles
	 * @return a list of all reassigned plan2's
	 */
	private List<VehiclePlan> changeVehicles(VehiclePlan plan1) {

		List<VehiclePlan> vehicleChanges = new ArrayList<VehiclePlan>();

		Double bestCostImprove = null;

		for (int j = 0; j < plan1.size(); j++) {
			VehiclePlan newPlan1 = new VehiclePlan(plan1);
			Task removedTask = newPlan1.removeTask(j);
			// cost decrease from removing a task from plan1
			double baseCostChange = newPlan1.getCost() - plan1.getCost();

			for (int i = 0; i < plans.size(); i++) {
				VehiclePlan plan2 = plans.get(i);
				if (plan2.getIndex() != plan1.getIndex()) {

					VehiclePlan newPlan2 = new VehiclePlan(plan2);
					// keeps track of the index of the task added into plan2 because plan1
					// is not changed until the best plan2 is found
					newPlan2.lastAdded = j;
					int tIndex = rand.nextInt(newPlan2.size()+1);
					// addTask return false if the task is overweight and could not be added
					if (newPlan2.addTask(tIndex, removedTask)) {
						double origCost = plan2.getCost();
						double newCost = newPlan2.getCost();
						double costImprove = baseCostChange + newCost-origCost;
						
						// keep track of the best plan2 by putting it at the front of the returned list of plans
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


	/**
	 * finds the best TaskAssignment and returns it as a list of plans
	 * @return list of plans
	 */
	public List<VehiclePlan> getPlans() {
		bestPlans = copyPlans(plans);
		boolean stop = false;
		int iterations = 0;
		while (iterations < 10000) {
			stop = chooseNeighbor();
			// keep track of how stuck the current TaskAssignment is
			if (stop) {
				stuck++;
			} else {
				stuck /= 2;
			}
			iterations++;
			// keeps track of the best TaskAssignment
			if (totalCost(plans) < totalCost(bestPlans)) {
				bestPlans = copyPlans(plans);
				System.out.println(totalCost(bestPlans));
			}
		}
		return bestPlans;
	}

	/**
	 * get the list of all available tasks
	 * @return all tasks
	 */
	public List<Task> getTasks() {
		return tasks;
	}

	/**
	 * get the total cost of the TaskAssignment (all VehiclePlans)
	 * @param vPlans
	 * @return total cost
	 */
	private double totalCost(List<VehiclePlan> vPlans) {
		double cost = 0;
		for (VehiclePlan plan : vPlans) {
			cost += plan.getCost();
		}
		return cost;
	}

	/**
	 * update the probability based on how stuck the TaskAssignment is in a local minima
	 * (I don't think this does much)
	 */
	private void updateProb() {
		prob = 0.2/Math.pow(0.9, stuck);
		prob = (prob > 0.8) ? 0.8 : prob;
	}


	/**
	 * copies a list of plans
	 * @param vPlans
	 * @return a hard copy of vPlans
	 */
	public List<VehiclePlan> copyPlans(List<VehiclePlan> vPlans) {
		List<VehiclePlan> copies = new ArrayList<VehiclePlan>();
		for (VehiclePlan plan : vPlans) {
			VehiclePlan planCopy = new VehiclePlan(plan);
			copies.add(planCopy);
		}
		return copies;
	}

}
