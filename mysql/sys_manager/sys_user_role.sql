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

 Date: 30/11/2023 14:00:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` int NOT NULL COMMENT '用户ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (4, 1);
INSERT INTO `sys_user_role` VALUES (7, 1);
INSERT INTO `sys_user_role` VALUES (10, 1);
INSERT INTO `sys_user_role` VALUES (10, 10);
INSERT INTO `sys_user_role` VALUES (11, 1);
INSERT INTO `sys_user_role` VALUES (15, 1);
INSERT INTO `sys_user_role` VALUES (15, 10);
INSERT INTO `sys_user_role` VALUES (19, 1);
INSERT INTO `sys_user_role` VALUES (21, 1);
INSERT INTO `sys_user_role` VALUES (22, 1);
INSERT INTO `sys_user_role` VALUES (23, 1);
INSERT INTO `sys_user_role` VALUES (24, 1);
INSERT INTO `sys_user_role` VALUES (26, 1);
INSERT INTO `sys_user_role` VALUES (27, 1);
INSERT INTO `sys_user_role` VALUES (28, 1);
INSERT INTO `sys_user_role` VALUES (29, 1);
INSERT INTO `sys_user_role` VALUES (30, 1);
INSERT INTO `sys_user_role` VALUES (31, 7);
INSERT INTO `sys_user_role` VALUES (32, 1);
INSERT INTO `sys_user_role` VALUES (33, 1);
INSERT INTO `sys_user_role` VALUES (35, 1);
INSERT INTO `sys_user_role` VALUES (38, 1);
INSERT INTO `sys_user_role` VALUES (39, 1);

SET FOREIGN_KEY_CHECKS = 1;
