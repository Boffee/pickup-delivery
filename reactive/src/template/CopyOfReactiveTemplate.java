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

public class CopyOfReactiveTemplate implements ReactiveBehavior {
	
	private Topology topology;
	private TaskDistribution td;
	private Agent agent;
	private Double discount;
	
	// state is defined as the currentCity and the taskCity available at the currentCity
	// a value of -1 = action(take task)
	// a value between 0 and total number of cities = action(move to the neighbor city i without task)
	private Integer states[][];
	private Double values[][];
	private Double newValues[][];
	
	private static final int TAKE_ACTION = -1;
	private static final double TOLERANCE = 0.0000001;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		this.topology = topology;
		this.td = td;
		this.agent = agent;
		// first index stores the currentCity
		// second index stores the taskCity
		// the extra index at the end is for when there is not task available at the current city
		states = new Integer[topology.cities().size()][topology.cities().size()+1];
		values = new Double[topology.cities().size()][topology.cities().size()+1];
		newValues = new Double[topology.cities().size()][topology.cities().size()+1];
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		
		// initiate best action for each state as null
		for (int i = 0; i < topology.cities().size(); i++) {
			for(int j = 0; j <= topology.cities().size();j++) {
				states[i][j] = null;
				values[i][j] = 0.0;
				newValues[i][j] = 0.0;
			}
		}
		
		// loop through all vehicle all starting cities, and all destination cites
		Vehicle v = agent.vehicles().get(0);
		
		boolean end = false;
		
		while(!end) {
			int nullStates = 0;
			for (int i = 0; i < topology.cities().size(); i++) {
				// j is the destination city
				for (int j = 0; j <= topology.cities().size(); j++) {
					if (i != j && states[i][j] == null) {	
						nullStates++;
					}
				}
			}
			if (nullStates == 0) {
				end = true;
				break;
			}
			for (int i = 0; i < topology.cities().size(); i++) {
				// j is the destination city
				for (int j = 0; j <= topology.cities().size(); j++) {
					if (states[i][j] == null) {	
						findValue(i,j, v);
					}
				}
			}
			values = newValues.clone();
		}
		for (int i = 0; i < topology.cities().size(); i++) {
			// j is the destination city
			for (int j = 0; j <= topology.cities().size(); j++) {
				System.out.println("i:"+i+" j:"+j +" action: " + states[i][j]);
			}
		}
	}
	
	
	// determine the best action for the current state
	private void findValue(int currCityIndex, int taskCityIndex, Vehicle v) {
		
		if (taskCityIndex == currCityIndex) {
			return;
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
			
			double Qa = findQa(currentCity, taskCity, reward);
			// action -1 = take the task
			Q.put(TAKE_ACTION, Qa);
		}
		
		// this accounts for the values of not taking the task as well as when no task is available
		// compute values for action "move to neighbor city i"
		// (computes n Qa's where n is the number of neighboring cities)
		for (City neighbor : currentCity.neighbors()) {
			
			double reward = v.costPerKm()*currentCity.distanceTo(neighbor);			
			
			double Qa = findQa(currentCity, neighbor, reward);
			// action neighbor.id means to move to neighbor
			Q.put(neighbor.id, Qa);
		}
		
		Integer maxQaAction = getMaxQaAction(Q);
		Double V = Q.get(maxQaAction);
		newValues[currCityIndex][taskCityIndex] = V;
		
		if ((V-values[currCityIndex][taskCityIndex])/(V) <= TOLERANCE) {
			if (states[currCityIndex][taskCityIndex] == null) {
				states[currCityIndex][taskCityIndex] = maxQaAction;
			}
		}
	}
	
	
	// finds the Qa for an action that takes the vehicle from the currentCity to the destCity
	private double findQa(City currentCity, City destCity, double reward) {
		
		double Qa = 0;
		// the possible states to end up in are: tasks from the destCity to all other cities
		for (City nextTaskCity : topology.cities()) {
			double prob = td.probability(destCity, nextTaskCity);
		    double value = values[destCity.id][nextTaskCity.id];//findValue(destCity.id, nextTaskCity.id, v, totalQa, iterations);
			Qa+= prob*value;
		}
		// or the destCity does not have any task to any city
		double probForNoTask = td.probability(destCity, null);
		Qa += probForNoTask*values[destCity.id][ topology.cities().size()];//findValue(destCity.id, topology.cities().size(), v, totalQa, iterations);
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
