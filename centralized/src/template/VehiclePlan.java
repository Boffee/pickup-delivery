package template;

import java.util.ArrayList;
import java.util.List;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class VehiclePlan {
	private Vehicle vehicle;
	private List<Task> tasks;
	private double cost;

	public VehiclePlan (Vehicle _vehicle, List<Task> _tasks) {
		vehicle = _vehicle;
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
	}

	/**
	 * swap the order of two tasks in the current plan
	 * @param tIdx1
	 * @param tIdx2
	 */
	public void swapTasksOrder(int tIdx1, int tIdx2) {
		Task task1 = tasks.remove(tIdx1);
		Task task2 = tasks.remove(tIdx2);
		// prevent index out of bound problems
		if (tIdx1 < tIdx2) {
			tasks.add(tIdx1, task2);
			tasks.add(tIdx2, task1);
		} else {
			tasks.add(tIdx2, task1);
			tasks.add(tIdx1, task2);
		}
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
		Task rmTask = tasks.get(tIdx);
		computeCost();
		return rmTask;
	}


	/**
	 * find the best task ordering out of all possible ordering
	 * @return the best task ordering plan
	 */
	public VehiclePlan bestTaskOrder() {
		VehiclePlan bestPlan = this;
		for (int i = 0; i < tasks.size()-1; i++) {
			for (int j = i+1; j < tasks.size(); j++) {
				VehiclePlan newPlan = new VehiclePlan(vehicle, new ArrayList<Task>(tasks));
				newPlan.swapTasksOrder(i, j);
				if (newPlan.cost < bestPlan.cost) {
					bestPlan = newPlan;
				}
			}
		}
		return bestPlan;
	}

	public int size() {
		return tasks.size();
	}

	/**
	 * compute the cost of the plan
	 */
	private void computeCost() {
		City currCity = vehicle.getCurrentCity();
		for (Task task: tasks) {
			cost += currCity.distanceTo(task.pickupCity);
			cost += task.pathLength();

			currCity = task.deliveryCity;
		}
	}
	
	public double getCost() {
		return cost;
	}
}
