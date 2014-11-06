package template;

import java.util.ArrayList;
import java.util.List;

import logist.simulation.Vehicle;

public class VehiclePlan {
	Vehicle vehicle;
	List<TaskNode> tasks;

	public VehiclePlan (Vehicle _vehicle, List<TaskNode> _tasks) {
		vehicle = _vehicle;
		if (_tasks == null) {
			tasks = new ArrayList<TaskNode>();
		} else {
			tasks = _tasks;
		}
	}
}
