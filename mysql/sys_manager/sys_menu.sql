/*
 Navicat Premium Data Transfer

 Source Server         : Ar_Test
 Source Server Type    : MySQL
 Source Server Version : 80035
 Source Host           : 8.222.160.178:3306
 Source Schema         : sys_manager

 Target Server Type    : MySQL
 Target Server Version : 80035
 File Encoding         : 65001

 Date: 30/11/2023 14:00:01
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '菜单名称',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父菜单ID',
  `path` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '路由路径',
  `component` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '菜单图标',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `visible` tinyint(1) NULL DEFAULT 1 COMMENT '状态：0-禁用 1-开启',
  `redirect` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '跳转路径',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 297 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '菜单管理' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', -1, '/admin', '/Layout', ' el-icon-setting', 1, 1, NULL, '2021-08-28 09:12:21', NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (2, '用户管理', 1, '/user', '/admin/user/index', 'el-icon-user', 1, 1, NULL, '2021-08-29 09:12:21', '2021-08-29 09:12:21', NULL, NULL);
INSERT INTO `sys_menu` VALUES (3, '菜单管理', 1, '/menu', '/admin/menu/index', 'el-icon-menu', 2, 1, NULL, '2021-08-28 09:12:21', '2021-08-28 09:12:21', NULL, NULL);
INSERT INTO `sys_menu` VALUES (234, '角色管理', 1, '/role', '/admin/role/index', 'el-icon-s-custom', 2, 1, '', '2022-04-23 12:36:11', '2022-04-23 12:36:11', NULL, NULL);
INSERT INTO `sys_menu` VALUES (235, '商户列表', 250, '/merchantInfo', '/admin/merchantInfo/index', '3', 3, 1, '', '2023-09-23 15:03:48', NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (250, '商户管理', -1, '/Layout', '/Layout', 'adf', 1, 1, '1', '2023-10-20 15:57:27', NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (251, '商户帐变', 250, '/merchant/changes', '/merchant/changes', '12412', 1, 1, '', '2023-10-20 15:57:30', NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (255, '下发记录', 250, '/merchant/record', '/merchant/record', '', 0, 1, '', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (256, '系统日志', 1, '/log', '/admin/log/index', '1', 0, 1, '', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (257, '仪表盘', -1, '/dashboard/monitor', '/dashboard/monitor', '1', 0, 1, '', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (258, '数据概况', 257, '/dashboard/monitor', '/dashboard/monitor', '1', 0, 1, '', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (263, 'IP白名单', 1, '/white', '/admin/white/index', 'seting', 0, 1, '/white', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (264, '支付管理', -1, '/pay/index', '/Layout', 'seting', 0, 1, '/pay/index', NULL, '2023-11-18 14:43:17', NULL, 'test2');
INSERT INTO `sys_menu` VALUES (265, '小鬼按钮', 257, '213', '/Layout', '123', 0, 0, '112', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (266, '币种管理', 264, '/pay/currency/index', '/pay/currency/index', 'seting', 0, 1, '/pay/currency/index', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (267, '支付通道', 264, '/pay/aisle/index', '/pay/aisle/index', 'seting', 0, 1, '/pay/aisle/index', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (268, '银行卡通道', 264, '/pay/bank/index', '/pay/bank/index', 'seting', 0, 1, '/pay/bank/index', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (269, '三方列表', 264, '/pay/tripartite/index', '/pay/tripartite/index', 'seting', 0, 1, '/pay/tripartite/index', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (270, '支付类型', 264, '/pay/payment/index', '/pay/payment/index', 'seting', 0, 1, '/pay/payment/index', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (271, '卡片管理', 264, '/pay/card/index', '/pay/card/index', 'seting', 0, 1, '/pay/card/index', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (272, '配置管理', -1, '/config', '/Layout', 'config', 3, 1, '/config', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (273, '参数配置', 272, '/config/setting/index', '/config/setting/index', 'config', 0, 1, '/config/setting/index', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (275, '下发申请', 250, '/merchant/application', '/merchant/application', '1', 0, 1, '/merchant/apply', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (276, 'USDT管理', 272, '/config/usdt/index', '/config/usdt/index', '1', 0, 1, '/config/usdt/index', NULL, NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (280, '充值取消原因', 272, '/config/rechargeCancel/index', '/config/rechargeCancel/index', '1', 0, 1, '', '2023-11-16 21:01:18', '2023-11-16 21:01:38', 'dev007', 'dev007');
INSERT INTO `sys_menu` VALUES (281, '提现取消原因', 272, '/config/withdrawCancel/index', '/config/withdrawCancel/index', '1', 0, 1, '', '2023-11-16 21:02:06', '2023-11-16 21:02:06', 'dev007', 'dev007');
INSERT INTO `sys_menu` VALUES (282, '订单管理', -1, '/order', '/Layout', 'order', 0, 1, '/order', '2023-11-17 15:01:35', '2023-11-17 15:15:22', 'test', 'test');
INSERT INTO `sys_menu` VALUES (283, '申诉订单', 282, '/order/appeal/index', '/order/appeal/index', '1', 0, 1, '/order/appeal/index', '2023-11-17 15:03:53', '2023-11-17 15:17:09', 'test', 'test');
INSERT INTO `sys_menu` VALUES (284, '卖出订单', 282, '/order/sale/index', '/order/sale/index', '1', 0, 1, '/order/sale/index', '2023-11-17 15:08:16', '2023-11-17 15:17:40', 'test', 'test');
INSERT INTO `sys_menu` VALUES (285, '代收报表', 287, '/report/moonOf', '/report/moonOf', '1', 0, 1, '', '2023-11-17 19:17:09', '2023-11-18 14:23:45', 'test2', 'test2');
INSERT INTO `sys_menu` VALUES (286, '代付报表', 287, '/report/moonPay', '/report/moonPay', '1', 0, 1, '2', '2023-11-17 19:18:29', '2023-11-18 14:23:51', 'test2', 'test2');
INSERT INTO `sys_menu` VALUES (287, '报表中心', -1, '/report', '/Layout', '1', 0, 1, '12', '2023-11-17 19:30:05', '2023-11-18 14:42:37', 'test2', 'test2');
INSERT INTO `sys_menu` VALUES (288, '卖出订单记录', 282, '/order/saleRecord/index', '/order/saleRecord/index', '1', 0, 1, '/order/saleRecord/index', '2023-11-17 21:50:29', '2023-11-17 21:52:01', 'test', 'test');
INSERT INTO `sys_menu` VALUES (289, '买入订单', 282, '/order/buyOrder/index', '/order/buyOrder/index', '1', 0, 1, '/order/buyOrder/index', '2023-11-17 21:56:57', '2023-11-17 22:01:20', 'test', 'test');
INSERT INTO `sys_menu` VALUES (290, '买入订单记录', 282, '/order/buyRecord/index', '/order/buyRecord/index', '1', 0, 1, '/order/buyRecord/index', '2023-11-17 21:58:04', '2023-11-17 21:58:04', 'test', 'test');
INSERT INTO `sys_menu` VALUES (291, '会员管理', -1, '/member', '/Layout', '2', 0, 1, 'member', '2023-11-18 14:25:49', '2023-11-18 14:42:21', 'test2', 'test2');
INSERT INTO `sys_menu` VALUES (292, '分组管理', 291, '/member/group', '/member/group', '1', 0, 1, '/member/group', '2023-11-18 14:28:51', '2023-11-18 14:28:51', 'test2', 'test2');
INSERT INTO `sys_menu` VALUES (293, '会员列表', 291, '/member/list', '/member/list', '2', 0, 1, '/member/list', '2023-11-18 14:33:54', '2023-11-18 14:33:54', 'test2', 'test2');
INSERT INTO `sys_menu` VALUES (294, '会员收款列表', 291, '/member/payment', '/member/payment', '41', 0, 1, '/member/payment', '2023-11-18 14:34:22', '2023-11-18 14:34:22', 'test2', 'test2');
INSERT INTO `sys_menu` VALUES (295, 'usdt买入订单', 282, '/order/usdtBuyOrder/index', '/order/usdtBuyOrder/index', '1', 0, 1, '/order/usdtBuyOrder/index', '2023-11-20 15:16:43', '2023-11-20 15:17:31', 'test', 'test');
INSERT INTO `sys_menu` VALUES (296, '匹配池', 282, '/order/matchingPool/index', '/order/matchingPool/index', '1', 0, 1, '/order/matchingPool/index', '2023-11-20 15:31:46', '2023-11-20 15:31:46', 'test', 'test');

SET FOREIGN_KEY_CHECKS = 1;
