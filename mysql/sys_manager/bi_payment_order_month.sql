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

 Date: 30/11/2023 14:01:14
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bi_payment_order_month
-- ----------------------------
DROP TABLE IF EXISTS `bi_payment_order_month`;
CREATE TABLE `bi_payment_order_month`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `date_time` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日期',
  `merchant_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户编码',
  `money` decimal(20, 2) NULL DEFAULT NULL COMMENT '订单金额',
  `actual_money` decimal(20, 2) NULL DEFAULT NULL COMMENT '实际金额',
  `order_num` bigint NULL DEFAULT NULL COMMENT '下单总笔数',
  `success_order_num` bigint NULL DEFAULT NULL COMMENT '成功笔数',
  `total_fee` decimal(20, 2) NULL DEFAULT NULL COMMENT '总费用',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `last_minute` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上一次执行时间：22:05',
  `cancel_pay` bigint NULL DEFAULT NULL COMMENT '取消支付订单数量',
  `cancel_order` bigint NULL DEFAULT NULL COMMENT '取消订单数量',
  `appeal_num` bigint NULL DEFAULT NULL COMMENT '申诉订单数量',
  `finish_duration` bigint NULL DEFAULT NULL COMMENT '平均完成时长',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `dm`(`date_time` ASC, `merchant_code` ASC) USING BTREE COMMENT '时间唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代收订单月表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bi_payment_order_month
-- ----------------------------
INSERT INTO `bi_payment_order_month` VALUES (2, '2023-10', NULL, 500.00, 400.00, 1000, 500, 320.00, '2023-11-05 10:35:26', '2023-11-05 10:35:34', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `bi_payment_order_month` VALUES (3, '2023-09', NULL, 500.00, 400.00, 1000, 500, 320.00, '2023-11-05 10:35:26', '2023-11-05 10:35:34', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `bi_payment_order_month` VALUES (31, '2023-11', NULL, 700.00, 6294.00, 8, 7, 0.00, '2023-11-23 20:35:43', '2023-11-28 15:40:00', '2023-11-28 15:40', NULL, NULL, NULL, NULL);
INSERT INTO `bi_payment_order_month` VALUES (35, '2023-11', NULL, 100.00, 100.00, 1, 1, 0.00, '2023-11-29 21:45:00', '2023-11-29 21:45:00', '2023-11-29 21:45', 0, 0, 0, 0);
INSERT INTO `bi_payment_order_month` VALUES (36, '2023-11', NULL, 100.00, 100.00, 1, 1, 0.00, '2023-11-29 22:50:00', '2023-11-29 22:50:00', '2023-11-29 22:50', 0, 0, 0, 0);
INSERT INTO `bi_payment_order_month` VALUES (37, '2023-11', NULL, 100.00, 100.00, 1, 1, 0.00, '2023-11-30 14:15:27', '2023-11-30 14:15:27', '2023-11-30 10:15', 0, 0, 0, 0);
INSERT INTO `bi_payment_order_month` VALUES (38, '2023-11', NULL, 100.00, 100.00, 1, 1, 0.00, '2023-11-30 14:15:27', '2023-11-30 14:15:27', '2023-11-30 10:09', 0, 0, 0, 0);
INSERT INTO `bi_payment_order_month` VALUES (39, '2023-11', 'test8', 100.00, 100.00, 1, 1, 0.00, '2023-11-30 14:25:07', '2023-11-30 14:25:07', '2023-11-30 10:25', 0, 0, 0, 0);
INSERT INTO `bi_payment_order_month` VALUES (40, '2023-11', 'test7', 100.00, 100.00, 2, 1, 0.00, '2023-11-30 14:25:08', '2023-11-30 14:33:15', '2023-11-30 10:33', 1, 0, 0, 0);

SET FOREIGN_KEY_CHECKS = 1;
