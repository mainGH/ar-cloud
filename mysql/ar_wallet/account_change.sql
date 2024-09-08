/*
 Navicat Premium Data Transfer

 Source Server         : Ar_Test
 Source Server Type    : MySQL
 Source Server Version : 80035
 Source Host           : 8.222.160.178:3306
 Source Schema         : ar_wallet

 Target Server Type    : MySQL
 Target Server Version : 80035
 File Encoding         : 65001

 Date: 30/11/2023 14:05:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account_change
-- ----------------------------
DROP TABLE IF EXISTS `account_change`;
CREATE TABLE `account_change`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户号',
  `currentcy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '币种',
  `change_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '账变类型: 1-代收, 2-代付, 3-下发,4-上分',
  `change_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '账变类别：add-增加, sub-支出',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单号',
  `before_change` decimal(10, 2) NULL DEFAULT NULL COMMENT '账变前',
  `amount_change` decimal(10, 2) NULL DEFAULT NULL COMMENT '变化金额',
  `after_change` decimal(10, 2) NULL DEFAULT NULL COMMENT '账变后金额',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` datetime NULL DEFAULT NULL COMMENT '修改人',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `merchant_order` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户订单号',
  `commission` decimal(10, 2) NULL DEFAULT NULL COMMENT '手续费',
  `merchant_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账变数据' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of account_change
-- ----------------------------
INSERT INTO `account_change` VALUES (48, 'test6', 'INR', '3', 'sub', 'MW2023112218402854607178', 99980.00, 10.00, 99970.00, '2023-11-22 22:40:38', '2023-11-22 22:40:38', NULL, NULL, 'sss', NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
