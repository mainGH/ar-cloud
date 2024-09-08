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

 Date: 30/11/2023 14:00:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `role_id` int NULL DEFAULT NULL COMMENT '角色id',
  `permission_id` int NULL DEFAULT NULL COMMENT '资源id',
  INDEX `role_id`(`role_id` ASC) USING BTREE,
  INDEX `permission_id`(`permission_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_permission
-- ----------------------------
INSERT INTO `sys_role_permission` VALUES (12, 13);
INSERT INTO `sys_role_permission` VALUES (12, 18);
INSERT INTO `sys_role_permission` VALUES (12, 16);
INSERT INTO `sys_role_permission` VALUES (12, 14);
INSERT INTO `sys_role_permission` VALUES (12, 33);
INSERT INTO `sys_role_permission` VALUES (12, 34);
INSERT INTO `sys_role_permission` VALUES (12, 19);
INSERT INTO `sys_role_permission` VALUES (12, 17);
INSERT INTO `sys_role_permission` VALUES (10, 13);
INSERT INTO `sys_role_permission` VALUES (10, 18);
INSERT INTO `sys_role_permission` VALUES (10, 16);
INSERT INTO `sys_role_permission` VALUES (10, 19);
INSERT INTO `sys_role_permission` VALUES (10, 14);
INSERT INTO `sys_role_permission` VALUES (10, 17);
INSERT INTO `sys_role_permission` VALUES (1, 31);
INSERT INTO `sys_role_permission` VALUES (1, 13);
INSERT INTO `sys_role_permission` VALUES (1, 18);
INSERT INTO `sys_role_permission` VALUES (1, 16);
INSERT INTO `sys_role_permission` VALUES (1, 33);
INSERT INTO `sys_role_permission` VALUES (1, 34);
INSERT INTO `sys_role_permission` VALUES (1, 19);
INSERT INTO `sys_role_permission` VALUES (1, 14);
INSERT INTO `sys_role_permission` VALUES (1, 17);

SET FOREIGN_KEY_CHECKS = 1;
