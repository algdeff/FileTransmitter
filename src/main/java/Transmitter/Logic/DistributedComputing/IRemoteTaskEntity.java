package Transmitter.Logic.DistributedComputing;

import java.util.concurrent.Callable;

public interface IRemoteTaskEntity {

    void setTaskName(String taskName);
    String getTaskName();

    void setAssignedClientName(String assignedClientName);
    String getAssignedClientName();

    void setTaskUnit(Callable<Object> taskUnit);
    Callable<Object> getTaskUnit();

    void setCompletedTaskResult(Object result);
    Object getCompletedTaskResult();

}
