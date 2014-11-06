package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

public class SLS {
	
	private List<VehiclePlan> plans = new ArrayList<VehiclePlan>();
	private List<Task> tasks = new ArrayList<Task>();
	
	private Topology topology;
	
	public SLS(List<Vehicle> _vehicles, Topology _topology, TaskSet _tasks) {
		for (Vehicle vehicle: _vehicles) {
			plans.add(new VehiclePlan(vehicle, null));
		}
		for (Task task : _tasks) {
			tasks.add(task);
		}
		topology = _topology;
		
	}
	
	private void neighborPermutation() {
		
	}
	
	
	private VehiclePlan bestChangeVehicle() {
		
		Random rand = new Random();
		// pick a random vehicle
		int vIndex = rand.nextInt(plans.size());
		VehiclePlan plan1 = new VehiclePlan(plans.get(vIndex));

		// pick a random task on the selected vehicle
		int tIndex = rand.nextInt(plan1.size());		
		Task removedTask = plan1.removeTask(tIndex);
		
		double baseCostChange = 
		
		for (VehiclePlan plan : plans) {
			double origCost = plan.getCost();
			
			VehiclePlan newPlan = new VehiclePlan(plan);

			tIndex = rand.nextInt(plan.size()+1);
			newPlan.addTask(tIndex, removedTask);
			
			double newCost = newPlan.getCost();
			
			if ()
		}
	}
	
}
