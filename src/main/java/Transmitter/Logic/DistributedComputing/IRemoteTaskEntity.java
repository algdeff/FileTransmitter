package Transmitter.Logic.DistributedComputing;

import java.util.concurrent.Callable;

public interface IRemoteTaskEntity {

    String TASK_TYPE_INSTANT = "task_type_instant";
    String TASK_TYPE_SCHEDULED = "task_type_scheduled";

    void setTaskName(String taskName);
    String getTaskName();

    String getTaskType();

    void setAssignedClientName(String assignedClientName);
    String getAssignedClientName();

    void setTaskUnit(Callable<Object> taskUnit);
    Callable<Object> getTaskUnit();

    void setCompletedTaskResult(Object result);
    Object getCompletedTaskResult();

}
