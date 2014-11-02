package template;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class BFS {

	private List<StateNode> queue = new ArrayList<StateNode>();
	
	private Map<String, StateNode> cycle = new LinkedHashMap<String, StateNode>();
	
	private Plan mPlan = null;
	private List<Task> mIdleTasks = new ArrayList<Task>();
	private List<Task> mCarriedTasks = new ArrayList<Task>();

	private Vehicle mVehicle;


	public Plan getPlan() {
		if (mPlan == null) {
			bfsPlan();
		}
		return mPlan;
	}

	public List<Task> getTasks() {
		return mIdleTasks;
	}

	public Vehicle getVehicle() {
		return mVehicle;
	}

	public BFS (Vehicle vehicle, TaskSet idleTasks, TaskSet carriedTasks) {
		for (Task task: idleTasks) {
			mIdleTasks.add(task);
		}
		
		if (carriedTasks != null) {
			for (Task task: carriedTasks) {
				mCarriedTasks.add(task);
			}
		}
		
		mVehicle = vehicle;
	}

	private Plan bfsPlan() {

		City currCity = mVehicle.getCurrentCity();

		StateNode startNode = new StateNode(currCity, mIdleTasks, mCarriedTasks, null, 0);
		queue.add(startNode);

		StateNode finalNode = null;

		while (finalNode == null) {
			StateNode topNode = queue.remove(0);
			finalNode = bfs(topNode);
		}

		mPlan = findPlan(finalNode);
		return mPlan;
	}

	/**
	 * perform bfs on the current state add the new state to the search queue
	 * @param currState
	 * @return a reachable state from the current state
	 */
	private StateNode bfs(StateNode currState) {

		cycle.put(currState.key, currState);
		List<Task> idleTasks = currState.getIdleTasks();
		List<Task> carriedTasks = currState.getCarriedTasks();
		City currCity = currState.currCity;

		System.out.println(idleTasks.size() + " : " + carriedTasks.size());
		if (idleTasks.size() == 0 && carriedTasks.size() == 0) {
			System.out.println("best cost: " + currState.cost);
			return currState;
		}

		// find the tasks that are available for pickup at the current city
		List<Task> pickableTasks = new ArrayList<Task>();
		for (Task task : idleTasks) {
			if (currCity == task.pickupCity) {
				pickableTasks.add(task);
			}
		}


		List<List<Task>> possiblePickups = possiblePickups(pickableTasks, currState, mVehicle.capacity());
		List<City> possibleMoves = possibleMoves(currState);
		// store all possible actions into queue by looking through all possible pickups 
		// and move to a neigboring city on the shortest path of an undelivered task
		for (City neighbor : possibleMoves) {

			for (List<Task> pickups : possiblePickups) {

				List<Task> nIdleTasks = new ArrayList<Task>(idleTasks);
				List<Task> nCarriedTasks = new ArrayList<Task>(carriedTasks);

				for (Task pickup : pickups) {
					nIdleTasks.remove(pickup);
					nCarriedTasks.add(pickup);
				}

				for (int i = nCarriedTasks.size()-1; i >= 0; i--) {
					Task task = nCarriedTasks.get(i);
					if (task.deliveryCity == neighbor) {
						nCarriedTasks.remove(task);
					}
				}

				double cost = currState.cost + currCity.distanceTo(neighbor);
				StateNode nextState = new StateNode(neighbor, nIdleTasks, nCarriedTasks, currState, cost);
				// TODO test
				if (!stateExists(nextState)) {
					queue.add(nextState);
				}
			}
		}

		return null;
	}

	/**
	 * check for cycles
	 * @param node
	 * @return true or false
	 */
	private boolean stateExists(StateNode node) {
		if (cycle.containsKey(node.key)) {
			System.out.println("cycle found");
			return true;
		}
		return false;
	}
	
	/**
	 * @param pickableTasks
	 * @param currState
	 * @param capacity
	 * @return all possible ways of picking the tasks without exceeding the weight capacity
	 */
	private List<List<Task>> possiblePickups(List<Task> pickableTasks, StateNode currState, int capacity) {

		List<List<Task>> permutations = new ArrayList<List<Task>>();

		int currWeight = tasksWeight(currState.carriedTasks);

		// if the agent can pick up all of the remaining tasks combined, then it should just pickup
		// all available task at the current city.
		if (canTakeTasks(currState.idleTasks, currWeight, capacity)) {
			permutations.add(pickableTasks);
			return permutations;
		}

		// permutation for not take any task
		List<Task> empty = new ArrayList<Task>();
		permutations.add(empty);

		for (Task pickableTask : pickableTasks) {
			// make a copy of the current permutations and add one additional task
			for(int j = permutations.size()-1; j >= 0; j--) {
				List<Task> taskList = new ArrayList<Task>(permutations.get(j));
				taskList.add(pickableTask);

				if (canTakeTasks(taskList, currWeight, capacity)) {
					permutations.add(taskList);
				}
			}
		}

		return permutations;
	}

	private List<City> possibleMoves(StateNode currState) {

		List<City> possibleMoves = new ArrayList<City>();
		List<Task> idleTasks = currState.getIdleTasks();
		List<Task> carriedTasks = currState.getCarriedTasks();
		City currCity = currState.currCity;	

		for (Task task : idleTasks) {
			if (currCity != task.pickupCity) {
				City neighborOnShortestPath = currCity.pathTo(task.pickupCity).get(0);
				if (!possibleMoves.contains(neighborOnShortestPath)) {
					possibleMoves.add(neighborOnShortestPath);
				}
			}
		}

		for (Task task : carriedTasks) {
			if (currCity != task.deliveryCity) {
				City neighborOnShortestPath = currCity.pathTo(task.deliveryCity).get(0);
				if (!possibleMoves.contains(neighborOnShortestPath)) {
					possibleMoves.add(neighborOnShortestPath);
				}
			}
		}

		return possibleMoves;
	}

	/**
	 * determine if the task set can be picked up or not
	 * @param tasks
	 * @param currWeight
	 * @param capacity
	 * @return can or can not take task set
	 */
	private boolean canTakeTasks(List<Task> tasks, int currWeight, int capacity) {
		currWeight += tasksWeight(tasks);
		return (currWeight > capacity) ? false : true;
	}

	/**
	 * @param tasks
	 * @return total weight of a list of tasks
	 */
	private int tasksWeight(List<Task> tasks) {		
		int weight = 0;
		for (Task task: tasks) {
			weight += task.weight;
		}
		return weight;
	}

	private Plan findPlan(StateNode currState) {

		StateNode prevState = currState.parent;

		if (prevState == null) {
			return new Plan(currState.currCity);
		}

		List<Task> pickups = diff(prevState.getIdleTasks(), currState.getIdleTasks());

		List<Task> deliveries = diff(prevState.getCarriedTasks(), currState.getCarriedTasks());

		// add the tasks that were picked up and delivered on the next move
		for (Task task : pickups) {
			if (!currState.getCarriedTasks().contains(task)) {
				deliveries.add(task);
			}
		}

		City moveToCity = currState.currCity;

		Plan plan = findPlan(currState.parent);

		// pickup tasks at current city
		for (Task pickup : pickups) {
			plan.appendPickup(pickup);
		}

		// move to next city
		plan.appendMove(moveToCity);

		// deliver tasks at next city
		for (Task delivery : deliveries) {
			plan.appendDelivery(delivery);
		}

		return plan;
	}

	private List<Task> diff(List<Task> prev, List<Task> curr) {
		List<Task> diff = new ArrayList<Task>();
		for (Task task : prev) {
			if (!curr.contains(task)) {
				diff.add(task);
			}
		}
		return diff;
	}

}
