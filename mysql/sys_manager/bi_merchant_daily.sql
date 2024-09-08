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

 Date: 02/12/2023 11:37:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bi_merchant_daily
-- ----------------------------
DROP TABLE IF EXISTS `bi_merchant_daily`;
CREATE TABLE `bi_merchant_daily`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `date_time` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日期',
  `merchant_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户编码',
  `merchant_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户名称',
  `pay_money` decimal(20, 2) NULL DEFAULT 0.00 COMMENT '代收金额',
  `withdraw_money` decimal(20, 2) NULL DEFAULT 0.00 COMMENT '代付金额',
  `pay_order_num` bigint NULL DEFAULT 0 COMMENT '代收下单总笔数',
  `pay_success_order_num` bigint NULL DEFAULT 0 COMMENT '代收成功笔数',
  `total_fee` decimal(20, 2) NULL DEFAULT 0.00 COMMENT '总费用',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `last_minute` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上一次执行时间：22:05',
  `merchant_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户类型: 1.内部商户 2.外部商户',
  `withdraw_order_num` bigint NULL DEFAULT 0 COMMENT '代付下单总笔数',
  `withdraw_success_order_num` bigint NULL DEFAULT 0 COMMENT '代付成功笔数',
  `difference` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '收付差额',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `dm`(`date_time` ASC, `merchant_code` ASC) USING BTREE COMMENT '时间唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户日报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bi_merchant_daily
-- ----------------------------
INSERT INTO `bi_merchant_daily` VALUES (2, '2023-04-06', NULL, NULL, 500.00, 400.00, 1000, 500, 320.00, '2023-11-05 10:05:00', '2023-11-05 10:05:00', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (3, '2023-05-05', NULL, NULL, 500.00, 400.00, 1000, 500, 320.00, '2023-11-05 10:00:55', '2023-11-05 10:00:55', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (4, '2023-06-05', NULL, NULL, 500.00, 400.00, 1000, 500, 320.00, '2023-02-05 10:00:55', '2023-11-05 10:00:55', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (22, '2023-11-19', NULL, NULL, 100.00, 100.00, 1, 1, 0.00, '2023-11-21 20:39:40', '2023-11-21 20:39:40', '2023-11-21 20:39', NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (23, '2023-11-20', NULL, NULL, 100.00, 100.00, 1, 1, 0.00, '2023-11-21 20:39:40', '2023-11-21 20:39:40', '2023-11-21 20:39', NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (24, '2023-11-21', NULL, NULL, 100.00, 100.00, 1, 1, 0.00, '2023-11-21 20:39:41', '2023-11-21 20:39:41', '2023-11-21 20:39', NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (27, '2023-11-22', NULL, NULL, 100.00, 1500.00, 1, 1, 0.00, '2023-11-23 20:03:00', '2023-11-23 20:03:00', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (36, '2023-11-23', NULL, NULL, 100.00, 6.00, 1, 1, 0.00, '2023-11-23 20:35:43', '2023-11-23 20:35:43', '2023-11-23 20:35', NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (37, '2023-11-27', NULL, NULL, 100.00, 2244.00, 1, 1, 0.00, '2023-11-27 20:40:00', '2023-11-27 20:40:00', '2023-11-27 20:40', NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (38, '2023-11-28', NULL, NULL, 100.00, 2244.00, 2, 1, 0.00, '2023-11-28 15:25:00', '2023-11-28 15:40:00', '2023-11-28 15:40', NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (44, '2023-11-30', 'test8', NULL, 100.00, 100.00, 1, 1, 0.00, '2023-11-30 14:25:07', '2023-11-30 14:25:07', '2023-11-30 10:25', NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (45, '2023-11-30', 'test7', NULL, 100.00, 100.00, 2, 1, 0.00, '2023-11-30 14:25:08', '2023-11-30 14:33:15', '2023-11-30 10:33', NULL, NULL, NULL, NULL);
INSERT INTO `bi_merchant_daily` VALUES (47, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
