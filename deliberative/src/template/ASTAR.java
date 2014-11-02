package template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ASTAR {

	private List<StateNode> queue = new ArrayList<StateNode>();

	private Map<String, StateNode> cycle = new LinkedHashMap<String, StateNode>();

	private Plan mPlan = null;
	private List<Task> mIdleTasks = new ArrayList<Task>();
	private List<Task> mCarriedTasks = new ArrayList<Task>();

	private Vehicle mVehicle;
	private double mShortestEdge = 0;
	
	StateNode bestNode = null;


	public Plan getPlan() {
		if (mPlan == null) {
			aStarPlan();
		}
		return mPlan;
	}

	public List<Task> getTasks() {
		return mIdleTasks;
	}

	public Vehicle getVehicle() {
		return mVehicle;
	}

	public ASTAR (Vehicle vehicle, TaskSet idleTasks, TaskSet carriedTasks, double shortestEdge) {
		for (Task task: idleTasks) {
			mIdleTasks.add(task);
		}
		if (carriedTasks != null) {
			for (Task task : carriedTasks) {
				mCarriedTasks.add(task);
			}
		}
		mVehicle = vehicle;
		mShortestEdge = shortestEdge;
	}

	private Plan aStarPlan() {

		City currCity = mVehicle.getCurrentCity();

		StateNode startNode = new StateNode(currCity, mIdleTasks, mCarriedTasks, null, 0);
		queue.add(startNode);


		while (queue.size() != 0) {
			StateNode topNode = queue.remove(0);
			aStar(topNode);
			Collections.sort(queue, new Comparator<StateNode>() {
				public int compare(StateNode node1, StateNode node2) {
					Double cost1 = new Double(node1.cost+node1.heuristic);
					Double cost2 = new Double(node2.cost+node2.heuristic);
					return cost1.compareTo(cost2);
				}
			});
		}

		mPlan = findPlan(bestNode);
		return mPlan;
	}

	/**
	 * perform aStar on the current state add the new state to the search queue
	 * @param currState
	 * @return a reachable state from the current state
	 */
	private void aStar(StateNode currState) {
		
		if (bestNode != null) {
			if (currState.cost+currState.heuristic >= bestNode.cost) {
				return;
			}
		}

		cycle.put(currState.key, currState);
		List<Task> idleTasks = currState.getIdleTasks();
		List<Task> carriedTasks = currState.getCarriedTasks();
		City currCity = currState.currCity;

//		System.out.println(idleTasks.size() + " : " + carriedTasks.size());
		if (idleTasks.size() == 0 && carriedTasks.size() == 0) {
			System.out.println("new best cost: " + currState.cost);
			bestNode = currState;
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
				nextState.heuristic = heuristic(nextState);
				addNewState(nextState);
			}
		}
	}

	/**
	 * check for cycles
	 * @param nextState
	 * @return true or false
	 */
	private void addNewState(StateNode nextState) {
		StateNode currBest = cycle.get(nextState.key);
		if (currBest == null) {
			queue.add(nextState);
		}
		else if (currBest.cost > nextState.cost) {
			currBest.parent = nextState.parent;
			currBest.cost = nextState.cost;
		}
	}
	
	/**
	 * find the heuristic term for current state
	 * @param state
	 * @return heuristic cost
	 */
	private double heuristic(StateNode state) {
		
		double longestPathLength = mShortestEdge*state.idleTasks.size();
		
		for (Task task : state.carriedTasks) {
			double pathDist = state.currCity.distanceTo(task.deliveryCity);
			longestPathLength = (pathDist > longestPathLength) ? pathDist : longestPathLength;
		}
		for (Task task : state.idleTasks) {
			double pathDist = state.currCity.distanceTo(task.pickupCity) + task.pathLength();
			longestPathLength = (pathDist > longestPathLength) ? pathDist : longestPathLength;
		}		
		
		return longestPathLength;
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

	/**
	 * finds the list of neighbors that are on the shortest path to a task
	 * @param currState
	 * @return list of cities to move to
	 */
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
