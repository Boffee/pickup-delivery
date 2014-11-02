package template;

/* import table */
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class Deliberative implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	TaskSet carriedTasks = null;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;
	double shortestEdge = 0;

	/* the planning class */
	Algorithm algorithm;
	
	
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// find shortest edge in topology for heuristic computation
		for (City city : topology.cities()) {
			for (City neighbor : city.neighbors()) {
				double edge = city.distanceTo(neighbor);
				shortestEdge = (edge > shortestEdge) ? edge : shortestEdge;
			}
		}
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

	    long start = System.currentTimeMillis();
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = new ASTAR(vehicle, tasks, carriedTasks, shortestEdge).getPlan();
			System.out.println("Running time: " + (System.currentTimeMillis()-start) + " ms");
			break;
		case BFS:
			// ...
			plan = new BFS(vehicle, tasks, carriedTasks).getPlan();
			System.out.println("Running time: " + (System.currentTimeMillis()-start) + " ms");
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
	
	
	@Override
	public void planCancelled(TaskSet carriedTasks) {
		this.carriedTasks = carriedTasks;
	}
}
