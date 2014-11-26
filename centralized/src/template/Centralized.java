package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class Centralized implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
	    long start = System.currentTimeMillis();

		List<VehiclePlan> vPlans = (new TaskAssignment(vehicles, tasks)).getPlans();
		List<Plan> plans = new ArrayList<Plan>();
		for (Vehicle vehicle : vehicles) {
			for (VehiclePlan vPlan : vPlans) {
				if (vPlan.getVehicle() == vehicle) {
					plans.add(vPlan.getPlan());
				}
			}
		}
		
		System.out.println("Running time: " + (System.currentTimeMillis()-start) + " ms");

		return plans;
	}
	
}
