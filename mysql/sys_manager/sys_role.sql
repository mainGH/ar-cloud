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

 Date: 30/11/2023 14:00:24
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '角色编码',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '角色状态：0-正常；1-停用',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除；1-已删除',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `create_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '管理员', 'admin', 1, 1, 0, '2023-09-09 17:19:56', '2023-09-09 17:20:00', NULL, NULL);
INSERT INTO `sys_role` VALUES (2, '商户', 'merchant', 1, 1, 1, '2023-10-12 14:49:40', '2023-10-12 14:49:45', NULL, NULL);
INSERT INTO `sys_role` VALUES (6, 'adf', '12', 0, 1, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (7, 'dd', '122', 0, 1, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (8, '12', 'adf', 0, 1, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (9, '小鬼按钮', 'ghost', 0, 1, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (10, '小鬼', '14', 0, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (11, 'ad1', '24', 0, 1, 0, NULL, NULL, NULL, NULL);
INSERT INTO `sys_role` VALUES (12, '会员', 'member', 0, 1, 0, '2023-10-24 18:41:35', '2023-10-24 18:41:39', NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
