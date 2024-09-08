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

 Date: 30/11/2023 14:00:17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '权限名称',
  `menu_id` int NULL DEFAULT NULL COMMENT '菜单模块ID\r\n',
  `url_perm` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'URL权限标识',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `create_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `btn_sign` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '按钮',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id` ASC, `name` ASC) USING BTREE,
  INDEX `id_2`(`id` ASC, `name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES (13, '新增', 2, 'POST:/ar-manager/user/**', '2022-04-23 14:07:02', '2022-04-23 14:07:02', '4', '4', 'admin:user:add');
INSERT INTO `sys_permission` VALUES (14, '查询', 235, 'POST:/ar-manager/merchantInfo', '2023-09-25 10:00:49', '2023-09-25 10:00:52', '4', '4', 'pay:merchant:query');
INSERT INTO `sys_permission` VALUES (15, '查询', 236, 'POST:/ar-manager/collectionOrder', '2023-09-26 11:37:53', '2023-09-26 11:37:51', '4', '4', 'pay:collectionOrder:query');
INSERT INTO `sys_permission` VALUES (16, '查询', 3, 'POST:/ar-manager/menu/**', '2023-10-20 16:12:49', '2023-10-20 16:12:51', '4', '4', 'admin:menu:add');
INSERT INTO `sys_permission` VALUES (17, '小鬼按钮', 250, 'delete:/阿斯顿发123', NULL, NULL, '4', '4', 'admin');
INSERT INTO `sys_permission` VALUES (18, '查询', 234, 'POST:/ar-manager/role/**', '2023-10-20 16:16:55', '2023-10-20 16:17:02', '4', '4', 'admin:role:add');
INSERT INTO `sys_permission` VALUES (19, '12', 251, '阿斯顿:/12412aaa', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `sys_permission` VALUES (29, '查询', 252, 'POST:/ar-manager/permission', '2023-10-20 16:21:15', '2023-10-20 16:21:17', '4', '4', 'admin:permission:add');
INSERT INTO `sys_permission` VALUES (30, '新增', 252, 'POST:/ar-wallet/', '2023-10-24 17:57:42', NULL, NULL, NULL, NULL);
INSERT INTO `sys_permission` VALUES (31, '新增', 258, 'POST:/ar-wallet/dashboard/*', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `sys_permission` VALUES (32, '查询', 259, 'GET:/ar-wallet/memberInfo/current', '2023-10-25 17:10:09', '2023-10-25 17:10:12', NULL, NULL, NULL);
INSERT INTO `sys_permission` VALUES (33, '查看', 1, '啊啊啊:/啊啊啊啊啊啊啊啊', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `sys_permission` VALUES (34, '查看', 1, 'post:/check123', NULL, NULL, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
