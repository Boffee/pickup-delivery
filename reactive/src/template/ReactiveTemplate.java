package template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {
	
	private Topology topology;
	private TaskDistribution td;
	private Agent agent;
	private Double discount;
	
	// state is defined as the currentCity and the taskCity available at the currentCity
	// a value of -1 = action(take task)
	// a value between 0 and total number of cities = action(move to the neighbor city i without task)
	private Integer states[][];
	
	private static final int TAKE_ACTION = -1;
	private static final double TOLERANCE = 0.4;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		this.topology = topology;
		this.td = td;
		this.agent = agent;
		// first index stores the currentCity
		// second index stores the taskCity
		// the extra index at the end is for when there is not task available at the current city
		states = new Integer[topology.cities().size()][topology.cities().size()+1];
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		
		// initiate best action for each state as null
		for (int i = 0; i < topology.cities().size(); i++) {
			for(int j = 0; j <= topology.cities().size();j++)
			states[i][j] = null;
		}
		
		// loop through all vehicle all starting cities, and all destination cites
		for (Vehicle v: agent.vehicles()) {
			for (int i = 0; i < topology.cities().size(); i++) {
				// j is the destination city
				for (int j = 0; j <= topology.cities().size(); j++) {
					findValue(i,j, v, 0, 0);
				}
			}
		}
	}
	
	
	// determine the best action for the current state
	private double findValue(int currCityIndex, int taskCityIndex, Vehicle v, double lastQa, int iterations) {
		
		if (taskCityIndex == currCityIndex) {
			return 0;
		}
		
		// Dictionary to store all of the returned values for each action
		Map<Integer, Double> Q = new HashMap<Integer, Double>();
		
		City currentCity = topology.cities().get(currCityIndex);
		
		// a cityIndex outside the available cities means there is not task available
		// if task is available at the current city, compute the value for "take task" action
		// (computes only 1 Qa)
		if (taskCityIndex != topology.cities().size()) {
			
			City taskCity = topology.cities().get(taskCityIndex);
			// the amount paid plus the cost of gas
			double reward = td.reward(currentCity, taskCity)-v.costPerKm()*currentCity.distanceTo(taskCity);
			// I don't have internet, so I defined my own power function
			double currentQa = lastQa + Math.pow(discount, iterations)*reward;
			// if the difference between the current Qa and last Qa is small enough, stop
			// I don't know the abs function either
			if (Math.abs(lastQa-currentQa)/currentQa < TOLERANCE) {
				return currentQa;
			}
			
			double Qa = findQa(currentCity, taskCity, v, reward, currentQa, iterations+1);
			// action -1 = take the task
			Q.put(TAKE_ACTION, Qa);
		}
		
		// this accounts for the values of not taking the task as well as when no task is available
		// compute values for action "move to neighbor city i"
		// (computes n Qa's where n is the number of neighboring cities)
		for (City neighbor : currentCity.neighbors()) {
			
			double reward = -v.costPerKm()*currentCity.distanceTo(neighbor);			
			double currentQa = lastQa + Math.pow(discount, iterations)*reward;
			if (Math.abs(lastQa-currentQa)/currentQa < TOLERANCE) {
				return currentQa;
			}
			
			double Qa = findQa(currentCity, neighbor, v, reward, currentQa, iterations+1);
			// action neighbor.id means to move to neighbor
			Q.put(neighbor.id, Qa);
		}
		
		Integer maxQaAction = getMaxQaAction(Q);
		if (states[currCityIndex][taskCityIndex] == null && iterations == 0) {
			states[currCityIndex][taskCityIndex] = maxQaAction;
		}
		return Q.get(new Integer(maxQaAction));
	}
	
	
	// finds the Qa for an action that takes the vehicle from the currentCity to the destCity
	private double findQa(City currentCity, City destCity, Vehicle v, double reward, double totalQa, int iterations) {
		
		double Qa = 0;
		// the possible states to end up in are: tasks from the destCity to all other cities
		for (City nextTaskCity : topology.cities()) {
			double prob = td.probability(destCity, nextTaskCity);
		    double value = findValue(destCity.id, nextTaskCity.id, v, totalQa, iterations);
			Qa+= prob*value;
		}
		// or the destCity does not have any task to any city
		double probForNoTask = td.probability(destCity, null);
		Qa += probForNoTask*findValue(destCity.id, topology.cities().size(), v, totalQa, iterations);
		Qa *= discount;
		
		// add the reward
		Qa += reward;
		return Qa;
	}
	
	
	private Integer getMaxQaAction(Map<Integer, Double> Q) {
		Map.Entry<Integer, Double> maxAction = null;
		for (Map.Entry<Integer, Double>action : Q.entrySet()) {
			if (maxAction == null || action.getValue().compareTo(maxAction.getValue()) > 0) {
				maxAction = action;
			}
		}
		return maxAction.getKey();
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		
		City currentCity = vehicle.getCurrentCity();

		// if there is a task available
		if (availableTask != null) {
			City taskCity = availableTask.deliveryCity;
			Integer maxQaAction = states[currentCity.id][taskCity.id];
			// check if action is worth taking
			if (maxQaAction == TAKE_ACTION) {
				action = new Pickup(availableTask);
			}
			// if not move, the best city specified for the current state
			else {
				City moveToCity = topology.cities().get(maxQaAction.intValue());
				action = new Move(moveToCity);
			}

		}
		// no task available
		else {
			Integer maxQaAction = states[currentCity.id][topology.size()];
			City moveToCity = topology.cities().get(maxQaAction.intValue());
			action = new Move(moveToCity);
		}
		return action;
	}	
	
}
