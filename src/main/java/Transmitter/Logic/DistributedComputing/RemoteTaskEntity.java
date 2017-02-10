package Transmitter.Logic.DistributedComputing;

import java.io.Serializable;
import java.util.concurrent.Callable;


public class RemoteTaskEntity implements IRemoteTaskEntity, Serializable {

    private Object _completedTaskResult;
    private Callable<Object> _taskUnit;
    private String _taskName, _assignedClientName;

    public RemoteTaskEntity() {
        this(null);
    }
    public RemoteTaskEntity(Callable<Object> taskUnit) {
        this(taskUnit, null);
    }
    public RemoteTaskEntity(Callable<Object> taskUnit, String taskName) {
        this(taskUnit, taskName, null);
    }
    public RemoteTaskEntity(Callable<Object> taskUnit, String taskName, String assignedClientName) {
        _taskUnit = taskUnit;
        _taskName = taskName;
        _assignedClientName = assignedClientName;
    }

    public void setTaskName(String taskName) {
        _taskName = taskName;
    }
    public String getTaskName() {
        return _taskName;
    }

    public void setAssignedClientName(String assignedClientName) {
        _assignedClientName = assignedClientName;
    }
    public String getAssignedClientName() {
        return _assignedClientName;
    }

    public void setTaskUnit(Callable<Object> taskUnit) {
        _taskUnit = taskUnit;
    }
    public Callable<Object> getTaskUnit() {
        return _taskUnit;
    }

    public void setCompletedTaskResult(Object completedTaskResult) {
        _completedTaskResult = completedTaskResult;
    }
    public Object getCompletedTaskResult() {
        return _completedTaskResult;
    }

}
