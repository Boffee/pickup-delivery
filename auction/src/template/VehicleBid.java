package template;

import java.util.AbstractCollection;
import java.util.ArrayList;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class VehicleBid {



	private double currentCost = 0;
	
	private ArrayList<Task> tasks = new ArrayList<Task>();
	private ArrayList<Integer> id = new ArrayList<Integer>();
	private ArrayList<Task> supposedTasks = new ArrayList<Task>();
	private Vehicle vehicle;
	private Plan plan;
	private Plan newPlan;


	VehicleBid(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	/**
	 * Compute a plan with our task and an additional task then compute a bid using the marginal cost and some ratio
	 * @param task
	 * @param  
	 * @return
	 */
	public double bid(Task task) {

		// update supposedTasks (tasks + new task to bid)

		supposedTasks.clear();
		supposedTasks.addAll(tasks);
		supposedTasks.add(task);
		// compute a new plan with the updated task set
		if(supposedTasks.size() == 1) {
			newPlan = new Plan(vehicle.getCurrentCity());
			newPlan.appendMove(task.pickupCity);
			newPlan.appendPickup(task);
			newPlan.appendMove(task.deliveryCity);
			newPlan.appendDelivery(task);
		} else {
			newPlan = new ASTAR(vehicle, supposedTasks, null, Auction.shortestEdge).getPlan();
		}
		// compute its cost

		double newCost = newPlan.totalDistance()*vehicle.costPerKm();

		// compute the marginal cost
		double marginalCost = newCost-currentCost;
	
		return marginalCost;
	}


	public ArrayList<Task> getTasks() {
		return tasks;
	}


	public ArrayList<Task> getSupposedTasks() {
		return supposedTasks;
	}

	// function called if we win the auction for a task
	// since it's always called after the result of the auction, so after "bid", newPlan will still be our winning plan
	// no need to actualize the plan if we lost
	/**
	 * Actualize the plan with a new task and update the current cost. The function is called only if the vehicle had won the auction so the plan was already computed.
	 * @param task new task to add in the plan
	 * @param winner 
	 */
	public void actualizePlan(Task task, int winner) {
		//update task set
		tasks.add(task);
		id.add(task.id);
		// plan is the newly computed Plan in bid
		plan = newPlan;
		// update the cost of our current plan
		currentCost = plan.totalDistance()*vehicle.costPerKm();
	}	

	public Plan getPlan(TaskSet tasks) {

		ArrayList<Task> taskList = new ArrayList<Task>();
		for(Task task : tasks) {
			if(id.contains(task.id)) {
				taskList.add(task);
			}
		}
		return new ASTAR(vehicle, taskList, null, Auction.shortestEdge).getPlan();
	}	

	public double getCurrentCost() {
		return currentCost;
	}

}

