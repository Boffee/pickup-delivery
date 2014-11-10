package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class VehiclePlan {
	private Vehicle vehicle;
	private List<Task> tasks;
	private double cost;
	private int index;
	
	public int lastAdded;

	public VehiclePlan (Vehicle _vehicle, List<Task> _tasks, int _index) {
		vehicle = _vehicle;
		index = _index;
		cost = 0;
		if (_tasks == null) {
			tasks = new ArrayList<Task>();
		} else {
			tasks = _tasks;
			computeCost();
		}
	}

	public VehiclePlan (VehiclePlan vPlan) {
		vehicle = vPlan.vehicle;
		tasks = new ArrayList<Task>(vPlan.tasks);
		cost = vPlan.cost;
		index = vPlan.index;
	}

	/**
	 * swap the order of two tasks in the current plan
	 * @param tIdx1
	 * @param tIdx2
	 */
	public void swapTasksOrder(int tIdx1, int tIdx2) {
		Task task1 = tasks.get(tIdx1);
		Task task2 = tasks.get(tIdx2);
		// prevent index out of bound problems
		tasks.remove(tIdx1);
		tasks.add(tIdx1, task2);
		tasks.remove(tIdx2);
		tasks.add(tIdx2, task1);

		computeCost();
	}


	/**
	 * add a task to the vehicle job queue
	 * @param task to be added
	 * @return true if task is added (does not exceed vehicle capacity) false otherwise
	 */
	public boolean addTask(Task task) {
		if (task.weight > vehicle.capacity()) {
			return false;
		} else {
			tasks.add(task);
			computeCost();
			return true;
		}
	}

	public boolean addTask(int index, Task task) {
		if (task.weight > vehicle.capacity()) {
			return false;
		} else {
			tasks.add(index, task);
			computeCost();
			return true;
		}
	}

	/**
	 * remove the Task at the given index
	 * @param tIdx
	 * @return the removed Task
	 */
	public Task removeTask(int tIdx) {
		Task rmTask = tasks.remove(tIdx);
		computeCost();
		return rmTask;
	}


	/**
	 * find all task ordering out of all possible ordering
	 * @return the all ordering with the best ordering at the front
	 */
	public List<VehiclePlan> changeOrders() {
		List<VehiclePlan> planOrders = new ArrayList<VehiclePlan>();
		planOrders.add(this);
		for (int i = 0; i < tasks.size()-1; i++) {
			for (int j = i+1; j < tasks.size(); j++) {
				VehiclePlan newPlan = new VehiclePlan(this);
				newPlan.swapTasksOrder(i, j);

				VehiclePlan bestPlan = planOrders.get(0);
				if (newPlan.cost < bestPlan.cost) {
					planOrders.add(0, newPlan);
				} else if (newPlan.cost == bestPlan.cost) {
					if ((new Random().nextBoolean())) {
						planOrders.add(0, newPlan);
					}
				} else {
					planOrders.add(newPlan);
				}
			}
		}
		return planOrders;
	}

	public Plan getPlan() {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	public int size() {
		return tasks.size();
	}

	/**
	 * compute the cost of the plan
	 */
	private void computeCost() {
		cost = 0;
		City currCity = vehicle.getCurrentCity();
		for (Task task: tasks) {
			cost += currCity.distanceTo(task.pickupCity);
			cost += task.pathLength();

			currCity = task.deliveryCity;
		}
	}
	
	public Task getTask(int index) {
		return tasks.get(index);
	}

	public double getCost() {
		return cost;
	}

	public int getIndex() {
		return index;
	}
	
	public Vehicle getVehicle() {
		return vehicle;
	}

}
