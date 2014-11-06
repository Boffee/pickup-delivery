package template;

import java.util.ArrayList;
import java.util.List;


public class TaskNodeList {
	
	private List<TaskNode> tasks;
	
	public TaskNodeList() {
		tasks = new ArrayList<TaskNode>();
	}

	/**
	 * swap the order of two tasks
	 * @param tIdx1
	 * @param tIdx2
	 */
	public void swapTasksOrder(int tIdx1, int tIdx2) {
		TaskNode task1 = tasks.remove(tIdx1);
		TaskNode task2 = tasks.remove(tIdx2);
		// switches the pointers of the tasks
		task1.switchWith(task2);
		// prevent index out of bound problems
		if (tIdx1 < tIdx2) {
			tasks.add(tIdx1, task2);
			tasks.add(tIdx2, task1);
		} else {
			tasks.add(tIdx2, task1);
			tasks.add(tIdx1, task2);
		}
	}
	
	
	/**
	 * add a new task node to the end of the list
	 * @param task
	 */
	public void addTask(TaskNode task) {
		TaskNode endTask = tasks.get(tasks.size()-1);
		endTask.setNextTaskNode(task);
		task.setPrevTaskNode(endTask);
		task.setNextTaskNode(null);
		tasks.add(task);
	}
	
	
	/**
	 * remove the TaskNode at the given index
	 * @param tIdx
	 * @return the removed TaskNode
	 */
	public TaskNode removeTask(int tIdx) {
		TaskNode rmTask = tasks.get(tIdx);
		
		TaskNode prevTask = rmTask.getPrevTaskNode();
		TaskNode nextTask = rmTask.getNextTaskNode();
		prevTask.setNextTaskNode(nextTask);
		nextTask.setPrevTaskNode(prevTask);
		
		rmTask.setPrevTaskNode(null);
		rmTask.setNextTaskNode(null);
		
		return rmTask;
	}
	
	public int size() {
		return tasks.size();
	}

}
