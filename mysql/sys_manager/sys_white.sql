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

 Date: 30/11/2023 14:00:59
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_white
-- ----------------------------
DROP TABLE IF EXISTS `sys_white`;
CREATE TABLE `sys_white`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'ip地址',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT 'chuang\'j',
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'ip白名单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_white
-- ----------------------------
INSERT INTO `sys_white` VALUES (1, '124.12.41.11', NULL, NULL, '你好，藏红花24', '0', NULL, NULL);
INSERT INTO `sys_white` VALUES (2, '124.12.41.11', NULL, NULL, '你好，藏红花', '1', NULL, NULL);
INSERT INTO `sys_white` VALUES (3, '124.12.41.112', NULL, NULL, '你好，藏红花', '1', NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
