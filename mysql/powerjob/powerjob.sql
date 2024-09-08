/*
 Navicat Premium Data Transfer

 Source Server         : Ar_Test
 Source Server Type    : MySQL
 Source Server Version : 80035
 Source Host           : 8.222.160.178:3306
 Source Schema         : powerjob-daily

 Target Server Type    : MySQL
 Target Server Version : 80035
 File Encoding         : 65001

 Date: 30/11/2023 14:04:30
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for container_info
-- ----------------------------
DROP TABLE IF EXISTS `container_info`;
CREATE TABLE `container_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '容器ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `container_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '容器名称',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  `last_deploy_time` datetime NULL DEFAULT NULL COMMENT '上次部署时间',
  `source_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源信息,内容取决于source_type\n1、FatJar -> String\n2、Git -> JSON，{\"repo”:””仓库,”branch”:”分支”,”username”:”账号,”password”:”密码”}',
  `source_type` int NOT NULL COMMENT '资源类型,1:FatJar/2:Git',
  `status` int NOT NULL COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '版本',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx01_container_info`(`app_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '容器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of container_info
-- ----------------------------

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱',
  `extra` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '扩展字段',
  `web_hook` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'webhook地址',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uidx01_user_info`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uidx02_user_info`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_info
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_info`;
CREATE TABLE `workflow_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `wf_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作流名称',
  `wf_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工作流描述',
  `extra` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '扩展字段',
  `lifecycle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生命周期',
  `max_wf_instance_num` int NOT NULL DEFAULT 1 COMMENT '最大运行工作流数量,默认 1',
  `next_trigger_time` bigint NULL DEFAULT NULL COMMENT '下次调度时间',
  `notify_user_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '报警用户(多值逗号分割)',
  `pedag` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'DAG信息(JSON)',
  `status` int NOT NULL COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
  `time_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '时间表达式,内容取决于time_expression_type,1:CRON/2:NULL/3:LONG/4:LONG',
  `time_expression_type` int NOT NULL COMMENT '时间表达式类型,1:CRON/2:API/3:FIX_RATE/4:FIX_DELAY,5:WORKFLOW\n）',
  `gmt_create` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx01_workflow_info`(`app_id` ASC, `status` ASC, `time_expression_type` ASC, `next_trigger_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工作流表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workflow_info
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_instance_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_instance_info`;
CREATE TABLE `workflow_instance_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作流实例ID',
  `wf_instance_id` bigint NULL DEFAULT NULL COMMENT '工作流实例ID',
  `workflow_id` bigint NULL DEFAULT NULL COMMENT '工作流ID',
  `actual_trigger_time` bigint NULL DEFAULT NULL COMMENT '实际触发时间',
  `app_id` bigint NULL DEFAULT NULL COMMENT '应用ID',
  `dag` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'DAG信息(JSON)',
  `expected_trigger_time` bigint NULL DEFAULT NULL COMMENT '计划触发时间',
  `finished_time` bigint NULL DEFAULT NULL COMMENT '执行结束时间',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '执行结果',
  `status` int NULL DEFAULT NULL COMMENT '工作流实例状态,1:等待调度WAITING/2:运行中RUNNING/3:失败FAILED/4:成功SUCCEED/10:手动停止STOPPED',
  `wf_context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工作流上下文',
  `wf_init_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工作流启动参数',
  `gmt_create` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `parent_wf_instance_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uidx01_wf_instance`(`wf_instance_id` ASC) USING BTREE,
  INDEX `idx01_wf_instance`(`workflow_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx02_wf_instance`(`app_id` ASC, `status` ASC, `expected_trigger_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工作流实例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workflow_instance_info
-- ----------------------------

-- ----------------------------
-- Table structure for workflow_node_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_info`;
CREATE TABLE `workflow_node_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `enable` bit(1) NOT NULL COMMENT '是否启动,0:否/1:是',
  `extra` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '扩展字段',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  `job_id` bigint NULL DEFAULT NULL COMMENT '任务ID',
  `node_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '节点名称',
  `node_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '节点参数',
  `skip_when_failed` bit(1) NOT NULL COMMENT '是否允许失败跳过,0:否/1:是',
  `type` int NULL DEFAULT NULL COMMENT '节点类型,1:任务JOB',
  `workflow_id` bigint NULL DEFAULT NULL COMMENT '工作流ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx01_workflow_node_info`(`workflow_id` ASC, `gmt_create` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工作流节点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workflow_node_info
-- ----------------------------

-- ----------------------------
-- Table structure for app_info
-- ----------------------------
DROP TABLE IF EXISTS `app_info`;
CREATE TABLE `app_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '应用ID',
  `app_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `current_server` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Server地址,用于负责调度应用的ActorSystem地址',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用密码',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uidx01_app_info`(`app_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '应用表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of app_info
-- ----------------------------
INSERT INTO `app_info` VALUES (1, 'powerjob-worker-samples', '192.168.254.1:10010', '2023-09-13 15:49:05', '2023-11-30 14:39:24', 'powerjob123');

-- ----------------------------
-- Table structure for oms_lock
-- ----------------------------
DROP TABLE IF EXISTS `oms_lock`;
CREATE TABLE `oms_lock`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '序号ID',
  `lock_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `max_lock_time` bigint NULL DEFAULT NULL COMMENT '最长持锁时间',
  `ownerip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '拥有者IP',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uidx01_oms_lock`(`lock_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 922 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据库锁' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of oms_lock
-- ----------------------------

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
DROP TABLE IF EXISTS `job_info`;
CREATE TABLE `job_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NULL DEFAULT NULL COMMENT '应用ID',
  `job_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务名称',
  `job_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务描述',
  `job_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '任务默认参数',
  `concurrency` int NULL DEFAULT NULL COMMENT '并发度,同时执行某个任务的最大线程数量',
  `designated_workers` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '运行节点,空:不限(多值逗号分割)',
  `dispatch_strategy` int NULL DEFAULT NULL COMMENT '投递策略,1:健康优先/2:随机',
  `execute_type` int NOT NULL COMMENT '执行类型,1:单机STANDALONE/2:广播BROADCAST/3:MAP_REDUCE/4:MAP',
  `instance_retry_num` int NOT NULL DEFAULT 0 COMMENT 'Instance重试次数',
  `instance_time_limit` bigint NOT NULL DEFAULT 0 COMMENT '任务整体超时时间',
  `lifecycle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生命周期',
  `max_instance_num` int NOT NULL DEFAULT 1 COMMENT '最大同时运行任务数,默认 1',
  `max_worker_count` int NOT NULL DEFAULT 0 COMMENT '最大运行节点数量',
  `min_cpu_cores` double NOT NULL DEFAULT 0 COMMENT '最低CPU核心数量,0:不限',
  `min_disk_space` double NOT NULL DEFAULT 0 COMMENT '最低磁盘空间(GB),0:不限',
  `min_memory_space` double NOT NULL DEFAULT 0 COMMENT '最低内存空间(GB),0:不限',
  `next_trigger_time` bigint NULL DEFAULT NULL COMMENT '下一次调度时间',
  `notify_user_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '报警用户(多值逗号分割)',
  `processor_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行器信息',
  `processor_type` int NOT NULL COMMENT '执行器类型,1:内建处理器BUILT_IN/2:SHELL/3:PYTHON/4:外部处理器（动态加载）EXTERNAL',
  `status` int NOT NULL COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
  `task_retry_num` int NOT NULL DEFAULT 0 COMMENT 'Task重试次数',
  `time_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '时间表达式,内容取决于time_expression_type,1:CRON/2:NULL/3:LONG/4:LONG',
  `time_expression_type` int NOT NULL COMMENT '时间表达式类型,1:CRON/2:API/3:FIX_RATE/4:FIX_DELAY,5:WORKFLOW\n）',
  `tag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'TAG',
  `log_config` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '日志配置',
  `extra` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '扩展字段',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  `alarm_config` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx01_job_info`(`app_id` ASC, `status` ASC, `time_expression_type` ASC, `next_trigger_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of job_info
-- ----------------------------
INSERT INTO `job_info` VALUES (1, 1, 'HandlePaymentOrderProcessor', '清洗代收订单数据', '', 5, '', 1, 1, 1, 100000, '{\"end\":1703966400000,\"start\":1699819200000}', 5, 1, 1, 0.001, 0.001, 1701338700000, NULL, 'org.ar.job.processor.HandlePaymentOrderProcessor', 1, 1, 1, '0 */5 * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-09-15 17:37:34', '2023-11-30 17:59:43', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');
INSERT INTO `job_info` VALUES (2, 1, 'MerchantNotify', '定时发送通知', NULL, 5, '', 1, 1, 1, 1, '{\"end\":1793304000000,\"start\":1694635200000}', 1, 2, 1, 1, 1, 1696683120000, NULL, 'org.ar.pay.job.MerchantNotify', 1, 2, 1, '0 */1 * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-09-30 15:27:28', '2023-10-07 20:50:54', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');
INSERT INTO `job_info` VALUES (3, 1, 'MatchingOrderProcessor', '定时扫描未成功和未匹配的订单添加到交易池', NULL, 5, '', 1, 1, 1, 0, '{\"end\":1750881600000,\"start\":1696968000000}', 1, 2, 1, 1, 1, 1697546980000, NULL, 'org.ar.wallet.job.MatchingOrderProcessor', 1, 99, 1, ' 0/10 * * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-10-11 20:32:00', '2023-10-23 14:31:34', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');
INSERT INTO `job_info` VALUES (4, 1, 'MatchedNotifyProcessor', '定时扫描匹配成功的订单并发送到MQ队列, MQ消费端进行回调商户', NULL, 5, '', 1, 1, 1, 0, '{\"end\":1793476800000,\"start\":1696104000000}', 1, 2, 1, 1, 1, 1698061755000, NULL, 'org.ar.wallet.job.MatchedNotifyProcessor', 1, 99, 1, ' 0/5 * * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-10-13 15:27:38', '2023-10-23 19:50:07', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');
INSERT INTO `job_info` VALUES (5, 1, 'TradeNotifyProcessor', '定时扫描充值成功的订单并发送到MQ队列, MQ消费端进行回调商户', NULL, 5, '', 1, 1, 1, 0, '{\"end\":1793476800000,\"start\":1696104000000}', 1, 2, 1, 1, 1, 1698061755000, NULL, 'org.ar.wallet.job.TradeNotifyProcessor', 1, 99, 1, ' 0/5 * * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-10-16 13:31:27', '2023-10-23 19:51:43', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');
INSERT INTO `job_info` VALUES (6, 1, 'RecommendAmountProcessor', '定时将推荐金额列表推送给前端', NULL, 5, '', 1, 1, 1, 0, '{\"end\":1825012800000,\"start\":1696104000000}', 1, 2, 1, 1, 1, 1698039700000, NULL, 'org.ar.wallet.job.RecommendAmountProcessor', 1, 99, 1, ' 0/2 * * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-10-19 16:19:09', '2023-10-23 14:30:43', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');
INSERT INTO `job_info` VALUES (7, 1, 'MatchedNotifyProcessor', '定时任务-发送匹配成功MQ消息', NULL, 5, '', 1, 1, 1, 1, '{\"end\":1793476800000,\"start\":1696104000000}', 1, 2, 1, 1, 1, 1698645970000, NULL, 'org.ar.wallet.job.MatchedNotifyProcessor', 1, 2, 1, ' 0/10 * * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-10-23 20:29:34', '2023-10-30 14:06:16', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');
INSERT INTO `job_info` VALUES (8, 1, 'TradeNotifyProcessor', '定时任务-发送交易成功MQ消息', NULL, 5, '', 1, 1, 1, 1, '{\"end\":1793476800000,\"start\":1696104000000}', 1, 2, 1, 1, 1, 1698645970000, NULL, 'org.ar.wallet.job.TradeNotifyProcessor', 1, 2, 1, ' 0/10 * * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-10-23 20:32:37', '2023-10-30 14:06:18', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');
INSERT INTO `job_info` VALUES (9, 1, 'HandleWithdrawOrderProcessor', '清洗代付订单数据', '', 5, '', 1, 1, 1, 100000, '{\"end\":1703966400000,\"start\":1699819200000}', 5, 1, 1, 0.001, 0.001, 1701338700000, NULL, 'org.ar.job.processor.HandleWithdrawOrderProcessor', 1, 1, 1, '0 */5 * * * ?', 2, NULL, '{\"level\":1,\"loggerName\":\"1\",\"type\":2}', NULL, '2023-11-21 21:16:28', '2023-11-30 17:59:43', '{\"alertThreshold\":0,\"silenceWindowLen\":0,\"statisticWindowLen\":0}');

-- ----------------------------
-- Table structure for instance_info
-- ----------------------------
DROP TABLE IF EXISTS `instance_info`;
CREATE TABLE `instance_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务实例ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `instance_id` bigint NOT NULL COMMENT '任务实例ID',
  `type` int NOT NULL COMMENT '任务实例类型,1:普通NORMAL/2:工作流WORKFLOW',
  `job_id` bigint NOT NULL COMMENT '任务ID',
  `instance_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '任务动态参数',
  `job_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '任务静态参数',
  `actual_trigger_time` bigint NULL DEFAULT NULL COMMENT '实际触发时间',
  `expected_trigger_time` bigint NULL DEFAULT NULL COMMENT '计划触发时间',
  `finished_time` bigint NULL DEFAULT NULL COMMENT '执行结束时间',
  `last_report_time` bigint NULL DEFAULT NULL COMMENT '最后上报时间',
  `result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '执行结果',
  `running_times` bigint NULL DEFAULT NULL COMMENT '总执行次数,用于重试判断',
  `status` int NOT NULL COMMENT '任务状态,1:等待派发WAITING_DISPATCH/2:等待Worker接收WAITING_WORKER_RECEIVE/3:运行中RUNNING/4:失败FAILED/5:成功SUCCEED/9:取消CANCELED/10:手动停止STOPPED',
  `task_tracker_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'TaskTracker地址',
  `wf_instance_id` bigint NULL DEFAULT NULL COMMENT '工作流实例ID',
  `additional_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '附加信息 (JSON)',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx01_instance_info`(`job_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx02_instance_info`(`app_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx03_instance_info`(`instance_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4344 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务实例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of instance_info
-- ----------------------------

-- ----------------------------
-- Table structure for server_info
-- ----------------------------
DROP TABLE IF EXISTS `server_info`;
CREATE TABLE `server_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '服务器ID',
  `gmt_create` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '服务器IP地址',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uidx01_server_info`(`ip` ASC) USING BTREE,
  INDEX `idx01_server_info`(`gmt_modified` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '服务器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of server_info
-- ----------------------------
INSERT INTO `server_info` VALUES (1, '2023-11-21 19:30:13', '2023-11-24 19:38:31', '192.168.70.53');
INSERT INTO `server_info` VALUES (2, '2023-11-23 18:34:24', '2023-11-30 18:04:28', '192.168.254.1');
INSERT INTO `server_info` VALUES (3, '2023-11-29 21:12:57', '2023-11-30 18:04:32', '192.168.70.82');

SET FOREIGN_KEY_CHECKS = 1;
