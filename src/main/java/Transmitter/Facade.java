package Transmitter;

/**
 * Facade
 *
 * Фасад содержит все глобальные поля и методы Transmitter'а
 * все комманды и события клиента, сервера и внутренних модулей
 *
 *
 * @author  Anton Butenko
 *
 */

public final class Facade {

    public static final String  EVENT_TYPE_BROADCAST  =  "event_type_broadcast",
                                  EVENT_TYPE_PRIVATE  =  "event_type_private_to_specific_reg_name",
                                    EVENT_TYPE_GROUP  =  "event_type_group",
                                EVENT_TYPE_SUBSCRIBE  =  "event_type_subscribe_interests",
                                  EVENT_TYPE_GENERIC  =  "event_type_generic",

                                     EVENT_GROUP_ALL  =  "event_group_all",
                                  EVENT_GROUP_LOGGER  =  "logger_group_event",
                                      EVENT_GROUP_DB  =  "database_manager_group_event",
                           EVENT_GROUP_TASK_EXECUTOR  =  "event_group_task_executor",
                           EVENT_GROUP_TASK_PRODUCER  =  "event_group_task_producer",
                                EVENT_GROUP_EXECUTOR  =  "event_group_executor",
                              EVENT_GROUP_NET_SERVER  =  "event_group_net_server",
                              EVENT_GROUP_NET_CLIENT  =  "event_group_net_client",

                    TRANSITION_EVENT_GROUP_ALL_USERS  =  "event_group_transition_all_users",
                       TRANSITION_EVENT_GROUP_CLIENT  =  "transition_event_group_client",

                                        CMD_DB_FLUSH  =  "cmd_db_flush",
                                     CMD_DB_SHUTDOWN  =  "cmd_db_shutdown",

                               CMD_EXECUTOR_PUT_TASK  =  "cmd_executor_put_task",
                              CMD_EXECUTOR_TAKE_TASK  =  "cmd_executor_take_task",
                                   CMD_EXECUTOR_DEMO  =  "cmd_executor_demo",

                             CMD_NET_CLIENT_UI_BREAK  =  "cmd_net_server_ui_break",
//                             CMD_NET_CLIENT_SHUTDOWN  =  "cmd_net_server_shutdown",

                                  CMD_LOGGER_ADD_LOG  =  "cmd_logger_add_log",
                               CMD_LOGGER_ADD_RECORD  =  "cmd_logger_add_record",
                          CMD_LOGGER_CONSOLE_MESSAGE  =  "cmd_logger_console_message",
                   CMD_LOGGER_ADD_FILE_TO_STATISTICS  =  "cmd_logger_add_file_to_statistics",
                                CMD_LOGGER_CLEAR_LOG  =  "cmd_logger_clear_log",
//                                 CMD_LOGGER_SHUTDOWN  =  "cmd_logger_shutdown",

                             CMD_TASK_EXECUTOR_START  =  "cmd_task_executor_start",
                     CMD_TASK_EXECUTOR_ADD_NEW_TASKS  =  "cmd_task_executor_add_new_tasks",

                             CMD_TASK_PRODUCER_START  =  "cmd_task_producer_start",
                 CMD_TASK_PRODUCER_REGISTER_EXECUTOR  =  "cmd_task_producer_register_executor",
             CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK  =  "cmd_task_producer_collect_complete_task",
                      CMD_TASK_PRODUCER_GET_NEW_TASK  =  "cmd_task_producer_get_new_task",

//                CMD_INTERNAL_SEND_TRANSITION_EVENT  =  "cmd_server_internal_send_transition_event",
                       CMD_INTERNAL_CLIENT_BREAKDOWN  = "cmd_server_internal_client_breakdown",

                                     GLOBAL_SHUTDOWN  =  "global_shutdown",

                            CMD_SERVER_SET_CLIENT_ID  =  "cmd_server_set_client_id",
                           CMD_SERVER_GET_FILES_LIST  =  "cmd_server_get_files_list",
                                CMD_SERVER_GET_FILES  =  "cmd_server_get_files",
                                CMD_SERVER_ADD_FILES  =  "cmd_server_add_files",
                    CMD_SERVER_REMOTE_PROCEDURE_CALL  =  "cmd_server_remote_procedure_call",
                         CMD_SERVER_TRANSITION_EVENT  =  "cmd_server_transition_event",
                                CMD_SERVER_TERMINATE  =  "cmd_server_terminate";


    private static volatile Facade _instance;
    private static boolean _inited = false;

    private static boolean _isServerRole = false;

    private Facade() {
    }

    public static Facade getInstance() {
        if (_instance == null) {
            synchronized (Facade.class) {
                if (_instance == null) {
                    _instance = new Facade();
                }
            }
        }
        return _instance;
    }

    void init() {
        if (_inited) {
            return;
        }


        _inited = true;
    }

    public static void setServerRole(boolean isServerRole) {
        _isServerRole = isServerRole;
    }

    public static boolean isServerRole() {
        return _isServerRole;
    }


}
