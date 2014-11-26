package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class VehiclePlan {
	private Vehicle vehicle;
	private List<Task> tasks;
	private double cost;
	private int index;
	private Plan plan;
	
	private int heaviestTask;
	
	public int lastAdded;

	/**
	 * constructor
	 * @param _vehicle
	 * @param _tasks
	 * @param _index
	 */
	public VehiclePlan (Vehicle _vehicle, List<Task> _tasks, int _index) {
		vehicle = _vehicle;
		index = _index;
		cost = 0;
		if (_tasks == null) {
			tasks = new ArrayList<Task>();
		} else {
			tasks = _tasks;
		}
	}

	/**
	 * copy constructor
	 * @param vPlan
	 */
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
			return true;
		}
	}

	/**
	 * add a task to the plan at the specified index
	 * @param index
	 * @param task
	 * @return add success/fail
	 */
	public boolean addTask(int index, Task task) {
		if (task.weight > vehicle.capacity()) {
			return false;
		} else {
			tasks.add(index, task);
			return true;
		}
	}
	
	/**
	 * sum the weight of tasks in a list
	 * @param tasks
	 * @return total weight
	 */
	public int sumWeight(List<Task> tasks) {

		int weight = 0;
		for (Task task : tasks) {
			weight += task.weight;
		}
			return weight;
	}

	/**
	 * remove the Task at the given index
	 * @param tIdx
	 * @return the removed Task
	 */
	public Task removeTask(int tIdx) {
		Task rmTask = tasks.remove(tIdx);
		return rmTask;
	}


	/**
	 * find all task ordering out of all possible ordering
	 * @return the all ordering with the best ordering at the front
	 */
	public List<VehiclePlan> changeOrders() {
		computeCost();
		List<VehiclePlan> planOrders = new ArrayList<VehiclePlan>();
		planOrders.add(this);
		for (int i = 0; i < tasks.size()-1; i++) {
			for (int j = i+1; j < tasks.size(); j++) {
				VehiclePlan newPlan = new VehiclePlan(this);
				newPlan.swapTasksOrder(i, j);
				newPlan.computeCost();
				
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

	/**
	 * get the actions plan
	 * @return actions plan
	 */
	public Plan getPlan() {
		System.out.println(tasks);
		System.out.println(plan);
		return plan;
	}

	/**
	 * get size of plan
	 * @return size of plan
	 */
	public int size() {
		return tasks.size();
	}

	/**
	 * compute the cost of the plan.
	 * First pickup the tasks at the starting city
	 * Then move to the first task pickup city and pickup/deliver all available tasks on the way there
	 * Then move to the delivery city and pickup/deliver all available tasks on path
	 * repeat last 2 steps until every task is delivered
	 */
	private void computeCost() {
		cost = 0;
		
		List<Task> carriedTasks = new ArrayList<Task>();
		List<Task> idleTasks = new ArrayList<Task>(tasks);
		heaviestTask = heaviestTaskWeight(tasks);
		
		City currCity = vehicle.getCurrentCity();
		plan = new Plan(currCity);
		
		pickupTasks(currCity, carriedTasks, idleTasks, heaviestTask);
		
		for (Task targetTask: tasks) {
			// if targetTask is not picked up yet
			boolean pickedup = false;
			if (idleTasks.contains(targetTask)) {
				// move through all cities on path to pickup
				List<City> cities = currCity.pathTo(targetTask.pickupCity);
				for (City city : cities) {
					plan.appendMove(city);
					cost += currCity.distanceTo(city) * vehicle.costPerKm();
					currCity = city;

					deliverTasks(currCity, carriedTasks, idleTasks);
					if (city == targetTask.pickupCity) {
						carriedTasks.add(targetTask);
						idleTasks.remove(targetTask);
						pickedup = true;
						plan.appendPickup(targetTask);
					}
					if (pickedup = true) {
						pickupTasks(currCity, carriedTasks, idleTasks, heaviestTask);
					} else {
						pickupTasks(currCity, carriedTasks, idleTasks, 2*heaviestTask);
					}
				}
			}
			
			// if targetTask is picked up and not delivered
			if (carriedTasks.contains(targetTask)) {
				// move through all cities on path to delivery
				List<City> cities = currCity.pathTo(targetTask.deliveryCity);
				for (City city : cities) {
					plan.appendMove(city);
					cost += currCity.distanceTo(city) * vehicle.costPerKm();
					currCity = city;
					
					deliverTasks(currCity, carriedTasks, idleTasks);
					pickupTasks(currCity, carriedTasks, idleTasks, heaviestTask);
				}
			}
		}
		
		// debug
		if (carriedTasks.size() != 0 && idleTasks.size() != 0) {
			System.out.println(carriedTasks.size() + " : " + idleTasks.size());
		}
	}
	
	private int heaviestTaskWeight(List<Task> tasks) {
		int heaviest = 0; 
		for (Task task: tasks) {
			if (task.weight > heaviest) {
				heaviest = task.weight;
			}
		}
		return heaviest;
	}
	
	/**
	 * picks up all tasks available at the current city without exceeding the capacity and add the pickup actions to plan
	 * 
	 * The commented out region is an alternative search method (comment out the current method and uncomment the alternative):
	 * It only adds the task available at the current city if it is the next sequential task.
	 * @param currCity
	 * @param carriedTasks
	 * @param idleTasks
	 * @param allowableWeight
	 */
	private void pickupTasks(City currCity, List<Task> carriedTasks, List<Task> idleTasks, int allowableWeight) {
//		List<Task> removedTasks = new ArrayList<Task>();
//		// pickup tasks that are on the path to the current task and are within the capacity
//		for (int i = 0; i < idleTasks.size(); i++) {
//			Task idleTask = idleTasks.get(i);
//			if (idleTask.pickupCity == currCity) {
//				int totalWeight = sumWeight(carriedTasks) + allowableWeight;
//				if (totalWeight <= vehicle.capacity()) {
//					carriedTasks.add(idleTask);
//					removedTasks.add(idleTask);
//					
//					plan.appendPickup(idleTask);
//				}
//			}
//		}
//		removeFromList(removedTasks, idleTasks);
		if (idleTasks.size() == 0) return;
		Task nextTask = idleTasks.get(0);
		if(currCity == nextTask.pickupCity) {
			if (sumWeight(carriedTasks) + allowableWeight <= vehicle.capacity()) {
				carriedTasks.add(nextTask);
				idleTasks.remove(nextTask);
				
				plan.appendPickup(nextTask);
				
				pickupTasks(currCity, carriedTasks, idleTasks, allowableWeight);
			}
		}
	}
	
	/**
	 * delivers the carried tasks carried by the vehicle if the current city is the corresponding
	 * delivery city.
	 * @param currCity
	 * @param carriedTasks
	 * @param idleTasks
	 */
	private void deliverTasks(City currCity, List<Task> carriedTasks, List<Task> idleTasks) {
		// deliver tasks that are on the current path
		for (int i = carriedTasks.size()-1; i >= 0; i--) {
			Task carriedTask = carriedTasks.get(i);
			if (carriedTask.deliveryCity == currCity) {
				carriedTasks.remove(carriedTask);
				
				plan.appendDelivery(carriedTask);
			}
		}
	}
		
	/**
	 * remove a list of tasks from a lost of tasks
	 * Note: the removeAll method of List doesn't work sometimes
	 * @param removes
	 * @param orig
	 */
	private void removeFromList(List<Task> removes, List<Task> orig) {
		for (Task remove: removes) {
			orig.remove(remove);
		}
	}
	
	
	/**
	 * get the task at the given index
	 * @param index
	 * @return task at the index
	 */
	public Task getTask(int index) {
		return tasks.get(index);
	}

	/**
	 * computes the cost of the plan
	 * @return plan cost
	 */
	public double getCost() {
		computeCost(); 
		return cost;
	}

	/**
	 * get the index of this VehiclePlan inside the VehiclePlan List in TaskAssignment
	 * @return index of this plan
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * get the vehicle of this plan
	 * @return vehicle
	 */
	public Vehicle getVehicle() {
		return vehicle;
	}

}
