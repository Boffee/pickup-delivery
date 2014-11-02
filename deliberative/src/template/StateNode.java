package template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import logist.task.Task;
import logist.topology.Topology.City;

class StateNode {

	public City currCity;
	public List<Task> idleTasks = new ArrayList<Task>();
	public List<Task> carriedTasks = new ArrayList<Task>();
	public StateNode parent;
	public String key;
	public double cost;
	public double heuristic = 0;

	public StateNode(City _currCity, List<Task> _idleTasks, List<Task> _carriedTasks, StateNode _parent, double _cost) {
		currCity = _currCity;
		parent = _parent;
		carriedTasks = _carriedTasks;
		idleTasks = _idleTasks;
		cost = _cost;
		key = (new HashData(currCity, idleTasks, carriedTasks)).hashable;
	}
	
	

	public List<Task> getIdleTasks() {
		return idleTasks;

	}

	public List<Task> getCarriedTasks() {
		return carriedTasks;	
	}

	public boolean equals(StateNode otherNode) {
		if (currCity != otherNode.currCity) return false;
		if (idleTasks.size() != otherNode.idleTasks.size()) return false;
		if (carriedTasks.size() != otherNode.carriedTasks.size()) return false;
		for (Task task : idleTasks) {
			if (!(otherNode.idleTasks.contains(task))) return false;
		}
		for (Task task : carriedTasks) {
			if (!(otherNode.carriedTasks.contains(task))) return false;
		}
		return true;
	}
	
	private class HashData {
		private Integer cityId;
		private List<Integer> idleTaskIds = new ArrayList<Integer>();
		private List<Integer> carriedTaskIds = new ArrayList<Integer>();
		public String hashable;

		public HashData(City city, List<Task> idleTasks, List<Task> carriedTasks) {
			cityId = city.id;
			for (Task task : idleTasks) {
				idleTaskIds.add(task.id);
			}
			for (Task task : carriedTasks) {
				carriedTaskIds.add(task.id);
			}
			Collections.sort(idleTaskIds);
			Collections.sort(carriedTaskIds);

			// generate a key for this state to be used in cycle detection
			StringBuilder sb = new StringBuilder();
			sb.append(cityId);
			sb.append("c");
			for (Integer id : idleTaskIds) {
				sb.append(id);
			}
			sb.append("t");
			for (Integer id : carriedTaskIds) {
				sb.append(id);
			}
			hashable = sb.toString();
		}
	}
}
