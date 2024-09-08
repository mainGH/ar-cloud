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

 Date: 30/11/2023 14:00:30
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID'
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (12, 1);
INSERT INTO `sys_role_menu` VALUES (12, 263);
INSERT INTO `sys_role_menu` VALUES (12, 256);
INSERT INTO `sys_role_menu` VALUES (12, 2);
INSERT INTO `sys_role_menu` VALUES (12, 234);
INSERT INTO `sys_role_menu` VALUES (12, 3);
INSERT INTO `sys_role_menu` VALUES (12, 235);
INSERT INTO `sys_role_menu` VALUES (12, 250);
INSERT INTO `sys_role_menu` VALUES (12, 275);
INSERT INTO `sys_role_menu` VALUES (12, 255);
INSERT INTO `sys_role_menu` VALUES (12, 251);
INSERT INTO `sys_role_menu` VALUES (10, 277);
INSERT INTO `sys_role_menu` VALUES (10, 279);
INSERT INTO `sys_role_menu` VALUES (10, 278);
INSERT INTO `sys_role_menu` VALUES (10, 263);
INSERT INTO `sys_role_menu` VALUES (10, 256);
INSERT INTO `sys_role_menu` VALUES (10, 2);
INSERT INTO `sys_role_menu` VALUES (10, 234);
INSERT INTO `sys_role_menu` VALUES (10, 3);
INSERT INTO `sys_role_menu` VALUES (10, 250);
INSERT INTO `sys_role_menu` VALUES (10, 275);
INSERT INTO `sys_role_menu` VALUES (10, 255);
INSERT INTO `sys_role_menu` VALUES (10, 251);
INSERT INTO `sys_role_menu` VALUES (10, 235);
INSERT INTO `sys_role_menu` VALUES (11, 277);
INSERT INTO `sys_role_menu` VALUES (11, 279);
INSERT INTO `sys_role_menu` VALUES (11, 278);
INSERT INTO `sys_role_menu` VALUES (1, 287);
INSERT INTO `sys_role_menu` VALUES (1, 285);
INSERT INTO `sys_role_menu` VALUES (1, 286);
INSERT INTO `sys_role_menu` VALUES (1, 282);
INSERT INTO `sys_role_menu` VALUES (1, 283);
INSERT INTO `sys_role_menu` VALUES (1, 284);
INSERT INTO `sys_role_menu` VALUES (1, 288);
INSERT INTO `sys_role_menu` VALUES (1, 289);
INSERT INTO `sys_role_menu` VALUES (1, 290);
INSERT INTO `sys_role_menu` VALUES (1, 295);
INSERT INTO `sys_role_menu` VALUES (1, 296);
INSERT INTO `sys_role_menu` VALUES (1, 291);
INSERT INTO `sys_role_menu` VALUES (1, 292);
INSERT INTO `sys_role_menu` VALUES (1, 293);
INSERT INTO `sys_role_menu` VALUES (1, 294);
INSERT INTO `sys_role_menu` VALUES (1, 264);
INSERT INTO `sys_role_menu` VALUES (1, 271);
INSERT INTO `sys_role_menu` VALUES (1, 270);
INSERT INTO `sys_role_menu` VALUES (1, 269);
INSERT INTO `sys_role_menu` VALUES (1, 268);
INSERT INTO `sys_role_menu` VALUES (1, 267);
INSERT INTO `sys_role_menu` VALUES (1, 266);
INSERT INTO `sys_role_menu` VALUES (1, 257);
INSERT INTO `sys_role_menu` VALUES (1, 258);
INSERT INTO `sys_role_menu` VALUES (1, 1);
INSERT INTO `sys_role_menu` VALUES (1, 263);
INSERT INTO `sys_role_menu` VALUES (1, 256);
INSERT INTO `sys_role_menu` VALUES (1, 2);
INSERT INTO `sys_role_menu` VALUES (1, 234);
INSERT INTO `sys_role_menu` VALUES (1, 3);
INSERT INTO `sys_role_menu` VALUES (1, 250);
INSERT INTO `sys_role_menu` VALUES (1, 275);
INSERT INTO `sys_role_menu` VALUES (1, 255);
INSERT INTO `sys_role_menu` VALUES (1, 251);
INSERT INTO `sys_role_menu` VALUES (1, 235);
INSERT INTO `sys_role_menu` VALUES (1, 272);
INSERT INTO `sys_role_menu` VALUES (1, 276);
INSERT INTO `sys_role_menu` VALUES (1, 280);
INSERT INTO `sys_role_menu` VALUES (1, 281);
INSERT INTO `sys_role_menu` VALUES (1, 273);

SET FOREIGN_KEY_CHECKS = 1;
